#!/usr/bin/env bash
#
# Runs Eink Launcher in an Android emulator on the dev machine.
#
#   tools/emulator.sh            build, start the emulator, install, open the app
#   tools/emulator.sh install    build and install again into a running emulator
#   tools/emulator.sh log        copy the app's log files out to build/emulator-logs/
#   tools/emulator.sh shot       save a picture of the screen to build/emulator-shots/
#   tools/emulator.sh grey       show the screen in grey, like the panel
#   tools/emulator.sh colour     back to colour
#   tools/emulator.sh stop       shut the emulator down
#
# The emulator is an Android 13 tablet, 1440 x 1920, the size of the AiPaper
# Mini panel. It is not e-ink: it cannot show ghosting or refresh, and it has
# no ViWoods pen calls, so the app uses its generic device layer there. It is
# for layout, flow and bugs. The tablet tests in PROGRESS.md still stand.
#
# The mouse draws on the ink canvas in a debug build on an emulator, and only
# there. See core/eink/DevEnvironment.kt.

set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"

AVD="eink_tablet"
IMAGE="system-images;android-33;google_apis;x86_64"
PACKAGE="io.github.foxesrcool1.einklauncher"
# 320 dpi is what a 292 PPI panel most likely reports. Pass EINK_DENSITY=480
# to see the layout the screenshot tests use.
DENSITY="${EINK_DENSITY:-320}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

for tool in "$ADB" "$EMULATOR" "$AVDMANAGER"; do
  if [ ! -x "$tool" ]; then
    echo "Missing $tool. The Android SDK should be in $ANDROID_HOME." >&2
    exit 1
  fi
done

running() { "$ADB" devices | grep -q "^emulator-"; }

serial() { "$ADB" devices | awk '/^emulator-/ {print $1; exit}'; }

create_avd() {
  if "$EMULATOR" -list-avds 2>/dev/null | grep -qx "$AVD"; then return; fi
  echo "Making the emulator $AVD"
  echo "no" | "$AVDMANAGER" create avd --name "$AVD" --package "$IMAGE" --device "pixel_tablet" --force > /dev/null
  local config="$HOME/.android/avd/$AVD.avd/config.ini"
  # The panel of the AiPaper Mini, upright, with no phone frame around it.
  sed -i \
    -e '/^hw.lcd.width/d' -e '/^hw.lcd.height/d' -e '/^hw.lcd.density/d' \
    -e '/^hw.initialOrientation/d' -e '/^showDeviceFrame/d' -e '/^hw.keyboard=/d' \
    -e '/^hw.ramSize/d' -e '/^skin\./d' -e '/^hw.gpu\./d' "$config"
  cat >> "$config" <<CONFIG
hw.lcd.width=1440
hw.lcd.height=1920
hw.lcd.density=$DENSITY
hw.initialOrientation=portrait
showDeviceFrame=no
hw.keyboard=yes
hw.ramSize=4096
hw.gpu.enabled=yes
hw.gpu.mode=auto
CONFIG
}

start_emulator() {
  if running; then return; fi
  echo "Starting the emulator. The first start takes a minute or two."
  mkdir -p build
  # -scale keeps a 1920 pixel tall window on a normal monitor.
  nohup "$EMULATOR" -avd "$AVD" -no-boot-anim -no-snapshot-save -no-audio \
    > build/emulator.out 2>&1 &
  "$ADB" wait-for-device
  until [ "$("$ADB" -s "$(serial)" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
  done
  local s; s="$(serial)"
  # The app has no animations. The system around it should not have any
  # either, or a dialog of the system looks like a bug of the app.
  "$ADB" -s "$s" shell settings put global window_animation_scale 0
  "$ADB" -s "$s" shell settings put global transition_animation_scale 0
  "$ADB" -s "$s" shell settings put global animator_duration_scale 0
  "$ADB" -s "$s" shell wm density "$DENSITY" > /dev/null
}

install_app() {
  echo "Building"
  ./gradlew assembleViwoodsDebug --console=plain -q < /dev/null
  local apk="app/build/outputs/apk/viwoods/debug/app-viwoods-debug.apk"
  echo "Installing"
  # -d lets an older build go over a newer one. Only a debug build allows that,
  # and the update tests leave a higher version behind.
  "$ADB" -s "$(serial)" install -r -d "$apk" > /dev/null
}

push_samples() {
  local s; s="$(serial)"
  mkdir -p build/samples
  if [ ! -s build/samples/Walden.epub ]; then
    # Public domain. Project Gutenberg ebook 205. Only for this emulator, never
    # for the repository: build/ is git ignored.
    curl -sSL --max-time 60 -o build/samples/Walden.epub \
      "https://www.gutenberg.org/ebooks/205.epub3.images" || rm -f build/samples/Walden.epub
  fi
  if [ ! -s build/samples/Test-pages.pdf ]; then
    /usr/bin/python3 tools/make-test-pdf.py build/samples/Test-pages.pdf
  fi
  for file in build/samples/*; do
    [ -s "$file" ] && "$ADB" -s "$s" push "$file" /sdcard/Download/ > /dev/null
  done
}

open_app() {
  "$ADB" -s "$(serial)" shell am start -n "$PACKAGE/.HomeActivity" > /dev/null
}

need_running() {
  if ! running; then
    echo "The emulator is not running. Start it with: tools/emulator.sh" >&2
    exit 1
  fi
}

case "${1:-start}" in
  start)
    create_avd
    start_emulator
    install_app
    push_samples
    open_app
    cat <<TIPS

Eink Launcher is open in the emulator window.

  Mouse click     a finger tap
  Mouse drag      draws on a handwriting page. In a PDF, press "Mouse turns"
                  in the toolbar to switch between drawing and page turns.
  Arrow keys      turn pages in a book
  Esc             Back
  Your keyboard   types into notes

  Two sample files are in the Download folder of the emulator:
  Walden.epub and Test-pages.pdf. In the app: Read, Import.

  After you change code:    tools/emulator.sh install
  To send me what happened: tools/emulator.sh log     and     tools/emulator.sh shot
  To stop:                  tools/emulator.sh stop
TIPS
    ;;

  install)
    need_running
    install_app
    open_app
    echo "Installed and open."
    ;;

  log)
    need_running
    out="build/emulator-logs"
    rm -rf "$out" && mkdir -p "$out"
    # The app keeps its logs in its private files. A debug build lets run-as read them.
    for name in $("$ADB" -s "$(serial)" shell run-as "$PACKAGE" ls files/logs 2>/dev/null | tr -d '\r'); do
      "$ADB" -s "$(serial)" shell run-as "$PACKAGE" cat "files/logs/$name" > "$out/$name"
    done
    "$ADB" -s "$(serial)" logcat -d -t 2000 "*:W" > "$out/logcat-warnings.txt" 2>/dev/null || true
    echo "Logs are in $ROOT/$out"
    ls "$out"
    ;;

  shot)
    need_running
    out="build/emulator-shots"
    mkdir -p "$out"
    file="$out/$(date +%Y%m%d-%H%M%S).png"
    "$ADB" -s "$(serial)" exec-out screencap -p > "$file"
    echo "$ROOT/$file"
    ;;

  grey)
    need_running
    "$ADB" -s "$(serial)" shell settings put secure accessibility_display_daltonizer_enabled 1
    "$ADB" -s "$(serial)" shell settings put secure accessibility_display_daltonizer 0
    echo "The screen is grey now."
    ;;

  colour|color)
    need_running
    "$ADB" -s "$(serial)" shell settings put secure accessibility_display_daltonizer_enabled 0
    echo "The screen is in colour again."
    ;;

  stop)
    if running; then "$ADB" -s "$(serial)" emu kill > /dev/null; echo "Stopped."; else echo "It was not running."; fi
    ;;

  *)
    sed -n '3,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
