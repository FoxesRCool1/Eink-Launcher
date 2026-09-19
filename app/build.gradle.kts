plugins {
    // AGP 9 compiles Kotlin itself and supplies the Compose compiler that
    // matches its own Kotlin version. Applying org.jetbrains.kotlin.android
    // now fails the build, and applying the Compose compiler plugin would
    // override a set of coordinates that already line up.
    alias(libs.plugins.android.application)
}

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
        // Tablets with the ViWoods e-ink platform. Step 2 decides whether this
        // flavour has to drop to targetSdk 30 for the fast pen path.
        create("viwoods") {
            dimension = "device"
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
        kotlin.srcDir("src/$name/kotlin")
    }

    compileOptions {
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
        disable += "GradleDependency"
    }
}

// Roborazzi writes a PNG only when this property is set. The Roborazzi Gradle
// plugin normally sets it, but that plugin is not needed for a record only
// setup, and one less plugin is one less thing to break on an AGP upgrade.
// The unit test JVM runs with the module directory as its working directory,
// so the paths in the tests land in app/build/outputs/roborazzi/.
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
    systemProperty("robolectric.graphicsMode", "NATIVE")
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
