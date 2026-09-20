#!/usr/bin/env bash
#
# Build a debug APK and serve it on the local network.
#
# There is no working "adb install" on the ViWoods AiPaper Mini, so the build
# has to reach the tablet over HTTP. Open the printed URL in the tablet
# browser, or press "Get latest build" on the Dev screen in the app.
#
# Usage:
#   tools/deploy.sh [flavour] [port]
#
#   flavour  viwoods (default) or generic
#   port     8000 by default
#
set -euo pipefail

# On this dev machine "python3" on PATH is a PlatformIO venv. Always use the
# system interpreter.
PYTHON=/usr/bin/python3

FLAVOUR="${1:-viwoods}"
PORT="${2:-8000}"
# Optional third argument: the target SDK of the viwoods flavour. The default
# is 30, which the fast pen is said to need. Pass 37 to test the other case.
VIWOODS_TARGET="${3:-}"

case "$FLAVOUR" in
  viwoods) GRADLE_TASK="assembleViwoodsDebug" ;;
  generic) GRADLE_TASK="assembleGenericDebug" ;;
  *) echo "Unknown flavour: $FLAVOUR. Use viwoods or generic." >&2; exit 1 ;;
esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ ! -x "$PYTHON" ]; then
  echo "Missing $PYTHON. Install the system Python: sudo pacman -S python" >&2
  exit 1
fi

echo "Building $FLAVOUR debug"
if [ -n "$VIWOODS_TARGET" ]; then
  ./gradlew "$GRADLE_TASK" "-PviwoodsTargetSdk=$VIWOODS_TARGET"
else
  ./gradlew "$GRADLE_TASK"
fi

APK="app/build/outputs/apk/$FLAVOUR/debug/app-$FLAVOUR-debug.apk"
if [ ! -f "$APK" ]; then
  echo "No APK at $APK" >&2
  exit 1
fi

SERVE_DIR="build/serve"
rm -rf "$SERVE_DIR"
mkdir -p "$SERVE_DIR"
cp "$APK" "$SERVE_DIR/"

APK_NAME="$(basename "$APK")"
SIZE="$(du -h "$APK" | cut -f1)"

cat > "$SERVE_DIR/index.html" <<HTML
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Eink Launcher build</title></head>
<body style="font-family: sans-serif; font-size: 20px; padding: 24px;">
<h1>Eink Launcher</h1>
<p>Flavour: $FLAVOUR. Size: $SIZE.</p>
<p><a href="$APK_NAME">Download $APK_NAME</a></p>
</body>
</html>
HTML

IP="$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7; exit}' || true)"
if [ -z "${IP:-}" ]; then
  IP="$(hostname -I 2>/dev/null | awk '{print $1}' || echo 127.0.0.1)"
fi

echo
echo "Serving $SERVE_DIR on port $PORT"
echo
echo "  Open on the tablet:   http://$IP:$PORT/"
echo "  Direct APK URL:       http://$IP:$PORT/$APK_NAME"
echo
echo "Paste the direct URL into the Dev screen in the app. Stop with Ctrl+C."
echo

exec "$PYTHON" -m http.server "$PORT" --bind 0.0.0.0 --directory "$SERVE_DIR"
