"""Lay renders out on one labelled sheet: one row per direction, one column per wallpaper.

Usage: python3 docs/design/contact_sheet.py [--wps 1,2,3,4] [--out name] key [key ...]
Reads renders/<key>-wp<N>.png, writes renders/<name>.png and a jpg copy for sending.
"""
import argparse
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RENDERS = os.path.join(HERE, "renders")
TW, TH = 640, 360          # thumbnail size (1920x1080 / 3)
GAP, LABEL = 24, 54
TITLES = {
    "a-tvos": "A  tvOS faithful",
    "b-tray": "B  iOS 18 tray",
    "c-lockscreen": "C  Lock screen",
    "d-hybrid": "D  Hybrid: C with A's dock",
}

ap = argparse.ArgumentParser()
ap.add_argument("--wps", default="1,2,3")
ap.add_argument("--out", default="contact-sheet")
ap.add_argument("keys", nargs="+")
args = ap.parse_args()
wps = [int(w) for w in args.wps.split(",")]

font_path = os.path.expanduser("~/Library/Fonts/Roboto-Medium.ttf")
font = ImageFont.truetype(font_path, 30)
small = ImageFont.truetype(font_path, 22)

width = GAP + len(wps) * (TW + GAP)
height = GAP + len(args.keys) * (LABEL + TH + GAP)
sheet = Image.new("RGB", (width, height), (24, 24, 26))
draw = ImageDraw.Draw(sheet)

y = GAP
for key in args.keys:
    draw.text((GAP, y + 10), TITLES.get(key, key), font=font, fill=(240, 240, 240))
    y += LABEL
    x = GAP
    for wp in wps:
        path = os.path.join(RENDERS, f"{key}-wp{wp}.png")
        if os.path.exists(path):
            thumb = Image.open(path).convert("RGB").resize((TW, TH), Image.LANCZOS)
            sheet.paste(thumb, (x, y))
        else:
            draw.rectangle([x, y, x + TW, y + TH], outline=(90, 90, 90))
            draw.text((x + 20, y + 20), f"missing {os.path.basename(path)}", font=small, fill=(160, 160, 160))
        draw.text((x, y + TH + 6), f"wallpaper {wp}", font=small, fill=(150, 150, 150))
        x += TW + GAP
    y += TH + GAP

out = os.path.join(RENDERS, f"{args.out}.png")
sheet.save(out, optimize=True)
sheet.save(os.path.join(RENDERS, f"{args.out}.jpg"), quality=86)
print("wrote", out, sheet.size)
