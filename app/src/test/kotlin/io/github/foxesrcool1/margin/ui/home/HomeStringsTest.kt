package io.github.foxesrcool1.margin.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class HomeStringsTest {

    private val morning = LocalDateTime.of(2026, 9, 19, 8, 4)
    private val evening = LocalDateTime.of(2026, 9, 19, 21, 30)

    @Test
    fun `the date reads as a day and a month`() {
        assertEquals("Saturday 19 September", HomeStrings.date(morning))
    }

    @Test
    fun `24 hour time keeps the leading zero`() {
        assertEquals("08:04", HomeStrings.time(morning, use24Hour = true))
        assertEquals("21:30", HomeStrings.time(evening, use24Hour = true))
    }

    @Test
    fun `12 hour time drops the leading zero and adds AM or PM`() {
        assertEquals("8:04 AM", HomeStrings.time(morning, use24Hour = false))
        assertEquals("9:30 PM", HomeStrings.time(evening, use24Hour = false))
    }

    @Test
    fun `the status line shows what is known`() {
        assertEquals("Wi-Fi on  .  Battery 82%", HomeStrings.status(82, onWifi = true))
        assertEquals("Wi-Fi off  .  Battery 5%", HomeStrings.status(5, onWifi = false))
    }

    @Test
    fun `a battery that does not report is left out, not guessed`() {
        assertEquals("Wi-Fi on", HomeStrings.status(null, onWifi = true))
    }
}
