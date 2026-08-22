#!/usr/bin/env python3
"""Tiny RCON client to drive the local test server from a terminal.

Usage:
  python3 rcon.py "clan list"
  python3 rcon.py "clan create SEPI red"

The command is sent as the server console, so player-only commands will
return the "You must be a player" message - use an in-game chat for those.
"""
import socket
import struct
import sys

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = "clans123"


def packet(request_id, packet_type, body: bytes) -> bytes:
    return struct.pack("<ii", request_id, packet_type) + body + b"\x00\x00"


def recv_packet(sock):
    length = struct.unpack("<i", _read(sock, 4))[0]
    request_id, packet_type = struct.unpack("<ii", _read(sock, 8))
    body = _read(sock, length - 10)
    sock.recv(2)  # trailing nulls
    return request_id, packet_type, body.decode("utf-8", "replace")


def _read(sock, n):
    data = b""
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise ConnectionError("connection closed")
        data += chunk
    return data


def main():
    command = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "list"
    with socket.create_connection((HOST, PORT), timeout=5) as sock:
        sock.sendall(packet(1, 3, PASSWORD.encode()))
        req, typ, body = recv_packet(sock)
        if typ == 2:
            print("RCON auth failed")
            sys.exit(1)
        sock.sendall(packet(2, 2, command.encode()))
        sock.settimeout(8)
        req, typ, body = recv_packet(sock)
        print(body)


if __name__ == "__main__":
    main()
