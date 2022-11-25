"""
Connect to specified server over RFCOMM (Bluetooth) and read 
data from stdin until EOF, sending it over the connection
Prints "CONNECTED" to stdout on first connection to signal owner processes.
"""

import socket
import argparse
import sys
import time

parser = argparse.ArgumentParser()
parser.add_argument("-a", "--address", required=True, help="Bluetooth address of server")
parser.add_argument("-p", "--port", default=5, type=int, help="Server port")
parser.add_argument("-v", "--verbose", action="store_true")
parser.add_argument("-r", "--retries", default=5, type=int, help="Max repeats when connecting")


args = parser.parse_args()
address: str = args.address
port: int = args.port
verbose: bool = args.verbose
retries: int = args.retries

if verbose:
    print(f"Connecting to {address}/{port}...", file=sys.stderr)

s = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)

done_repeats = 0
connected = False
while done_repeats < retries and not connected:
    try:
        s.connect((address, port))
        connected = True
    except Exception as e:
        print(e, file=sys.stderr)
        time.sleep(3)
        done_repeats += 1

if not connected:
    print("Wasn't able to connect", file=sys.stderr)
    sys.exit(-1)

print("CONNECTED")

if verbose:
    print(f"Sending standard input", file=sys.stderr)

for line in sys.stdin:
    s.send(bytes(line, 'UTF-8'))

if verbose:
    print(f"Input finished, closing...", file=sys.stderr)

s.close()
