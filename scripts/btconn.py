"""
Connect to specified server over RFCOMM (Bluetooth) and read 
data from stdin until EOF, sending it over the connection
Prints "CONNECTED" to stdout on first connection to signal owner processes.

IMPORTANT: Requires at least Python 3.6 (on Linux) adn 3.11* (on Windows) for socket.AF_BLUETOOTH to work.

* Note: cannot find exact information on when socket.AF_BLUETOOTH started working on Windows, 
but 3.11 is the version used in development and so sure to work.
"""

import socket
import select
import queue
import argparse
import sys
import time
import threading

parser = argparse.ArgumentParser()
parser.add_argument("-a", "--address", required=True, help="Bluetooth address of server (if client) or device (if server)")
parser.add_argument("-l", "--listen", action="store_true", help="Server mode: listen for incoming connections")
parser.add_argument("-p", "--port", default=5, type=int, help="Server port")
parser.add_argument("-v", "--verbose", action="store_true")
parser.add_argument("-r", "--retries", default=5, type=int, help="Max repeats when connecting")
parser.add_argument("--nodelay", action="store_true", help="Do not buffer socket messages")


args = parser.parse_args()
address: str = args.address
listen: bool = args.listen
port: int = args.port
verbose: bool = args.verbose
retries: int = args.retries
nodelay: bool = args.nodelay

def printv(s:str, file=sys.stderr, flush=False):
    if verbose:
        print(s, file=file, flush=flush)

send_queue = queue.Queue()
rsock, ssock = socket.socketpair()
main_socket = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)

if listen:
    printv("BT | Server mode", flush=True)
else:
    printv("BT | Client mode", flush=True)


flags = 0
if nodelay:
    printv("No delay on, setting buffer to 0")
    main_socket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 0)
    flags += socket.MSG_WAITALL

finished = False

def input_thread_fun(name):
    global finished

    printv(f"Sending standard input", flush=True)
    for line in sys.stdin:
        send_queue.put(line)
        nonlline = line.replace('\n', '')
        printv(f"T2 | Queueing '{nonlline}'", flush=True)

        # Notify main thread
        ssock.send(b"\x00")
  
    printv(f"T2 | Input finished, closing...")

    finished = True
    ssock.send(b"\x00")


def connect_client(sock: socket.socket, addr, p, retries):
    printv(f"Connecting to {address}/{port}...", flush=True)

    done_repeats = 0
    connected = False
    while done_repeats < retries and not connected:
        try:
            sock.connect((addr, p))
            connected = True
        except Exception as e:
            print(e, file=sys.stderr, flush=True)
            time.sleep(3)
            printv("Retrying...", flush=True)
            done_repeats += 1

    printv(f'Connected')

    return connected

def listen_server(sock: socket.socket, addr, p):
    sock.bind((addr, p))

    printv(f'Bound to {address}/{port}')

    sock.listen(1)
    printv('Listening for connection...')

    client, clnt_addr = sock.accept()
    
    printv(f'Connected to {address}')

    return client, clnt_addr

active_socket = None
connected = False

if listen:
    active_socket, _ = listen_server(main_socket, address, port)
    connected = True
else:
    connected = connect_client(main_socket, address, port, retries)
    active_socket = main_socket

if not connected:
    print("Wasn't able to connect", file=sys.stderr)
    main_socket.close()
    rsock.close()
    ssock.close()
    sys.exit(-1)

print("CONNECTED")

input_thread = threading.Thread(target=input_thread_fun, args=(1,), daemon=True)
input_thread.start()
printv("Started input thread")

errored = False

try:
    while not finished:
        # When either main_socket has data or rsock has data, select.select will return
        rlist, _, _ = select.select([active_socket, rsock], [], [])
        # printv(f"Select wake: {rlist}")
        for ready_socket in rlist:
            if ready_socket is active_socket:
                data = active_socket.recv(1024)
                if data:
                    str_data = data.decode('utf-8')
                    print(str_data, flush=True, end='')
                else:
                    finished = True
                    printv("Remote peer closed")

            else: #rsock ready
                # Rimuovi "ready mark"
                rsock.recv(1)
                if not finished:
                    val = send_queue.get()
                    nonlval = val.replace('\n', '')
                    printv(f"Sending: {nonlval}")
                    active_socket.sendall(bytes(val, 'utf-8'))
except KeyboardInterrupt as e:
    print("Interrupted via key", file=sys.stderr)
except ConnectionResetError as e:
    print("Connection reset by peer", file=sys.stderr)
except Exception as e:
    print(f'Something went wrong: {e}, <{e.__class__.__name__}>', file=sys.stderr)
    errored = True

try:
    while not send_queue.empty() and not errored:
        val = send_queue.get()
        nonlval = val.replace('\n', '')
        printv(f"Sending on closure: {nonlval}")
        active_socket.sendall(bytes(val, 'utf-8'))
finally:
    active_socket.close()
    main_socket.close()
    rsock.close()
    ssock.close()
