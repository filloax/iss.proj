"""
Wait from connections over RFCOMM (Bluetooth) and dump data
from connection to stdout, until EOF signal is received
"""

import socket
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-a", "--address", required=True, help="Bluetooth adapter address to use (in case PC has more)")
parser.add_argument("-p", "--port", default=5, help="Server port")
parser.add_argument("-v", "--verbose", action="store_true")

args = parser.parse_args()
address: str = args.address
port: int = args.port
verbose: bool = args.verbose
buf_size = 1024

s = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
s.bind((address, port))
s.listen(1)
try:
    if verbose:
        print('Listening for connection...', file=sys.stderr)
    client, address = s.accept()
    if verbose:
        print(f'Connected to {address}', file=sys.stderr)

    while True:
        data = client.recv(buf_size)
        if data:
            sys.stdout.write(str(data, 'UTF-8'))
        else:
            if verbose:
                print('Connection closed', file=sys.stderr)
            break
except Exception as e:
    print(f'Something went wrong: {e}', file=sys.stderr)
    client.close()
    s.close()