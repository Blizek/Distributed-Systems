import socket

server_address = ('localhost', 9008)
msg_bytes = (300).to_bytes(4, byteorder='little')

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
try:
    print(f"Wysyłanie liczby 300 (Little-Endian)...")
    sock.sendto(msg_bytes, server_address)

    data, _ = sock.recvfrom(1024)
    received_val = int.from_bytes(data, byteorder='little')
    print(f"Otrzymano od serwera: {received_val}")
finally:
    sock.close()