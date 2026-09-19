# Progress

One section per step. Newest step at the top.

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
