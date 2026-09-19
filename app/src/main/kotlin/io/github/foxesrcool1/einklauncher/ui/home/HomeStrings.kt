package io.github.foxesrcool1.einklauncher.ui.home

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The strings on Today.
 *
 * Kept free of Android so the unit tests can check them at a fixed time.
 */
object HomeStrings {

    private val DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
    private val TIME_24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val TIME_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun date(now: LocalDateTime): String = DATE.format(now)

    fun time(now: LocalDateTime, use24Hour: Boolean): String =
        if (use24Hour) TIME_24.format(now) else TIME_12.format(now)

    /**
     * The one line status under the clock. Parts that are not known are left
     * out rather than shown as a guess.
     */
    fun status(batteryPercent: Int?, onWifi: Boolean): String {
        val parts = mutableListOf<String>()
        parts += if (onWifi) "Wi-Fi on" else "Wi-Fi off"
        if (batteryPercent != null) parts += "Battery $batteryPercent%"
        return parts.joinToString("  .  ")
    }
}
