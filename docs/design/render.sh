#!/usr/bin/env bash
# Render a mockup to PNG with headless Chrome at TV resolution.
# Usage: docs/design/render.sh mockups/a.html 'wp=2&focus=netflix' renders/a-wp2.png
set -euo pipefail
cd "$(dirname "$0")"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
page="$1"; query="${2:-}"; out="$3"
url="file://$PWD/$page"; [ -n "$query" ] && url="$url?$query"
"$CHROME" --headless=new --disable-gpu --hide-scrollbars --window-size=1920,1080 --force-device-scale-factor=1 \
  --virtual-time-budget=4000 --screenshot="$PWD/$out" "$url" >/dev/null 2>&1
echo "rendered $out"
