plugins {
    // AGP 9 compiles Kotlin itself. Applying org.jetbrains.kotlin.android now
    // fails the build. The Compose compiler plugin is still separate, and AGP
    // refuses to turn Compose on without it.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The public notes on the ViWoods fast pen say the vendor library only loads
// for an app that targets SDK 30. Nobody has proved that on a stock tablet
// from this app yet, so the number can be changed from the command line:
//   ./gradlew assembleViwoodsDebug -PviwoodsTargetSdk=37
// See docs/decisions/0007-viwoods-ink-and-refresh.md.
val viwoodsTargetSdk: Int =
    (providers.gradleProperty("viwoodsTargetSdk").orNull ?: "30").toInt()

android {
    namespace = "io.github.foxesrcool1.einklauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.foxesrcool1.einklauncher"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        // A fixed debug key, checked into the repository on purpose.
        // Every debug build then has the same signature, so a new build
        // installs over the last one on the tablet. It signs debug builds only.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    flavorDimensions += "device"
    productFlavors {
        // Tablets with the ViWoods e-ink platform.
        create("viwoods") {
            dimension = "device"
            targetSdk = viwoodsTargetSdk
            versionNameSuffix = "-viwoods"
        }
        // Any other Android device. Jetpack Ink rendering only, no hidden APIs.
        create("generic") {
            dimension = "device"
            versionNameSuffix = "-generic"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Keep the Kotlin sources under src/<set>/kotlin rather than src/<set>/java.
    sourceSets.configureEach {
        kotlin.directories.add("src/$name/kotlin")
    }

    // The EPUB reader shows Literata inside a web view, and a web view can
    // only load a font from the assets. The file already lives in res/font
    // for the rest of the app, so that folder doubles as an assets folder
    // rather than keeping a second copy of a 900 KB file in the repository.
    sourceSets.getByName("main") {
        assets.directories.add("src/main/res/font")
    }

    compileOptions {
        // Readium asks for this. It lets the library use newer java.* classes
        // than the oldest Android this app runs on has.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        // A text report can be read straight out of the CI log.
        textReport = true
        disable += "GradleDependency"
        // The viwoods flavour targets SDK 30 on purpose, see the top of this
        // file. It never goes to the Play Store, which is what these two
        // checks are about. The generic flavour targets the newest SDK.
        disable += "ExpiredTargetSdkVersion"
        disable += "OldTargetApi"
    }
}

// Roborazzi writes a PNG only when this property is set. The Roborazzi Gradle
// plugin normally sets it, but that plugin is not needed for a record only
// setup, and one less plugin is one less thing to break on an AGP upgrade.
// The unit test JVM runs with the module directory as its working directory,
// so the paths in the tests land in app/build/outputs/roborazzi/.
tasks.withType<Test>().configureEach {
    // Robolectric reaches into java.io and java.lang internals to build its
    // Android sandbox. A modern JDK seals those packages, and the failure
    // reads "Failed to interact with raw FileDescriptor internals", which
    // says nothing about the cause. These flags open exactly what it needs.
    jvmArgs(
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    )

    systemProperty("roborazzi.test.record", "true")
    systemProperty("robolectric.graphicsMode", "NATIVE")

    // The screenshot tests are told exactly where to write, as an absolute
    // path. A relative path would depend on the working directory of the test
    // JVM, and then the pictures land somewhere CI does not look.
    val screenshotDir = layout.buildDirectory.dir("outputs/roborazzi").get().asFile
    systemProperty("roborazzi.output.dir", screenshotDir.absolutePath)
    systemProperty("eink.screenshot.dir", screenshotDir.absolutePath)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // The EPUB reader, plan section 4. BSD-3.
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.readium.navigator)
    implementation(libs.androidx.fragment.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    testImplementation(composeBom)

    // Foundation only. Material is left out on purpose: it brings ripples,
    // elevation and animated indication, and none of that belongs on e-ink.
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)

    // The Jetpack Ink baseline canvas on the device test screen. Debug only.
    debugImplementation(libs.androidx.ink.authoring)
    debugImplementation(libs.androidx.ink.brush)
    debugImplementation(libs.androidx.ink.strokes)
    debugImplementation(libs.androidx.ink.rendering)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
