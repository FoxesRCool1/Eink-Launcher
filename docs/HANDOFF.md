# Handoff

For the next session, wherever it runs. Rewritten 2026-09-20 by the session
that built steps 2, 5, 8 and 10 and finished steps 6, 7 and 9. Section 2b was
added the same day by the session that built the in-app update.

Read `CLAUDE.md` first for the rules, then this file for the state.

---

## 1. The one thing that shapes everything now

**All ten steps have code. None of it has run on the tablet.**

The dev machine can build now. The Android SDK is in `~/Android/Sdk`, and
every step was built, unit tested and linted locally and on GitHub Actions.
What nobody has done is hold the tablet. Every step has a device test list in
`PROGRESS.md`, and until those are run, treat the app as untested where it
touches the glass, the pen and the vendor firmware.

Run this first in any new session:

```
./gradlew assembleViwoodsDebug testViwoodsDebugUnitTest lintViwoodsDebug
```

Then look at the pictures in `app/build/outputs/roborazzi/`. They are the only
eyes there are.

---

## 2. Where the plan stands

| Step | State |
| --- | --- |
| 0. Owner preparation | Owner's job. Ask what ViWoods support said about the ADB tool. |
| 1. Scaffold and design system | Done. |
| 2. Device spike | Code done. **The device test decides everything about the pen.** Decision 0007 is provisional. |
| 3. Launcher shell | Done. |
| 4. Storage layer | Done. The 500 file measurement is still open, see 3.3. |
| 5. Ink engine | Code done. Own stroke model, not Jetpack Ink. Decision 0008 says why. |
| 6. Writing tab | Done. |
| 7. Reading, EPUB | Done, on Readium 3.4.0. **Judge it on the panel**, plan section 8. |
| 8. Reading, PDF | Code done, on the platform `PdfRenderer`. No PDF was rendered off the tablet. |
| 9. Journal | Done. |
| 10. Audit and release | Audit done from code and screenshots. Release workflow ready. **No release was made.** |

The work is in seven stacked pull requests, one per step branch:
`step-02-device-spike`, `step-05-ink-engine`, `step-06-writing-tab`,
`step-07-epub-reader`, `step-08-pdf-and-ink-annotations`,
`step-09-journal-handwriting`, `step-10-audit-and-release`. Merge them in
that order.

### 2b. The app updates itself now

Branch `in-app-updates`, on top of `dev-emulator`. Settings, Help, "Check for
updates" looks at the GitHub Releases of this repository, downloads the APK
that fits the build, and hands it to Android. `tools/release.sh 0.1.1 "what
changed"` makes a release with one command. Decision 0012 and
`docs/RELEASING.md` have the whole story. What a new session must know:

- **The whole update ran in the emulator**, from 0.1.0 to 0.1.1, against
  `tools/fake-github.py`. It has not run against a real GitHub Release, and
  the Release workflow has never run. The first `tools/release.sh` is the
  test of both. If the workflow fails, read its log before anything else.
- **The repository is private, and GitHub hides the releases of a private
  repository.** Either it is made public, or the owner puts a read-only
  access token into the update screen once. That choice is the owner's.
- The tablet runs the **debug** build while the app is tested. The updater
  in a debug build only takes `-debug.apk` files, and the workflow builds
  those with no release key. Moving to the release build later means an
  uninstall, and an uninstall deletes the data folder: back up first.
- The version is one line, `appVersionName` in `app/build.gradle.kts`. The
  version code comes from it. Do not set either by hand, use the script.
- The app has the internet permission now. `CLAUDE.md` has the network rules
  that keep that honest. Read them before adding anything that goes online.

---

## 3. What to do next, in order

### 3.1 The owner runs the device tests

`PROGRESS.md`, from the bottom up: step 2 first, because the pen setting for
everything else comes out of it. Then send the log file. The log holds the
vendor method list, which call route worked, render times, paint times and
the start-up time.

### 3.2 Turn what the log says into code

- The real package names of the ViWoods settings app and the stock launcher
  replace the name guess in `AppsRepository.escapeEntries`. See 4.2.
- The working fast pen path becomes the default in `SettingsStore`, and
  `PenWidths.vendorRange` gets matched by eye.
- Decision 0007 loses the word "provisional".
- If path A works at target SDK 37, raise the `viwoods` flavour and turn the
  two lint checks back on.

### 3.3 Run the storage measurement

Still open from step 4. Settings, Help, "Design demo", Dev, "Time 500 files".
`docs/decisions/0005-storage-layer.md` explains what the numbers decide.

### 3.4 Then the known gaps

1. A paged typed editor. A long note scrolls inside its field today, which
   breaks e-ink rule 2. See decision 0011.
2. Screenshot comparison in CI. Blocked by one screenshot that shows a
   temporary path. See decision 0011.
3. A read-ahead for PDF screens, only if the log says the render is slow.
4. Cover pictures in the library. The plan calls them optional.
5. The first public release. `docs/RELEASING.md` has the steps, and the
   release key is the one thing still to make. Pick the final name first:
   the plan says the working name must go before a public release. Test
   releases for the tablet need neither.
6. `TextPromptDialog` does not take the focus when it opens, so every prompt
   costs one extra tap before the keyboard comes. Small, and it touches every
   prompt in the app, so it wants a look on the tablet first.

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

### 4.3 The fast pen is off by default

`SettingsStore.fastPenMode` starts at "off", so this app paints the ink and
the pen will lag. That is on purpose: one of the two fast paths is said to
crash the process in native code, and a home app should not try that on its
own. The owner turns it on after the device test. `FastPenGuard` makes sure
a crash can happen once and not twice.

### 4.3b The engine does not use Jetpack Ink

The plan names it. Decision 0008 says why not: its model needs native code,
so the unit tests the plan asks for could not run. Jetpack Ink is a debug
only dependency, for the latency baseline on the device test screen.

### 4.4 `desugar_jdk_libs` is GPL-2.0 with the Classpath Exception

Readium needs core library desugaring. The library behind that is a cut of
OpenJDK, under the same licence as the Java class library itself. The
exception means linking it does not make the app GPL. It is written up in
`licenses/DEPENDENCIES.md`. The owner should read that paragraph once and
agree, because plan section 4 says "no GPL".

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
6. **The Kotlin compiler is set in the root build file.** AGP 9.4.0 would
   compile with Kotlin 2.2.10. Readium 3.4.0 needs 2.4, so
   `build.gradle.kts` puts the Kotlin Gradle plugin 2.4.20 on the build
   classpath. If a dependency ever says "compiled with an incompatible
   version of Kotlin", that line is where to look.
7. **Robolectric runs at API 36**, set in
   `app/src/test/resources/robolectric.properties`, while the app compiles
   against API 37. Raise it when Robolectric ships an API 37 sandbox.

---

## 6. Useful commands

```
./gradlew assembleViwoodsDebug          build for the tablet
./gradlew assembleGenericDebug          build for any other Android device
./gradlew testViwoodsDebugUnitTest      unit tests, and write the screenshots
./gradlew lintViwoodsDebug              lint
./gradlew assembleViwoodsRelease        release build, signed if the key is set
tools/deploy.sh viwoods 8000            build and serve the APK on the LAN
tools/deploy.sh viwoods 8000 37         the same, with target SDK 37
tools/parse-check.sh                    syntax check with no Android SDK
tools/release.sh 0.1.1 "what changed"   tag a release, GitHub builds it
tools/emulator.sh                       the app in an emulator on the dev machine
tools/fake-github.py <apk> 0.1.1        try the in-app update with no release
```

Screenshots land in `app/build/outputs/roborazzi/`.
