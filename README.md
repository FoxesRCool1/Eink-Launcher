# Eink Launcher

An Android launcher for e-ink tablets. It replaces the home screen with a
calm, text first screen: Reading, Writing, Journal and Apps.

Working name. The final name comes before the first public release.

Target device: ViWoods AiPaper Mini (8.2 inch E Ink Carta 1000, 1440 x 1920,
Wacom EMR stylus, Android 13).

Status: early. See `PROGRESS.md` for what works today and `plan.md` for the
full build plan.

## What it is meant to be

- Black on white. Large serif words. Small capitals for labels. A lot of white
  space. One fine line drawing in a corner.
- No animations at all. Pages turn, they do not scroll.
- Plain files as the source of truth, in one folder the user can see and sync.

## Build

You need JDK 17 or newer and the Android SDK (`compileSdk 37`). Nothing else.
Android Studio is not required.

```
./gradlew assembleViwoodsDebug    # build for a ViWoods tablet
./gradlew assembleGenericDebug    # build for any other Android device
./gradlew testViwoodsDebugUnitTest
./gradlew recordRoborazziViwoodsDebug   # write screenshots to app/build/outputs/roborazzi
```

Two flavours:

- `viwoods`: uses the hidden ViWoods display and pen APIs through reflection,
  with every call guarded.
- `generic`: no vendor APIs. Jetpack Ink rendering only.

## Getting a build onto a ViWoods tablet

`adb install` does not work on this device. Use the local network instead.

```
tools/deploy.sh viwoods 8000
```

The script builds the APK, serves it on the local network and prints a URL.
Open that URL in the tablet browser, or press "Get latest build" on the Dev
screen inside a debug build.

## Credits

- The idea came from "Prose: The distraction-free, e-ink laptop that should
  exist" by Micah Daigle. The article and its images are CC BY-SA 4.0. No image
  and no layout from that article is copied, traced or reused here.
- The ViWoods display and pen research in `jdkruzr/ViwoodsAppDev` is the source
  of the facts about the hidden APIs. No code from that repository is copied.
- Fonts: Bodoni Moda, Jost and Literata, all SIL OFL 1.1. See `ASSETS.md`.

## Licence

Apache-2.0. See `LICENSE`.
