"""Three placeholder wallpapers in the beige, painterly direction the real photos will take.

They stand in until the real pictures arrive, and give all mockups the same backgrounds to be judged on.
Pure Pillow, no numpy.  Usage: python3 docs/design/make_placeholders.py
"""
import os

from PIL import Image, ImageDraw, ImageFilter

W, H = 1920, 1080
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "wallpapers")


def gradient(top, bottom, angle_shift=0.0):
    """Vertical gradient between two RGB colours, with an optional diagonal lean."""
    img = Image.new("RGB", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(0, W, 4):
            t = min(1.0, max(0.0, y / H + angle_shift * (x / W - 0.5)))
            c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
            for dx in range(4):
                px[x + dx, y] = c
    return img


def blob(base, box, color, alpha, blur):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse(box, fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    return Image.alpha_composite(base.convert("RGBA"), layer).convert("RGB")


def stroke(base, points, width, color, alpha, blur):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(layer).line(points, fill=color + (alpha,), width=width, joint="curve")
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    return Image.alpha_composite(base.convert("RGBA"), layer).convert("RGB")


def grain(base, amount=0.06):
    noise = Image.effect_noise((W, H), 40).convert("RGB")
    return Image.blend(base, noise, amount)


def save(img, name):
    path = os.path.join(OUT, name)
    img.save(path, quality=88, optimize=True)
    print("wrote", path, os.path.getsize(path) // 1024, "KB")


os.makedirs(OUT, exist_ok=True)

# 1. Dune: sand to cream, one warm ochre glow low right. The gentlest one.
img = gradient((226, 214, 196), (244, 238, 226), 0.25)
img = blob(img, (1000, 500, 2200, 1500), (205, 160, 110), 170, 140)
img = blob(img, (-300, -200, 700, 500), (255, 252, 244), 160, 160)
save(grain(img), "placeholder-1-dune.jpg")

# 2. Ink: warm off-white with one dark umber brush stroke. Tests text on a dark passage.
img = gradient((239, 232, 221), (228, 219, 204), -0.15)
img = stroke(img, [(200, 760), (620, 420), (1080, 620), (1500, 330), (1800, 520)], 150, (78, 60, 48), 215, 12)
img = stroke(img, [(260, 800), (640, 480), (1100, 660), (1480, 390)], 40, (120, 92, 70), 120, 30)
save(grain(img, 0.05), "placeholder-2-ink.jpg")

# 3. Sage: pale beige into sage, two overlapping soft discs in clay and moss.
img = gradient((236, 229, 214), (196, 203, 186), 0.35)
img = blob(img, (300, 150, 1150, 1000), (196, 142, 108), 150, 90)
img = blob(img, (800, 350, 1750, 1300), (130, 150, 122), 150, 90)
save(grain(img), "placeholder-3-sage.jpg")

# 4. Dusk: dark umber sky over warm sand. The one that forces the light-on-dark flip at the top.
img = gradient((58, 46, 40), (214, 196, 172), 0.1)
img = blob(img, (500, -300, 1500, 450), (96, 72, 60), 150, 160)
img = blob(img, (1100, 650, 2300, 1500), (232, 205, 170), 150, 150)
save(grain(img), "placeholder-4-dusk.jpg")
