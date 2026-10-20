"""Turn the owner's photos into launcher assets.

For each photo: a 1920x1080 cover crop (wallpaper), the same blurred (what the frosted panels show),
and a theme line: which colour pair each screen region uses, sampled from the blurred photo. The TV
reads the result as-is and does no image work at start.

Usage: python3 docs/design/prepare_wallpapers.py <source folder>
Writes launcher/app/src/main/assets/wallpapers/{NN.jpg, NN-blur.jpg, themes.json}
and copies of the crops to docs/design/wallpapers/real/ for the mockups.
"""
import glob
import json
import os
import sys

from PIL import Image, ImageFilter, ImageStat

W, H = 1920, 1080
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "launcher/app/src/main/assets/wallpapers")
MOCK = os.path.join(ROOT, "docs/design/wallpapers/real")

# Screen regions (1080p px) the launcher's chrome sits in. Luminance under each decides its pair.
REGIONS = {
    "top": (560, 120, 1360, 440),      # greeting, clock, date
    "pills": (1250, 56, 1860, 150),    # the three pills
    "dock": (300, 600, 1620, 1030),    # app name, tiles, VPN chip, Add app
}
DARK_BELOW = 0.42   # mean luminance under which a region takes the dark pair (light text)


def cover(im):
    s = max(W / im.width, H / im.height)
    im = im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS)
    l, t = (im.width - W) // 2, (im.height - H) // 2
    return im.crop((l, t, l + W, t + H))


def luminance(gray, box):
    return ImageStat.Stat(gray.crop(box)).mean[0] / 255


src = sorted(p for p in glob.glob(os.path.join(sys.argv[1], "*")) if p.lower().endswith((".jpg", ".jpeg", ".png")))
os.makedirs(ASSETS, exist_ok=True)
os.makedirs(MOCK, exist_ok=True)
for old in glob.glob(os.path.join(ASSETS, "*")):
    os.remove(old)

themes = []
for i, path in enumerate(src, start=1):
    name = f"{i:02d}"
    im = cover(Image.open(path).convert("RGB"))
    blur = im.filter(ImageFilter.GaussianBlur(28))
    im.save(os.path.join(ASSETS, f"{name}.jpg"), quality=86, optimize=True, progressive=False)
    # The blurred twin only needs to be sharp enough for a 24px-blur look: half size saves memory on the TV.
    blur.resize((W // 2, H // 2), Image.LANCZOS).save(os.path.join(ASSETS, f"{name}-blur.jpg"), quality=80, optimize=True)
    im.save(os.path.join(MOCK, f"{name}.jpg"), quality=86)
    gray = blur.convert("L")
    regions = {k: luminance(gray, box) for k, box in REGIONS.items()}
    theme = {
        "file": f"{name}.jpg",
        "source": os.path.basename(path),
        "top": "dark" if regions["top"] < DARK_BELOW else "light",
        "pills": "dark" if regions["pills"] < DARK_BELOW else "light",
        "dock": "dark" if regions["dock"] < DARK_BELOW else "light",
        "luminance": {k: round(v, 2) for k, v in regions.items()},
    }
    themes.append(theme)
    print(f"{name}  {os.path.basename(path):<14} top {regions['top']:.2f} {theme['top']:<5} "
          f"pills {regions['pills']:.2f} {theme['pills']:<5} dock {regions['dock']:.2f} {theme['dock']}")

with open(os.path.join(ASSETS, "themes.json"), "w") as f:
    json.dump(themes, f, indent=1)
total = sum(os.path.getsize(p) for p in glob.glob(os.path.join(ASSETS, "*")))
print(f"{len(themes)} wallpapers, assets {total // 1024} KB")
