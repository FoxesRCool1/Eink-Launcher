# Rules for Claude Code

Read this file at the start of every session. It is the short form of
`plan.md` sections 1 and 5. `plan.md` wins if the two ever disagree.

## Working rules

1. Do one step of `plan.md` section 7 per session. Stop at the end of the step.
2. Each step has a model tag. If Opus fails the same task two times, tell the
   owner to switch to Fable for that task.
3. Keep `PROGRESS.md` current: what you did, what works, what does not work,
   and what the owner must test on the tablet.
4. Keep `docs/decisions/` current. One short file per important decision.
5. Do not trust library versions from memory. Check the current stable version
   on the web before you add or raise a dependency. Ask the owner before you
   add a dependency that is not in `plan.md`.
6. Check the licence of every dependency and every asset. No GPL. No AGPL.
   Record every asset in `ASSETS.md`.
7. You cannot see the tablet. `adb shell`, `adb install` and logcat do not work
   on this device. Use unit tests, screenshot tests and the in-app log. End
   every step with a short device test list for the owner.
8. Write plain English in the app and in the docs. Do not use em dashes.

## E-ink rules (mandatory in all UI code)

1. No animations. No transitions, no ripples, no crossfades, no animated
   scroll, no overscroll glow, no progress spinners.
2. Paginate, do not scroll. A list shows one page of rows with Previous and
   Next. A swipe means one page turn.
3. Pure black `#000000` on pure white `#FFFFFF`. No cream. No beige.
4. Grey is for large inactive text only.
5. The pressed state is an instant colour invert. No shadows, no elevation,
   no gradients. Lines are 1 dp or 2 dp.
6. Touch targets are 56 dp or more, with space between them for finger and pen.
7. Change as few pixels as possible per update. The clock updates once a minute.
8. Ask for a full refresh through `EinkDevice` after a big screen change, once
   Step 2 proves that it works. It is a setting.
9. Dark theme is optional and comes later.

## Code rules that follow from the above

- The app does not depend on Compose Material. Material brings ripples,
  elevation and animated indication. Use `androidx.compose.foundation` and the
  components in `design/components/`.
- Every tap target goes through `Modifier.einkClickable`, which passes
  `indication = null`.
- Text goes through `EinkText` or `CapsLabel`, never `BasicText` directly.
- Lists go through `PagedList`. Do not add a `LazyColumn` to a user screen.
- Dialogs go through `EinkDialog`, never `Dialog`. The platform dialog fades
  and dims the screen behind it.
- Text fields go through `EinkTextField`, never `BasicTextField`. The platform
  cursor blinks.
- A screen that does not scroll can overflow. Add a screenshot test for every
  new screen and look at the PNG. A control that falls off the bottom of a
  home app is a trap.
- Every reflection call into a vendor API is wrapped in `runCatching` and
  writes its result to `AppLog`. A vendor API must never crash the app.

## Storage rules

- Plain files are the truth. The index is a cache and must always be
  rebuildable from the folder. If a file the user copied in by hand would be
  invisible, the design is wrong.
- Screens talk to `DataRepository`, never to `FileStore` and never to a raw
  path string.
- Every path goes through `RelativePaths.normalise`. A path that leaves the
  data folder is refused, not repaired. This matters most on restore: a zip
  can name an entry `../../somewhere`.
- Every write is atomic: temporary file, then rename. `LocalFileStore.write`
  already does this, so use it rather than opening a stream yourself.
- New kinds of file get a path builder in `StorageLayout`, not a string
  somewhere in a screen.

## Network rules

- The app goes online in one place, `core/update/`, and only when the user
  presses "Check for updates". Do not add a second place, a background check
  or a check at start-up. See `docs/decisions/0012-in-app-updates.md`.
- Release builds talk https only. Plain http is for the debug LAN update.
- A web view that shows book content has its network loads blocked.
- The version lives in one line, `appVersionName` in `app/build.gradle.kts`.
  `tools/release.sh` changes it. Do not set a version code by hand.

## Layout of the code

```
app/src/main/kotlin/io/github/foxesrcool1/einklauncher/
  core/log/      file logger, crash handler
  core/storage/  data folder, file store, backup, index
  core/settings/ the few values that live in DataStore
  core/launcher/ becoming the home app
  core/eink/     EinkDevice, the ViWoods layer by reflection, the crash guard
  core/ink/      strokes, file formats, undo, eraser geometry. No Android.
  core/reading/  annotations, reading log and timer. No Readium.
  core/pdf/      screens of a page, margin search, ink sidecar files
  core/update/   the in-app update from GitHub Releases. The only network code.
  ui/ink/        the ink canvas, the handwriting screen, export
  ui/reading/epub/  the Readium reader
  ui/reading/pdf/   the PDF reader
  design/        colours, type, sizes, theme
  design/components/  the design system
  ui/            screens
app/src/debug/   the dev panel, internet permission, file provider
app/src/release/ stubs for the debug only parts
app/src/test/    unit tests and Roborazzi screenshot tests
```

## Before you start

Read `docs/HANDOFF.md`. It says what is finished, what is deliberately not
finished, and which traps have already cost a day.

## Useful commands

```
./gradlew assembleViwoodsDebug          build the tablet APK
./gradlew testViwoodsDebugUnitTest      unit tests
./gradlew recordRoborazziViwoodsDebug   write the screenshots
tools/deploy.sh viwoods 8000            build and serve the APK on the LAN
tools/release.sh 0.1.1 "what changed"   tag a release. GitHub builds it, the app finds it
tools/emulator.sh                       run the app in an emulator on the dev machine
```
