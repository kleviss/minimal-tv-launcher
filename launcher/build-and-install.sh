#!/usr/bin/env bash
# Build the launcher APK and install it on the TV as the default home screen.
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the TV IP address, e.g. TV_HOST=192.168.1.50 $0}"
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || echo /opt/homebrew/opt/openjdk@17)}"
export ANDROID_HOME="${ANDROID_HOME:-$([ -d ~/Library/Android/sdk ] && echo ~/Library/Android/sdk || echo /opt/homebrew/share/android-commandlinetools)}"
export PATH="$JAVA_HOME/bin:$PATH"
[ -f local.properties ] || echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease -q --console=plain
PY=../tools/venv/bin/python
[ -x "$PY" ] || PY=../tools/venv/Scripts/python.exe
export MSYS_NO_PATHCONV=1
# Which app owns Home differs by build (com.google.android.tvlauncher on Android TV,
# com.google.android.apps.tv.launcherx on newer Google TV), so ask the TV instead of guessing.
# Only a launcher we recognise is accepted: the answer can also be Android's chooser ("android"),
# the setup wizard, or another third-party launcher, none of which should be disabled by accident.
# Pass STOCK_LAUNCHER=<package> to name it yourself, or STOCK_LAUNCHER=none to disable nothing.
KNOWN_STOCK_LAUNCHERS="com.google.android.tvlauncher com.google.android.apps.tv.launcherx com.google.android.leanbacklauncher"
if [ -z "${STOCK_LAUNCHER:-}" ]; then
  HOME_PKG=$("$PY" -I ../tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' \
    | tail -n 1 | tr -d '\r' | cut -d/ -f1) || {
    echo "ERROR: could not ask the TV which launcher owns Home. Check TV_HOST and the debugging prompt on the TV," >&2
    echo "or pass STOCK_LAUNCHER=<package> yourself." >&2; exit 1; }
  case " $KNOWN_STOCK_LAUNCHERS " in
    *" $HOME_PKG "*) STOCK_LAUNCHER=$HOME_PKG ;;
    *)
      if [ "$HOME_PKG" = com.minimaltv.home ]; then
        STOCK_LAUNCHER=none; echo "Home already belongs to this launcher; nothing will be disabled."
      else
        echo "ERROR: Home currently resolves to '${HOME_PKG:-nothing}', which is not a stock launcher this script knows." >&2
        echo "  android                                  = Android's launcher chooser (two launchers tie); pick a default on the TV first" >&2
        echo "  com.google.android.tungsten.setupwraith  = Google TV's setup wizard; see README, Troubleshooting" >&2
        echo "  anything else                            = another launcher is the default" >&2
        echo "Run again with STOCK_LAUNCHER=<package to disable> or STOCK_LAUNCHER=none." >&2; exit 1
      fi ;;
  esac
fi
echo "Stock launcher: $STOCK_LAUNCHER"
if [ "$STOCK_LAUNCHER" = none ]; then DISABLE_STOCK="echo stock launcher left alone"
else DISABLE_STOCK="pm disable-user --user 0 $STOCK_LAUNCHER"; fi
APK=app/build/outputs/apk/release/app-release.apk
"$PY" -I ../tools/b64push.py "$APK" /data/local/tmp/tvhome.apk
printf '%s\n' \
  "pm install -r /data/local/tmp/tvhome.apk" \
  "rm -f /data/local/tmp/tvhome.apk" \
  "pm grant com.minimaltv.home android.permission.ACCESS_FINE_LOCATION 2>/dev/null; pm grant com.minimaltv.home android.permission.ACCESS_COARSE_LOCATION 2>/dev/null; echo location-granted" \
  "cmd package compile -m speed -f com.minimaltv.home" \
  "am start -W -n com.minimaltv.home/.MainActivity | grep -E '^(Status|Error)'" \
  "$DISABLE_STOCK" \
  "cmd package set-home-activity --user 0 com.minimaltv.home/.MainActivity" \
  | "$PY" -I ../tools/tv.py --batch
# Order matters: the new launcher is started before the stock one is disabled, and the home
# preference is set after. Android drops the preference whenever the set of HOME apps changes, and
# on Google TV 14 the next candidate is the setup wizard's RecoveryActivity, which crash-loops
# system_server (see examples/xiaomi-tv-a-pro-2026). So check where Home ended up, and put the
# stock launcher back if it is not ours.
HOME_NOW=$("$PY" -I ../tools/tv.py 'cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN' | tail -n 1 | tr -d '\r')
case "$HOME_NOW" in
  com.minimaltv.home/*) echo "Home now resolves to $HOME_NOW" ;;
  *)
    [ "$STOCK_LAUNCHER" = none ] || "$PY" -I ../tools/tv.py "pm enable --user 0 $STOCK_LAUNCHER" >/dev/null || true
    echo "ERROR: Home resolves to ${HOME_NOW:-nothing}, not to the new launcher; $STOCK_LAUNCHER was re-enabled." >&2
    echo "On Google TV this usually means com.google.android.tungsten.setupwraith also claims Home: add it to" >&2
    echo "your debloat list (see README, Troubleshooting), apply the list, then run this script again." >&2
    exit 1 ;;
esac

echo "Installed. Press Home on the remote to see it."
