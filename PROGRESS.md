# Progress

One section per step. Newest step at the top.

---

## Step 4. Storage layer

Model: Opus. Date: 2026-09-19. State: code and tests complete, waiting for CI
and for a device test.

### What was built

- `FileStore`: everything the app may do with the data folder, as one
  interface. `LocalFileStore` is the only implementation today and uses plain
  `java.io.File`.
- `StorageLayout`: which folder and which file name for every kind of thing,
  plus a name cleaner that survives FAT32 and a synced Windows folder.
- `RelativePaths`: the path rules. `..`, a leading slash, a drive letter and a
  stray space are all refused rather than repaired.
- Safe writes: every write goes to a temporary file next to the target and is
  renamed over it, so a tablet that loses power keeps the last good version.
- `BackupArchive`: backup to zip and restore from zip, with the zip slip attack
  refused and counted.
- `LibraryIndex`: a list of what is in the folder, thrown away and rebuilt from
  the folder on demand. Written as lines, so no library is needed and a
  damaged line does not lose the rest.
- `DataRepository`: the one way in. Import a book, read and write notes and
  journal entries, find a free file name, back up, restore, rebuild the index.
- `StorageBenchmark`: the 500 file measurement the plan asks for, run from the
  Dev screen.
- Settings now shows the data folder path and has Back up, Restore and Rebuild
  index. Restore asks first.
- 60 or so unit tests across paths, layout, the file store, backup and
  restore, the index and the repository. All plain JUnit, no Robolectric, so
  they run fast.
- `docs/decisions/0005-storage-layer.md`.

### What does not work yet

- **The measurement is not done.** The plan asks for 500 files through the
  Storage Access Framework before choosing where the data folder lives. That
  has to happen on the tablet. Until then the folder is
  `Android/data/<package>/files/EinkLauncher`, which needs no permission but
  which a sync program cannot reach on Android 11 and later. The decision file
  explains what would change the choice.
- **Room is not in.** The plan names Room for the index. Room needs KSP, AGP 9
  compiles Kotlin itself now, and the KSP version has to match the Kotlin
  version AGP brings. That pairing cannot be tested on this machine. The index
  works without it and keeps the same shape, so swapping it in later is a
  contained change. The app writes its Kotlin version into the log at start-up
  so the right KSP version can be picked.
- No `SafFileStore` yet. It fits behind `FileStore` when the measurement says
  it is fast enough.

### Device test list for the owner

1. Open Settings. Read the data folder path. It should start with
   `/storage/emulated/0/Android/data/io.github.foxesrcool1.einklauncher`.
2. Press "Back up". A file picker should open. Save the zip somewhere you can
   find it.
3. Copy that zip to a computer and open it. It should hold `notes/`,
   `journal/` and the rest, with readable names.
4. Press "Rebuild index". Write down how long it takes.
5. Press "Restore" and pick the zip back. It should ask first, then say how
   many files it read.
6. Open Settings, then Design demo, then the Dev tab. Press "Time 500 files".
   Send me every number. That measurement decides where the data folder lives.

---

## Step 3. Launcher shell

Model: Opus. Date: 2026-09-19. State: code complete, waiting for CI and for a
device test.

### What was built

- `HomeActivity` with the HOME, DEFAULT and LAUNCHER categories, `singleTask`
  and `stateNotNeeded`. The Home key calls `onNewIntent`, which always goes
  back to Today. Back goes to Today from a tab and does nothing on Today.
- `DemoActivity` holds the design demo, the log viewer and the dev panel. Its
  own activity, so the launcher task stays clean.
- Today screen: date and time in small tracked capitals at the top centre, the
  four large serif words, a Wi-Fi and battery line, and a way into Settings.
  The clock listens to `ACTION_TIME_TICK`, so it repaints once a minute and
  never on a timer.
- Reading, Writing and Journal are placeholder screens that say which step
  builds them.
- Apps tab: `LauncherApps` with a `PackageManager` fallback, package add and
  remove events, pins in DataStore with a limit of 8, two pages (Pinned and
  All apps), long press for pin, unpin, app info and uninstall.
- "Always available" block: every other home app, any package with "viwoods"
  in its name, and the Android settings app. Plan section 3.2: never trap the
  user.
- Settings screen: home app state and the three ways to set it, the corner
  drawing switch, the log viewer and the design demo.
- New design system parts: `EinkRow` (invert on press, long press, no ripple)
  and `OptionsDialog`.
- Tests: `HomeStringsTest`, `LauncherRouteTest`, `LauncherEntryTest` and three
  more screenshots (Today, Today without art, a placeholder tab).
- `docs/decisions/0004-launcher-shell.md`.

### What does not work yet

- The build still has not run on this machine, for the same network reason as
  Step 1. `tools/parse-check.sh` is clean. CI decides.
- The third CI run got past configuration and then sat in the build step until
  the job timed out. The workflow now installs the Android SDK packages in a
  step of its own, runs Gradle once instead of three times, reads from
  `/dev/null` so nothing can block on a console prompt, and gives every long
  step its own timeout. A hang now names the step it is in.
- The ViWoods settings app is found by looking for "viwoods" in a package name.
  That is a guess. The owner must read the real package name off the tablet
  from the log, and then this becomes an exact match.
- Step 2 has not run, so there is no `EinkDevice` and no full refresh yet.
- The "Next" line and Quick note on Today are not built. They belong to Step 9
  and Step 6.

### Device test list for the owner

1. Install the build. Open Eink Launcher from the stock launcher.
2. You should see Today: the date and time at the top centre, then Read,
   Write, Journal and Apps in large serif, then a status line.
3. Press Apps. The Pinned page should list "Always available" entries. Write
   down the exact names you see there.
4. Press each "Always available" entry in turn. Write down which ones open the
   ViWoods settings and which one opens the stock launcher.
5. Go to All apps. Turn the pages. Hold an app and press Pin. Go back to
   Pinned and check that it is there.
6. Hold a pinned app and press Unpin.
7. Go to Settings and press "Set as home". Write down exactly what happens:
   a system dialog, a settings screen, or the written steps.
8. If it worked, press the Home key from inside another app. You should land
   on Today, not on the last tab you had open.
9. Press the Back key on Today. Nothing should happen. Press Back inside a tab.
   You should land on Today.
10. Leave the tablet on Today for two minutes and watch the clock. It should
    change once a minute and leave no smear.
11. Open Settings, press Log, then "Copy to download". Send me the log file.
    It tells me which route the home app flow took and what the ViWoods
    settings package is really called.

---
## Step 1. Project scaffold and design system

Model: Opus. Date: 2026-09-19. State: code complete, waiting for CI and for a
device test.

### What was built

**Build**

- Gradle 9.6.0 wrapper, Android Gradle Plugin 9.4.0, Kotlin 2.4.20.
- `compileSdk 37`, `targetSdk 37`, `minSdk 29`.
- Two flavours: `viwoods` and `generic`. Both build the same code today. Step 2
  decides whether `viwoods` has to drop to `targetSdk 30`.
- Fixed debug keystore at `keystore/debug.keystore`, checked in on purpose, so
  every debug build installs over the last one on the tablet.
- Version catalogue at `gradle/libs.versions.toml`. Every version was checked on
  the web on 2026-09-19.
- GitHub Actions workflow: builds both flavours, runs the unit tests, records
  the screenshots, runs lint, and uploads the APKs, the screenshots and the
  reports as artifacts.

**Design system** (`app/src/main/kotlin/.../design/`)

- `EinkColors`: black, white and one grey. Nothing else.
- `EinkType`: Bodoni Moda for large words, Jost for capitals, Literata for
  body. All three SIL OFL, all listed in `ASSETS.md`.
- `EinkDimens`: 56 dp touch targets, 1 dp and 2 dp rules.
- `EinkTheme` and `Modifier.einkClickable`, which passes `indication = null` so
  no ripple can ever appear.
- Components: `EinkText`, `CapsLabel`, `HairlineDivider`, `InvertPressButton`,
  `WordMenu`, `PagedList`, `ConfirmDialog`, `BotanicalSprig`.
- The app does not depend on Compose Material at all. That removes ripples,
  elevation and animated indication at the dependency level, not by discipline.

**Logging** (`app/src/main/kotlin/.../core/log/`)

- `AppLog`: one file per day in `filesDir/logs/`, a 600 line memory buffer for
  the viewer, a background writer thread, and files older than 14 days deleted
  at start-up.
- `CrashHandler`: writes a crash file and copies it straight into
  `Download/EinkLauncher/`, then hands over to the previous handler.
- Log viewer screen with Refresh and "Copy to download".

**Getting a build onto the tablet**

- `tools/deploy.sh viwoods 8000` builds the APK, serves it on the local network
  and prints the URL.
- Debug builds have a Dev screen with the build URL and a "Get latest build"
  button. It downloads the APK and opens the system installer. Only the debug
  build asks for the internet and install permissions; the release build asks
  for neither.

**Tests**

- `PaginationTest`: 8 tests on the page maths, including an empty list, a part
  full last page and an out of range page index.
- `LogFormatTest`: 6 tests on file names, time stamps and file expiry.
- `DesignSystemScreenshotTest`: 8 Roborazzi screenshots at 480 dp by 640 dp at
  xxhdpi, which is 1440 by 1920 pixels, the panel size. One of them presses a
  button and checks the colour invert.

### What does not work yet

- **The build was not run on this machine.** The machine that wrote this step
  cannot reach `dl.google.com`, so it could not download the Android SDK or the
  Android Gradle Plugin. Every Kotlin file was parsed with the standalone
  Kotlin compiler instead (`tools/parse-check.sh`) and no syntax error and no
  unknown name inside this project was found. **CI is the first real build.**
  If CI is red, that is the first thing to fix next session.
- The screenshots have no recorded baseline yet, so CI records them and uploads
  them as an artifact. It does not compare them. Turn on comparison once the
  first set is reviewed and committed.
- `gradle/wrapper/gradle-wrapper.properties` has no `distributionSha256Sum`,
  because `services.gradle.org` could not be reached for the checksum file.
- No `EinkDevice` yet. That is Step 2.

### The owner has to decide

1. **Package id.** It is `io.github.foxesrcool1.einklauncher` today. Confirm or
   change it now. Changing it later means every tester reinstalls from scratch.
2. **Licence.** Apache-2.0 is in `LICENSE`. Confirm or change.
3. **Name.** "Eink Launcher" is the working name.

### Device test list for the owner

Do these in order and write down what happens.

1. Run `tools/deploy.sh viwoods 8000` on the dev machine. It should print a URL.
2. Open that URL in the tablet browser and download the APK.
3. Install it. Android may ask to allow installs from the browser. Allow it.
4. Open the app. You should see the Design screen: four large words, three
   controls, and a paged list with Previous and Next.
5. Press and hold "Press me". It must turn to white text on a black box at
   once, with no fade and no ripple. Let go. It must come straight back.
6. Press "Next" on the paged list a few times, then "Previous". Watch for grey
   smear or ghosting. Write down what you see.
7. Swipe left and right across the list. One swipe must move exactly one page.
8. Press "Delete". A bordered panel should appear with Keep and Delete. Press
   Keep.
9. Look at the corner drawing. Say whether the 1 dp line is too thin on the
   panel.
10. Go to the Log tab. You should see lines, starting with "Eink Launcher
    0.1.0-viwoods ... started".
11. Press "Copy to download". Open the tablet file manager and check that
    `Download/EinkLauncher/` holds a `log-...txt` file.
12. Go to the Dev tab. Check that the build URL is the one the script printed.
    Press "Get latest build". It should download and open the installer.
13. Tell me: which text is too small, which is too large, and whether the grey
    in the disabled control is readable or just dirty.
