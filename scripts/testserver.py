import socket

adapter_addr = '00:10:60:d1:4b:88'
port = 5
buf_size = 1024

s = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
s.bind((adapter_addr, port))
s.listen(1)
try:
    print('Listening for connection...')
    client, address = s.accept()
    print(f'Connected to {address}')

    while True:
        data = client.recv(buf_size)
        if data:
            print(data)
except Exception as e:
    print(f'Something went wrong: {e}')
    client.close()
    s.close()