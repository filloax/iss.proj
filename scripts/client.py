"""
Connect to specified server over RFCOMM (Bluetooth) and read 
data from stdin until EOF, sending it over the connection
"""

import socket
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-a", "--address", required=True, help="Bluetooth address of server")
parser.add_argument("-p", "--port", default=5, help="Server port")
parser.add_argument("-v", "--verbose", action="store_true")

args = parser.parse_args()
address: str = args.address
port: int = args.port
verbose: bool = args.verbose

if verbose:
    print(f"Connecting to {address}/{port}...")

s = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
s.connect((address, port))

if verbose:
    print(f"Sending standard input")

for line in sys.stdin:
    s.send(bytes(line, 'UTF-8'))

if verbose:
    print(f"Input finished, closing...")

s.close()
