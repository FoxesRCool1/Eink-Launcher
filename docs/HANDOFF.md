# Handoff

For the next session, wherever it runs. Written 2026-09-19 by the session that
built steps 1, 3 and 4.

Read `CLAUDE.md` first for the rules, then this file for the state.

---

## 1. The one thing that shaped every decision here

The machine that wrote steps 1, 3 and 4 **could not reach `dl.google.com`**. No
Android SDK, no Android Gradle plugin, no androidx artifacts. So:

- `./gradlew` never ran on that machine. Not once.
- Every Kotlin file was checked with `tools/parse-check.sh` instead, which
  compiles the sources with the standalone Kotlin compiler and no Android
  classes. It catches syntax errors and names that do not exist in this
  project. It cannot catch a wrong argument to an androidx function.
- GitHub Actions was the only real build. Every round trip cost about seven
  minutes.

**If your machine can run `./gradlew`, that limit is gone.** Run the build
first, before anything else:

```
./gradlew assembleViwoodsDebug testViwoodsDebugUnitTest lintViwoodsDebug
```

Fix whatever it says, then carry on. Do not trust that the code is correct
because it is committed.

---

## 2. Where the plan stands

| Step | Model | State |
| --- | --- | --- |
| 0. Owner preparation | manual | Owner's job. Ask what happened with the ViWoods support email. |
| 1. Scaffold and design system | Opus | Code complete. See `PROGRESS.md`. |
| 2. Device spike: refresh and fast pen | **Fable** | Not started. Blocks step 5. |
| 3. Launcher shell | Opus | Code complete. |
| 4. Storage layer | Opus | Code and tests complete. One measurement still open, see below. |
| 5. Ink engine | **Fable** | Not started. Blocks the handwriting half of steps 6, 8 and 9. |
| 6. Writing tab | Opus | Not started. The typed half can be built without step 5. |
| 7. Reading tab, library and EPUB | Opus | Not started. Needs the Readium toolkit added. |
| 8. Reading tab, PDF | **Fable** | Not started. |
| 9. Journal tab | Opus | Mostly built: day view, month view, habits with streaks and dots, all tested. Missing the handwritten entry (needs step 5) and the routine list. |
| 10. Audit and release | Opus | Not started. |

Steps 2, 5 and 8 are tagged for Fable in the plan and were left alone on
purpose.

---

## 3. What to do next, in order

### 3.1 Get CI green if it is not

Check the CI badge or the Actions tab. The workflow prints a compact failure
summary on any failure: failing test names with their messages, the lint text
report, and whether the screenshots were written. That step is called "Show
what failed".

### 3.2 Turn the screenshots into a real check

CI records screenshots into `app/build/outputs/roborazzi/` and uploads them as
an artifact. Nothing compares them to anything yet.

Once the first set looks right on screen:

1. Download the artifact and look at every PNG.
2. Commit them as the baseline.
3. Switch CI from recording to comparing, so a change to the design system has
   to be looked at before it lands.

### 3.3 Decide the two things only the owner can decide

Both are in `PROGRESS.md` under step 1 and are still open:

1. **Package id.** `io.github.foxesrcool1.einklauncher` today. Changing it
   later means every tester reinstalls from scratch.
2. **Licence.** Apache-2.0 is in `LICENSE`.

### 3.4 Run the storage measurement

`docs/decisions/0005-storage-layer.md` explains it. Short version: the data
folder lives in `Android/data/<package>` today, which no sync program can
reach on Android 11 and later. The other option is a folder the user picks
through the Storage Access Framework, which is slower by an amount nobody has
measured on this tablet.

The Dev screen has a "Time 500 files" button. The owner runs it and sends the
numbers. Then write `SafFileStore` behind `FileStore` or do not, and record
which and why.

### 3.5 Then pick a step

- **Step 6, Writing**, is the best next Opus step. The typed half can be built
  now on the storage layer: a file browser over `DataRepository.listNotes`, a
  Markdown editor, and Bluetooth keyboard shortcuts. Leave a gap where the
  handwritten notebook goes and fill it after step 5.
- **Step 9, Journal**, is nearly done. What is left is the routine list and
  the "Next" line on Today, plus the handwritten entry once step 5 exists.
- **Step 6, Writing**, can be built for typed notes now. Leave a gap where the
  handwritten notebook goes and fill it after step 5.
- **Step 7, Reading**, needs the Readium Kotlin toolkit added. Check its
  current version on the web first and check the licence, which should be
  BSD-3.

---

## 4. Things that are deliberately not finished

### 4.1 Room is not in the index

The plan names Room. Room needs KSP. AGP 9 compiles Kotlin itself now, and the
KSP version has to match the Kotlin version AGP brings, which could not be
discovered or tested on a machine with no Android SDK.

`LibraryIndex` holds the same shape Room would and is rebuilt from the folder
on demand, which is what the plan says the index is for. To add Room:

1. Start the app and read the log. It writes `Kotlin <version>` at start-up,
   put there for exactly this.
2. Add the KSP plugin at that Kotlin version. KSP has supported AGP built-in
   Kotlin since KSP 2.3.1.
3. Keep `IndexEntry` and `IndexSnapshot`. Replace only how they are stored.

### 4.2 The ViWoods settings app is found by guessing

`AppsRepository.escapeEntries` looks for any package whose name contains
"viwoods". The real package name is not documented anywhere I could find.

**Ask the owner to send a log file after opening the Apps tab.** The log lists
every app. Replace the name match with the exact package once it is known.

### 4.2b There is a JSON reader in this project, on purpose

`core/json/` is about 250 lines and is used by `habits.json`. Do not replace
it with kotlinx.serialization without checking first: that library needs a
compiler plugin pinned to a Kotlin version, and AGP owns the Kotlin version
now. The same reasoning kept Room out. If you do swap it, the tests in
`core/json/JsonTest.kt` should keep passing unchanged.

### 4.3 No EinkDevice

Step 2 builds it. Until then there is no refresh mode control and no full
refresh, so ghosting on the tablet is whatever the stock firmware does.

### 4.4 The Gradle distribution checksum is not pinned

`gradle/wrapper/gradle-wrapper.properties` has no `distributionSha256Sum`,
because `services.gradle.org` could not be reached for the checksum file. Add
it from a machine with normal network access.

---

## 5. Traps that already cost time

1. **AGP 9 compiles Kotlin itself.** Applying `org.jetbrains.kotlin.android`
   fails the build. Do not add it back.
2. **The Compose compiler plugin is still separate.** AGP refuses
   `buildFeatures { compose = true }` without
   `org.jetbrains.kotlin.plugin.compose`.
3. **`sourceSets { kotlin.srcDir(...) }` is deprecated** under built-in
   Kotlin. Use `kotlin.directories.add(...)`.
4. **`--stacktrace` in CI is useless here.** It fills the log with Gradle
   internals and hides the real message. The workflow reads the reports
   instead.
5. **This project does not depend on Compose Material.** That is on purpose:
   Material brings ripples, elevation and animated indication, and e-ink rule
   1 forbids all three. Use the components in `design/components/`. If you
   find yourself adding Material to get one widget, write the widget instead.
6. **Robolectric runs at API 36**, set in
   `app/src/test/resources/robolectric.properties`, while the app compiles
   against API 37. Raise it when Robolectric ships an API 37 sandbox.

---

## 6. Useful commands

```
./gradlew assembleViwoodsDebug          build for the tablet
./gradlew assembleGenericDebug          build for any other Android device
./gradlew testViwoodsDebugUnitTest      unit tests, and write the screenshots
./gradlew lintViwoodsDebug              lint
tools/deploy.sh viwoods 8000            build and serve the APK on the LAN
tools/parse-check.sh                    syntax check with no Android SDK
```

Screenshots land in `app/build/outputs/roborazzi/`.
