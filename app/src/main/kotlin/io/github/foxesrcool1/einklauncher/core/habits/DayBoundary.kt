package io.github.foxesrcool1.einklauncher.core.habits

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Where one day ends and the next begins.
 *
 * Midnight is the wrong line for a habit tracker. Someone who reads until half
 * past one has not started tomorrow, and a tracker that says they missed
 * yesterday is wrong about the only thing it does. The plan makes the hour a
 * setting and suggests 04:00.
 *
 * This holds no Android class and takes the moment and the zone as arguments,
 * so a test can ask what happens in Auckland on the day the clocks change.
 */
data class DayBoundary(val hour: Int = DEFAULT_HOUR) {

    init {
        require(hour in 0..23) { "The day boundary must be an hour of the day, not $hour" }
    }

    /** Which day a moment belongs to. */
    fun dateOf(instant: Instant, zone: ZoneId): LocalDate =
        dateOf(LocalDateTime.ofInstant(instant, zone))

    fun dateOf(localTime: LocalDateTime): LocalDate =
        if (localTime.hour < hour) {
            localTime.toLocalDate().minusDays(1)
        } else {
            localTime.toLocalDate()
        }

    companion object {
        const val DEFAULT_HOUR = 4
    }
}
