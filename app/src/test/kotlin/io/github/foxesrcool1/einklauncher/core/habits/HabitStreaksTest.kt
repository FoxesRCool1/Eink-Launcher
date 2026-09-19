package io.github.foxesrcool1.einklauncher.core.habits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStreaksTest {

    private val today = LocalDate.of(2026, 9, 19)

    private fun days(vararg offsets: Int): Set<LocalDate> =
        offsets.map { today.minusDays(it.toLong()) }.toSet()

    @Test
    fun `no marks means no streak`() {
        assertEquals(0, HabitStreaks.currentStreak(emptySet(), today))
        assertEquals(0, HabitStreaks.longestStreak(emptySet()))
    }

    @Test
    fun `today alone is a streak of one`() {
        assertEquals(1, HabitStreaks.currentStreak(days(0), today))
    }

    @Test
    fun `days in a row are counted`() {
        assertEquals(5, HabitStreaks.currentStreak(days(0, 1, 2, 3, 4), today))
    }

    @Test
    fun `a day that is not finished yet does not break the streak`() {
        // Yesterday and before are marked, today is not. The user still has
        // the rest of today, so the streak stands.
        assertEquals(3, HabitStreaks.currentStreak(days(1, 2, 3), today))
        assertTrue(HabitStreaks.needsDoingToday(days(1, 2, 3), today))
    }

    @Test
    fun `a missed day ends the streak`() {
        // Marked three days ago and before, nothing since. That is a miss.
        assertEquals(0, HabitStreaks.currentStreak(days(2, 3, 4), today))
        assertFalse(HabitStreaks.needsDoingToday(days(2, 3, 4), today))
    }

    @Test
    fun `a gap in the middle is not counted`() {
        assertEquals(2, HabitStreaks.currentStreak(days(0, 1, 3, 4, 5), today))
    }

    @Test
    fun `nothing needs doing today once today is marked`() {
        assertFalse(HabitStreaks.needsDoingToday(days(0, 1, 2), today))
    }

    @Test
    fun `the longest streak is found anywhere in the history`() {
        val marks = days(0, 1) + days(5, 6, 7, 8) + days(20)
        assertEquals(4, HabitStreaks.longestStreak(marks))
    }

    @Test
    fun `one mark is a longest streak of one`() {
        assertEquals(1, HabitStreaks.longestStreak(days(7)))
    }

    @Test
    fun `a streak that crosses a month and a year is still one streak`() {
        val marks = setOf(
            LocalDate.of(2025, 12, 30),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
        )
        assertEquals(4, HabitStreaks.longestStreak(marks))
    }

    @Test
    fun `a streak that crosses the end of February in a leap year is counted`() {
        val marks = setOf(
            LocalDate.of(2028, 2, 27),
            LocalDate.of(2028, 2, 28),
            LocalDate.of(2028, 2, 29),
            LocalDate.of(2028, 3, 1),
        )
        assertEquals(4, HabitStreaks.longestStreak(marks))
    }

    @Test
    fun `the row of dots ends on today and reads left to right`() {
        val dots = HabitStreaks.lastDays(days(0, 2), today, count = 4)
        // Four days: three days ago, two days ago, yesterday, today.
        assertEquals(listOf(false, true, false, true), dots)
    }

    @Test
    fun `the row of dots is fourteen days by default`() {
        assertEquals(14, HabitStreaks.lastDays(emptySet(), today).size)
    }

    @Test
    fun `how many of the last days were done is counted`() {
        assertEquals(2, HabitStreaks.doneInLastDays(days(0, 3), today, count = 7))
        assertEquals(1, HabitStreaks.doneInLastDays(days(0, 30), today, count = 7))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a row of no days is refused`() {
        HabitStreaks.lastDays(emptySet(), today, count = 0)
    }

    @Test
    fun `a mark in the future does not join today's streak`() {
        val marks = days(0, 1) + setOf(today.plusDays(1))
        assertEquals(2, HabitStreaks.currentStreak(marks, today))
    }
}
