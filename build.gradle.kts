// AGP 9 compiles Kotlin itself, with the Kotlin Gradle plugin it was built
// against (2.2.10 for AGP 9.4.0). Readium 3.4.0 is compiled with Kotlin 2.4,
// and an older compiler cannot read it. Putting the newer plugin on the build
// classpath is the documented way to make built-in Kotlin use it. Keep this
// at the same version as the Compose compiler plugin in the version catalogue.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
