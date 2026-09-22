package io.github.foxesrcool1.margin.core.habits

import java.time.LocalDate

/**
 * Streak arithmetic.
 *
 * Every function takes the day it should treat as today, so nothing here reads
 * the clock and every rule can be tested.
 */
object HabitStreaks {

    /**
     * How many days in a row, counting back from today.
     *
     * A day that is not finished yet does not break a streak. If today is not
     * marked but yesterday is, the streak still stands and the user still has
     * the rest of today to keep it. A tracker that reset the count at one
     * minute past the boundary would be punishing someone for not having done
     * the thing yet.
     */
    fun currentStreak(marks: Set<LocalDate>, today: LocalDate): Int {
        val start = when {
            marks.contains(today) -> today
            marks.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var count = 0
        var day = start
        while (marks.contains(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** True when a streak is running but today has not been marked yet. */
    fun needsDoingToday(marks: Set<LocalDate>, today: LocalDate): Boolean =
        !marks.contains(today) && currentStreak(marks, today) > 0

    /** The longest run of days in a row, ever. */
    fun longestStreak(marks: Set<LocalDate>): Int {
        if (marks.isEmpty()) return 0

        val ordered = marks.sorted()
        var longest = 1
        var run = 1

        for (index in 1 until ordered.size) {
            run = if (ordered[index] == ordered[index - 1].plusDays(1)) run + 1 else 1
            if (run > longest) longest = run
        }
        return longest
    }

    /**
     * One true or false per day for the last [count] days, oldest first, so a
     * row of dots reads left to right and ends on today.
     */
    fun lastDays(marks: Set<LocalDate>, today: LocalDate, count: Int = DOT_COUNT): List<Boolean> {
        require(count > 0) { "A row of dots needs at least one day" }
        return (count - 1 downTo 0).map { back -> marks.contains(today.minusDays(back.toLong())) }
    }

    /** How many of the last [count] days were marked. */
    fun doneInLastDays(marks: Set<LocalDate>, today: LocalDate, count: Int = DOT_COUNT): Int =
        lastDays(marks, today, count).count { it }

    /** Section 6: a row of dots shows the last 14 days. */
    const val DOT_COUNT = 14
}
