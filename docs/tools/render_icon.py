"""Renders the project avatar: the Hill Core on concentric field rings.

Usage: python3 docs/tools/render_icon.py   (writes docs/art/avatar.png and neoforge logo.png)
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "common/src/main/resources/assets/hillsphere/textures/block"
SIZE = 512
LO, HI = 1.0, 15.0  # the body of the core in block pixels


def load(name):
    return np.asarray(Image.open(TEX / f"{name}.png").convert("RGBA"), dtype=np.float32)


def rotation(pitch, yaw):
    p, y = np.radians(pitch), np.radians(yaw)
    rx = np.array([[1, 0, 0], [0, np.cos(p), -np.sin(p)], [0, np.sin(p), np.cos(p)]])
    ry = np.array([[np.cos(y), 0, np.sin(y)], [0, 1, 0], [-np.sin(y), 0, np.cos(y)]])
    return ry @ rx


def background():
    ys, xs = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    d = np.hypot(xs - SIZE / 2, ys - SIZE / 2) / (SIZE / 2)
    img = np.zeros((SIZE, SIZE, 3), np.float32)
    base = np.array([10, 12, 28], np.float32)
    glow = np.array([40, 120, 150], np.float32)
    img[:] = base + (glow - base) * np.clip(1.0 - d, 0, 1)[..., None] ** 2.2 * 0.8
    for radius, width, color in ((0.42, 0.012, (70, 190, 210)), (0.62, 0.010, (60, 150, 190)),
                                 (0.82, 0.008, (110, 90, 190)), (0.98, 0.006, (90, 70, 160))):
        ring = np.clip(1.0 - np.abs(d - radius) / width, 0, 1)[..., None]
        img = img * (1 - ring * 0.85) + np.array(color, np.float32) * ring * 0.85
    return img


def cube(img):
    rot = rotation(-30, 45)
    view = rot @ np.array([0.0, 0.0, 1.0])  # the camera looks along -view
    scale = 17.5 / SIZE * 1.0
    ys, xs = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    px = (xs - SIZE / 2) * scale * 2.0
    py = -(ys - SIZE / 2) * scale * 2.0
    right = rot @ np.array([1.0, 0, 0])
    up = rot @ np.array([0, 1.0, 0])
    origin = 8 + right * px[..., None] + up * py[..., None] + view * 60
    direction = -view
    faces = {  # axis, side -> (texture, u axis, v axis, flip u, flip v)
        (0, 1): ("hill_core_face", 2, 1, True, False), (0, 0): ("hill_core_panel", 2, 1, False, False),
        (2, 1): ("hill_core_panel", 0, 1, False, False), (2, 0): ("hill_core_casing", 0, 1, True, False),
        (1, 1): ("hill_core_casing", 0, 2, False, False), (1, 0): ("hill_core_casing", 0, 2, False, True),
    }
    best = np.full((SIZE, SIZE), np.inf, np.float32)
    for (axis, side), (name, ua, va, fu, fv) in faces.items():
        plane = HI if side else LO
        t = (plane - origin[..., axis]) / direction[axis]
        hit = origin + direction * t[..., None]
        inside = (hit[..., ua] >= LO) & (hit[..., ua] <= HI) & (hit[..., va] >= LO) & (hit[..., va] <= HI)
        inside &= (t > 0) & (t < best)
        tex = load(name)
        u = (hit[..., ua] - LO) / (HI - LO)
        v = (hit[..., va] - LO) / (HI - LO)
        u = 1 - u if fu else u
        v = v if fv else 1 - v
        ui = np.clip((u * 16).astype(int), 0, 15)
        vi = np.clip((v * 16).astype(int), 0, 15)
        colour = tex[vi, ui]
        shade = 0.55 + 0.45 * abs(np.dot(np.eye(3)[axis], view))
        rgb = colour[..., :3] * shade
        alpha = colour[..., 3:4] / 255.0
        if name == "hill_core_face":  # the window is open: dark inside, a faint glow at its middle
            depth = np.clip(1.0 - np.hypot(u - 0.5, v - 0.5) * 2.2, 0, 1)[..., None]
            inside_colour = np.array([8, 10, 20], np.float32) + np.array([20, 80, 95], np.float32) * depth ** 2
            rgb = np.where(alpha > 0.5, rgb, inside_colour * shade)
            alpha = np.ones_like(alpha)
        paint = inside[..., None] & (alpha > 0.5)
        img[paint[..., 0]] = rgb[paint[..., 0]]
        best = np.where(inside, t, best)
    return img


def main():
    img = cube(background())
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    (ROOT / "docs/art").mkdir(exist_ok=True)
    out.save(ROOT / "docs/art/avatar.png")
    out.resize((256, 256), Image.LANCZOS).save(ROOT / "neoforge/src/main/resources/logo.png")


main()
