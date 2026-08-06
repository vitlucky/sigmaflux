#!/usr/bin/env python3
"""Генерация graphite_material_bg.png (Graphite Premium фон для графика).

Диагональный полнотелый серый bullish-chart фон с едва видимой технической сеткой.
Использование: python3 tools/generate_graphite_bg.py
Результат: app/src/main/res/drawable-nodpi/graphite_material_bg.png
"""

import os
import struct
import zlib

W, H = 720, 1280
BG_TOP = (0x16, 0x16, 0x1C)
BG_BOT = (0x1F, 0x1F, 0x2A)
STEP = 80


def chunk(tag: bytes, data: bytes) -> bytes:
    c = struct.pack(">I", len(data)) + tag + data
    c += struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    return c


def main() -> None:
    rows = []
    for y in range(H):
        t = y / (H - 1)
        r = BG_TOP[0] + (BG_BOT[0] - BG_TOP[0]) * t
        g = BG_TOP[1] + (BG_BOT[1] - BG_TOP[1]) * t
        b = BG_TOP[2] + (BG_BOT[2] - BG_TOP[2]) * t
        row = bytearray()
        for x in range(W):
            diag = (x / W + y / H) / 2.0
            r2, g2, b2 = r + 6 * diag, g + 6 * diag, b + 8 * diag
            if x % STEP == 0 or y % STEP == 0:
                row += bytes((int(r2 * 0.92), int(g2 * 0.92), int(b2 * 0.92), 255))
            else:
                row += bytes((int(r2), int(g2), int(b2), 255))
        rows.append(bytes(row))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(b"".join(rows), 6))
    png += chunk(b"IEND", b"")

    out = os.path.join(
        os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi", "graphite_material_bg.png"
    )
    out = os.path.normpath(out)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as f:
        f.write(png)
    print(f"written {out} ({len(png)} bytes)")


if __name__ == "__main__":
    main()
