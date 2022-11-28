"""
Mocks a message process
"""

import sys

def send(string):
    print(string)
    print(f"Sent '{string}'", file=sys.stderr)

print("Starting dummy server..", file=sys.stderr)

send("CONNECTED")
send("msg(test,dispatch,clnt,srvr,ananas,0)")

print("Dummy server done", file=sys.stderr)
