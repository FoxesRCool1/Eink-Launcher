package io.github.foxesrcool1.margin.core.habits

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayBoundaryTest {

    private val boundary = DayBoundary(hour = 4)

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun `after the boundary the day is today`() {
        assertEquals(date("2026-09-19"), boundary.dateOf(LocalDateTime.parse("2026-09-19T04:00")))
        assertEquals(date("2026-09-19"), boundary.dateOf(LocalDateTime.parse("2026-09-19T12:00")))
        assertEquals(date("2026-09-19"), boundary.dateOf(LocalDateTime.parse("2026-09-19T23:59")))
    }

    @Test
    fun `before the boundary the day is still yesterday`() {
        assertEquals(date("2026-09-18"), boundary.dateOf(LocalDateTime.parse("2026-09-19T00:00")))
        assertEquals(date("2026-09-18"), boundary.dateOf(LocalDateTime.parse("2026-09-19T01:30")))
        assertEquals(date("2026-09-18"), boundary.dateOf(LocalDateTime.parse("2026-09-19T03:59")))
    }

    @Test
    fun `a boundary of midnight behaves like a plain calendar`() {
        val midnight = DayBoundary(hour = 0)
        assertEquals(date("2026-09-19"), midnight.dateOf(LocalDateTime.parse("2026-09-19T00:00")))
        assertEquals(date("2026-09-19"), midnight.dateOf(LocalDateTime.parse("2026-09-19T23:59")))
    }

    @Test
    fun `the day is worked out in the zone the user is in`() {
        // The same moment is two different days in these two places.
        val moment = LocalDateTime.parse("2026-09-19T20:00").toInstant(java.time.ZoneOffset.UTC)
        assertEquals(date("2026-09-19"), boundary.dateOf(moment, ZoneId.of("UTC")))
        assertEquals(date("2026-09-20"), boundary.dateOf(moment, ZoneId.of("Pacific/Auckland")))
    }

    @Test
    fun `flying to another time zone does not lose a day`() {
        // 23:00 in Auckland is still the same calendar day there, whatever the
        // traveller's phone said an hour earlier somewhere else.
        val auckland = ZoneId.of("Pacific/Auckland")
        val moment = LocalDateTime.parse("2026-09-20T23:00").atZone(auckland).toInstant()
        assertEquals(date("2026-09-20"), boundary.dateOf(moment, auckland))
    }

    @Test
    fun `the day the clocks go forward still has a boundary`() {
        // New Zealand jumps from 02:00 to 03:00 on this date, so 02:30 does
        // not exist. The boundary is 04:00, so the answer must still be the
        // 27th and not the 26th.
        val auckland = ZoneId.of("Pacific/Auckland")
        val afterJump = LocalDateTime.parse("2026-09-27T05:00").atZone(auckland).toInstant()
        assertEquals(date("2026-09-27"), boundary.dateOf(afterJump, auckland))

        val beforeJump = LocalDateTime.parse("2026-09-27T01:00").atZone(auckland).toInstant()
        assertEquals(date("2026-09-26"), boundary.dateOf(beforeJump, auckland))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an hour that is not an hour of the day is refused`() {
        DayBoundary(hour = 24)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a negative hour is refused`() {
        DayBoundary(hour = -1)
    }
}
