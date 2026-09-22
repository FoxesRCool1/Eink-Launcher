package io.github.foxesrcool1.margin.core.eink

import android.os.Build
import io.github.foxesrcool1.margin.BuildConfig

/**
 * Knows when the app runs on the Android emulator on the dev machine.
 *
 * An emulator has no pen. Its mouse arrives as a finger, and a finger never
 * draws in this app. So in a debug build on an emulator, and only there, the
 * ink canvas lets the mouse draw. A release build and a real device never
 * take this path: on the tablet a finger that draws is a resting hand that
 * ruins the page.
 */
object DevEnvironment {

    val isEmulator: Boolean by lazy {
        val hardware = Build.HARDWARE.orEmpty().lowercase()
        val product = Build.PRODUCT.orEmpty().lowercase()
        hardware.contains("ranchu") || hardware.contains("goldfish") ||
            product.startsWith("sdk_") || product.contains("emulator") ||
            Build.FINGERPRINT.orEmpty().contains("generic")
    }

    /** True when a finger, which is what the emulator makes of the mouse, may draw. */
    val fingerDraws: Boolean get() = BuildConfig.DEBUG && isEmulator
}
