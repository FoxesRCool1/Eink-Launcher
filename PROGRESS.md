# Progress

One section per step. Newest step at the top.

---

## Step 2. Device spike: refresh control and fast pen

Model: Fable. Date: 2026-09-20. State: code and tests complete. **The device
test is not done**, and this step is only finished when it is.

The build now runs on the dev machine. The Android SDK is in `~/Android/Sdk`
and `local.properties` points at it. Every step from here on was built, tested
and linted locally before it was pushed.

### What was built

- `core/eink/`: `EinkDevice`, `GenericEinkDevice`, `ViwoodsEinkDevice`,
  `EinkDevices` and `FastPenGuard`. All vendor calls go through reflection,
  none can throw, and each one writes its result to the log.
- Three ways to reach a vendor call: the wrapper object, the binder service,
  and the `service call` shell command. The log says which one worked.
- `FastPenGuard`: a fast pen path that kills the app with a native crash is
  switched off at the next start. One crash at most.
- The `viwoods` flavour targets SDK 30. `tools/deploy.sh viwoods 8000 37`
  builds it with target SDK 37 for the comparison.
- Settings, then "Device test": device info, vendor API list, each picture
  mode, full refresh, four pen canvases (this app draws, Jetpack Ink, path A,
  path B), the crash guard, the home role, and a search for the ViWoods
  settings app and the stock launcher.
- The pen canvas has a toolbar that is handed to the tablet as "do not draw
  here", an eraser switch, four width ranges and four redraw waits.
- Jetpack Ink 1.0.0 is a debug only dependency, for the baseline canvas.
- 16 new unit tests. The vendor class is tested against a fake of the same
  shape.
- `docs/decisions/0007-viwoods-ink-and-refresh.md`.

### What does not work yet

- Nothing in `ViwoodsEinkDevice` has touched a real tablet. The public notes
  it is written from contradict themselves. Treat every call as unproved.

### Device test list for the owner

1. Run `tools/deploy.sh viwoods 8000` and install the build.
2. Open Settings, then "Device test". Press each row from the top. Read the
   answer under the row.
3. For each picture mode in row 3: leave the screen, turn some pages in the
   All apps list, and write down how the page turn looks.
4. Row 5, both canvases: write fast with the pen. Write down how far the ink
   is behind the pen tip.
5. Row 6, path A. If the app closes by itself, open it again, run row 8, and
   write that down. If it stays open: is the ink fast? Does ink appear on the
   toolbar? Press "Redraw" to change the wait, and find the wait where the
   stroke neither flickers nor doubles.
6. Row 7, path B. Same questions.
7. Try the Eraser and the Width button on the path that worked.
8. Run rows 9 and 10.
9. Press "Copy log to download" and send the log file.
10. Run `tools/deploy.sh viwoods 8000 37`, install, and do steps 5, 6 and 9
    again. That tells us whether target SDK 30 is really needed.

---

## Step 7 part one. The library and import

Model: Opus. Date: 2026-09-19. State: the library is built. **The reader is
not**, and that is on purpose. See below.

### What was built

- Import with the system file picker. The file is copied into `books/`. The
  app never reads a book from where the user picked it: a file outside the
  data folder can be moved or deleted, and then the reading position and every
  note point at nothing.
- `EpubMetadata`: reads the title and the author out of an EPUB by hand. An
  EPUB is a zip, `META-INF/container.xml` says which file inside is the
  package document, and that document holds `dc:title` and `dc:creator`. About
  a hundred lines, with 14 tests that build real EPUB files and read them
  back.
  - It refuses a document that declares a doctype. A book is a file from
    somewhere else, and an XML document can name an external entity and make a
    careless parser read a file off the device. There is a test for that
    attack.
- The library list: title, author, kind and size, sorted by recent or by
  title, paginated. Hold a row for the details and Delete.
- Deleting a book takes its annotations with it, so nothing is left behind
  that no book can open.

### Why there is no reader yet

Step 7 builds the reader on the Readium toolkit. Plan section 8 lists
"Readium WebView is slow or ghosts on e-ink" as a risk and says to judge it
before building more on it. That judgement needs the tablet.

The library, the import and the title reading do not depend on that decision,
so they are here now. Tapping a book says so rather than pretending.

**When you add Readium:** check its current version and licence on the web
first. It should be BSD-3. `EpubMetadata` can go once Readium is reading the
same fields, or stay as the fast path for the list. Either is fine; say which
in a decision file.

### Device test list for the owner

1. Open Read from Today. It should say the library is empty.
2. Press Import. Pick a public domain EPUB from Standard Ebooks.
3. The row should show the real title and author from inside the file, not the
   file name.
4. Import the same file again. It should say it is already in the library.
5. Import a PDF. It should appear with the file name as its title.
6. Hold a row and delete it.
7. Check the data folder with a file manager. The books should be in
   `EinkLauncher/books/`.

---

## CI went green

2026-09-19, at commit `9cbfc04`, run 9.

Everything passes at once for the first time:

- Both flavours build. The APKs are about 24 MB together.
- Every unit test passes.
- All twelve screenshots are recorded at 1440 by 1920 and uploaded.
- Lint passes with `abortOnError = true`.

It took nine runs. What was wrong each time, in order:

1. AGP 9 compiles Kotlin itself and refuses `org.jetbrains.kotlin.android`.
2. AGP still needs the Compose compiler plugin, which I had removed too eagerly.
3. The SDK install step looked for `sdkmanager` in a place this runner does
   not keep it.
4. Robolectric could not reach into `java.io` on a sealed JDK, and at API 36 it
   takes a path that needs to. Fixed with `--add-opens` and by pinning the test
   sandbox to API 35.
5. One test was wrong: it expected `///` to mean the data folder.

Two of those runs were cancelled by my own pushes before they could report.
The workflow no longer cancels a run that is already going.

**The screenshots have no baseline yet.** CI records them and uploads them as
an artifact; it does not compare them. Look at them, commit them, then turn on
comparison. That is the next thing to do.

---

## Step 6. Writing tab, typed half

Model: Opus. Date: 2026-09-19. State: the typed half is built. The handwritten
notebook waits for the ink engine, which is step 5 and is tagged for Fable.

### What was built

- `NotesRepository`: list, read, write, make a note, make a folder, rename,
  move and delete, all over the storage layer, so a note can never be written
  outside the user's folder. A folder cannot be moved into itself, which would
  otherwise make it and everything in it disappear.
- `NoteText`: word count, character count, and the title of a note. The title
  is the first Markdown heading, or the first line with something on it, or
  the file name. A heading marker does not make the word count go up.
- The file browser: folders first, paginated at five rows a page. A row shows
  the title from inside the note and the first line after it.
- Moving is done by marking something and pressing "Paste here" in the folder
  it should land in. Typing a path on a tablet with no keyboard is worse than
  two taps, and dragging on e-ink needs an animation, which rule 1 forbids.
- Delete asks first, and says whether it is about to take a folder with it.
- `NoteEditorActivity`: its own activity, as the plan says, so the Home key
  closes the launcher and not the note being written. It autosaves once the
  typing stops for a second and a half, and again on the way out.
- Bluetooth keyboard: Escape leaves the editor, Ctrl+S saves, Ctrl+N makes a
  note from anywhere and opens the Writing tab.
- Quick note from Today: one tap makes a note named after the moment and opens
  it.

### What is left

- The handwritten notebook, after step 5.
- Export to PDF and PNG.

### Device test list for the owner

1. Open Write from Today. Press "New note", type a title, press Write.
2. The editor should open. Type a sentence. Wait two seconds. The line under
   the text should change from "Not saved yet" to "Saved".
3. Press the Home key. You should land on Today, not in the note.
4. Open Write again. The note should be there with your first line under the
   title.
5. Make a folder. Hold the note, choose Move, open the folder, press "Paste
   here".
6. Hold the note and delete it. It should ask first.
7. From Today, press "Quick note". A note named after the time should open.
8. With a Bluetooth keyboard: press Ctrl+N from Today, then Escape in the
   editor. Write down whether both worked.

---

## Step 9. Journal tab

Model: Opus. Date: 2026-09-19. State: most of it built. Waiting for CI and for
a device test.

Two parts are missing on purpose. The handwritten entry needs the ink engine,
which is step 5 and is tagged for Fable. The routine list and the "Next" line
on Today are still to build.

### What was built

- A small JSON reader and writer (`core/json/`). The plan writes
  `habits.json` and `annotations/<book-id>.json`, so something had to read
  JSON. `org.json` is stubbed out in a plain unit test, which would let a test
  pass while the real code did nothing, and every Kotlin serialisation library
  carries a compiler plugin tied to a Kotlin version that AGP now owns. This
  is about 250 lines, refuses a document nested more than 64 deep, and has 16
  tests including every broken input I could think of.
- `DayBoundary`: where one day ends. Midnight is the wrong line for a habit
  tracker, because someone who reads until half past one has not started
  tomorrow. The hour is a setting and the default is 04:00, as the plan says.
  Tested across time zones and across the day New Zealand puts its clocks
  forward.
- `HabitStreaks`: current streak, longest streak, and the row of 14 dots. A
  day that is not finished yet does not break a streak: if yesterday is marked
  and today is not, the streak still stands and the user still has today.
- `HabitsFile` and `HabitLogFile`: `habits/habits.json` and `habits/log.csv`,
  both meant to be read and edited by a person. One broken habit or one broken
  line is dropped rather than losing the rest.
- `HabitsRepository`: add, rename, archive, mark a day, and a summary for each
  row of the Journal tab. Archiving never deletes, so a year of ticks cannot
  be lost to one tap.
- About 60 more unit tests.

### The screens

- Day view: the date, Previous and Next, the typed entry with Write, Edit,
  Save and Cancel, and the habits.
- Month view: whole weeks starting on Monday. A day with an entry carries a
  short rule under its number, not a filled cell, because a filled cell on
  this panel ghosts. Tap a day to open it.
- Habit rows: the name, the streak in words, and fourteen dots for the last
  fourteen days. Tap to mark today done or undone. Hold for rename, archive
  and the longest streak.
- Add a habit through a one line prompt.
- New design system parts: `TextPromptDialog`.

### The routine

- `habits/routine.json` holds the list, `habits/routine-log.csv` holds what
  was ticked off on which day. Two files, so ticking something off never
  rewrites the list.
- The Journal tab has a Routine page. A tap ticks an item off. The order is
  changed with Up and Down arrows rather than by dragging, because a drag
  needs a moving picture under the finger and rule 1 has no exceptions.
- Today shows the next item that is not ticked off, with a Start control.
  "Next" means the first one in the user's own order, not the nearest in time.
  When the routine is empty or finished the line is left out entirely rather
  than shown blank, because a blank line on e-ink is a repaint for nothing.
- An item can open a tab in this app, open another app, or open nothing at
  all. An item that opens nothing is ticked off by its own Start control.

### What is left for step 9

- The handwritten entry. The "Handwrite" control is there and disabled until
  step 5 exists.

### Device test list for the owner

1. Open Journal from Today. You should land on today's date.
2. Press "Write", type a line, press "Save". Leave the tab and come back. The
   line should still be there.
3. Press "Add habit" and add one. Tap its row. The big dot should fill.
4. Tap it again. It should empty.
5. Press "Month". The month should fill one screen with no scrolling. The days
   you wrote on should carry a rule under the number.
6. Tap a day in the month. It should open that day.
6a. Press "Routine", add two items, then use Up and Down to swap them.
6b. Go back to Today. The first item should show under "Next" with a Start
    control. Press Start, then check the Journal shows it ticked off.
7. Hold a habit row. Rename it. The streak must not reset.
8. Watch for ghosting when you turn from Day to Month and back. Write down
   what you see.

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
