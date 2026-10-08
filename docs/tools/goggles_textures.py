#!/usr/bin/env python3
# Copyright (C) 2026 loki
# SPDX-License-Identifier: GPL-3.0-or-later
"""Draws the Hill Goggles textures: the 16x16 item icon, the 64x32 armor layer and its glow layer.

Run from the repository root:  python3 docs/tools/goggles_textures.py
Colours come from docs/ART.md (metal, gravite crystal, attraction) plus a brass ramp for the rims.
"""
import math
from pathlib import Path
from PIL import Image

OUT = Path("common/src/main/resources/assets/hillsphere/textures")

METAL = ["#1c1b2a", "#2b2a3d", "#3d3b57", "#55527a", "#7a77a3"]  # deep, dark, mid, light, highlight
CRYSTAL = ["#5a3fc4", "#8a6bff", "#b9a6ff"]
TEAL = ["#1c8c99", "#3fd9e8", "#a8f5ff"]
BRASS = ["#6b3d23", "#a8673a", "#e4b153", "#f8e3a0"]  # deep, dark, mid, light
ANDESITE = "#8b8a83"
SPARK = "#effdff"


def rgba(hex_colour, alpha=255):
    h = hex_colour.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha)


def icon():
    """Two ringed lenses in brass rims on a dark strap, outlined and with a soft drop shadow."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    shape = {}
    for y in range(16):
        for x in range(16):
            colour = lens_pixel(x, y)
            if colour is None and 7 <= y <= 9 and (x <= 1 or x >= 14):
                colour = METAL[3] if y == 7 else METAL[2] if y == 8 else METAL[1]
            if colour is None and x in (7, 8) and y in (8, 9):
                colour = METAL[3] if y == 8 else METAL[2]
            if colour is not None:
                shape[(x, y)] = colour
    shape[(0, 8)] = ANDESITE
    shape[(15, 8)] = ANDESITE
    for (x, y) in shape:  # drop shadow one pixel down-right
        if (x + 1, y + 1) not in shape and x < 15 and y < 15:
            px[x + 1, y + 1] = (0, 0, 0, 70)
    for (x, y), colour in shape.items():
        px[x, y] = rgba(colour)
    for (x, y) in list(shape):  # dark outline where the shape meets nothing
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in shape and px[nx, ny][3] < 255:
                px[nx, ny] = rgba(METAL[0])
    px[2, 7] = rgba(SPARK)  # the one bright spot
    return img


def lens_pixel(x, y):
    for cx in (3.5, 11.5):
        d = math.hypot(x - cx, y - 8.5)
        if d <= 1.0:
            return METAL[0]
        if d <= 2.0:
            return TEAL[1] if y <= 8 else TEAL[0]
        if d <= 2.6:
            return METAL[1]
        if d <= 3.6:
            return BRASS[3] if y < 7 else BRASS[2] if y <= 9 else BRASS[1]
    return None


def lens_front(put, u, v, glow):
    """A 3x3 lens: dark core, a ring around it, rim corners."""
    for j in range(3):
        for i in range(3):
            centre = i == 1 and j == 1
            corner = i != 1 and j != 1
            if glow:
                put(u + i, v + j, "#000000" if centre or corner else "#ffffff")
            else:
                put(u + i, v + j, METAL[0] if centre else BRASS[1] if corner else TEAL[1] if j < 2 else TEAL[0])


def armor(glow):
    """Vanilla head layout from (0,0); the 3D frame cube at (32,0) and the lens cube at (32,6)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()

    def put(x, y, colour):
        px[x, y] = rgba(colour)

    if glow:
        lens_front(put, 33, 7, True)
        return img
    band = {11: METAL[3], 12: METAL[2], 13: METAL[1]}
    for y, colour in band.items():  # strap around the sides and the back of the head
        for x in list(range(0, 8)) + list(range(16, 32)):
            put(x, y, colour)
    for y in (11, 12, 13):  # brass buckle on the back
        put(27, y, BRASS[2])
        put(28, y, BRASS[1])
    for y, colour in band.items():  # face band, seen only if a pack swaps the model for the plain layer
        for x in range(8, 16):
            put(x, y, colour)
    lens_front(put, 8, 11, False)
    lens_front(put, 13, 11, False)
    frame(put)
    for x in range(33, 36):
        put(x, 6, BRASS[3])
    for x in range(36, 39):
        put(x, 6, BRASS[1])
    for y in range(7, 10):
        put(32, y, BRASS[2])
        put(36, y, BRASS[2])
        for x in range(37, 40):
            put(x, y, METAL[0])
    lens_front(put, 33, 7, False)
    return img


def frame(put):
    """The 9x4x1 plate the lenses sit in."""
    for x in range(33, 42):
        put(x, 0, BRASS[3])
    for x in range(42, 51):
        put(x, 0, BRASS[1])
    for y in range(1, 5):
        put(32, y, BRASS[2])
        put(42, y, BRASS[2])
        for x in range(43, 52):
            put(x, y, METAL[1])
        for x in range(33, 42):
            edge = y in (1, 4) or x in (33, 41)
            put(x, y, (BRASS[2] if y == 1 else BRASS[1]) if edge else METAL[2] if x == 37 else METAL[0])


def main():
    (OUT / "item").mkdir(parents=True, exist_ok=True)
    (OUT / "models/armor").mkdir(parents=True, exist_ok=True)
    icon().save(OUT / "item/hill_goggles.png")
    armor(False).save(OUT / "models/armor/hill_goggles_layer_1.png")
    armor(True).save(OUT / "models/armor/hill_goggles_layer_1_glow.png")


if __name__ == "__main__":
    main()
