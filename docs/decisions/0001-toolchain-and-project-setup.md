# 0001. Toolchain and project setup

Date: 2026-09-19
Status: accepted
Step: 1

## Decision

| Topic | Choice | Why |
| --- | --- | --- |
| Language | Kotlin, the version AGP brings | See "Built-in Kotlin" below. |
| Build | Gradle 9.6.0 wrapper, AGP 9.4.0 | Current stable pair. AGP 9.4 needs Gradle 9.6 and JDK 17 or newer. |
| compileSdk / targetSdk | 37 (Android 17) | Current stable platform. `androidx.core:core-ktx` 1.19.0 needs compileSdk 37. |
| minSdk | 29 | The plan sets it. The tablet runs Android 13. MediaStore Downloads, which the log export uses, needs API 29. |
| UI | Jetpack Compose, BOM 2026.09.00 | The plan sets it. |
| Compose Material | Not used | Material adds ripples, elevation and animated indication. E-ink rule 1 and rule 5 forbid all three. `androidx.compose.foundation` gives everything this design needs. |
| Navigation library | Not used | A launcher has four tabs and a few screens. A `when` on a state value costs nothing, starts faster and cannot animate. Readers and editors get their own activities in Step 3. |
| Screenshot tests | Roborazzi 1.74.0 library on Robolectric 4.17, without the Roborazzi Gradle plugin | The plan sets Roborazzi. The plugin only adds record and verify tasks; setting `roborazzi.test.record` on the test task does the same with one less plugin to break on an AGP upgrade. The tests record at 480 dp by 640 dp at xxhdpi, which is 1440 by 1920 pixels, the panel size. |
| Package id | `io.github.foxesrcool1.einklauncher` | The plan suggests it. The owner must confirm. |
| Licence | Apache-2.0 | The plan default. The owner must confirm. |

## Built-in Kotlin

AGP 9 compiles Kotlin itself. The first CI run failed with:

```
The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin
support since AGP 9.0.
```

So this project applies one plugin, `com.android.application`, and nothing
else. There are three results:

1. There is no Kotlin Gradle plugin version to keep in step with AGP.
2. The Compose Compiler Gradle plugin is still needed and is still separate.
   AGP refuses `buildFeatures { compose = true }` without it:
   "Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required
   when compose is enabled." Its version is a Kotlin version, so keep
   `kotlin` in the version catalogue at the Kotlin release AGP compiles with.
3. `kotlin.compilerOptions.jvmTarget` is not set, because it already defaults
   to `android.compileOptions.targetCompatibility`, which is 17.

Kotlin source directories are registered by hand:

```kotlin
sourceSets.configureEach {
    kotlin.directories.add("src/$name/kotlin")
}
```

`srcDir` is deprecated under built-in Kotlin. Use the `directories` set.

This keeps `.kt` files out of `src/<set>/java`, which is the usual place to
hide them.

## Debug signing key

`keystore/debug.keystore` is in the repository on purpose. Every debug build
then carries the same signature, so a new build installs over the old one on
the tablet and keeps the app data. Without a fixed key each build would be a
different app to Android and the owner would have to uninstall first.

The key signs debug builds only. The release key never enters the repository.
`.gitignore` blocks `keystore/release.keystore`, `*.jks` and
`*.keystore.properties`.

## Robolectric sandbox level

Robolectric 4.17 runs the tests at API 36, set in
`app/src/test/resources/robolectric.properties`. The app itself compiles and
ships against API 37. Raise the test level when a Robolectric release adds a
full API 37 sandbox.

## Open points

- The Gradle distribution checksum is not pinned in
  `gradle/wrapper/gradle-wrapper.properties`. The machine that set the project
  up could not reach `services.gradle.org` for the `.sha256` file. Add
  `distributionSha256Sum` when a machine with normal network access can fetch it.
