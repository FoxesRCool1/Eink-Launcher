package io.github.foxesrcool1.margin.ui.home

import io.github.foxesrcool1.margin.ui.apps.LauncherEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherEntryTest {

    @Test
    fun `the pin key is the package and the class`() {
        val entry = LauncherEntry("com.example.reader", "com.example.reader.Main", "Reader")
        assertEquals("com.example.reader/com.example.reader.Main", entry.key)
        assertEquals(entry.key, LauncherEntry.keyOf(entry.packageName, entry.className))
    }

    @Test
    fun `two activities in one package get different keys`() {
        val one = LauncherEntry("com.example.suite", "com.example.suite.Notes", "Notes")
        val two = LauncherEntry("com.example.suite", "com.example.suite.Draw", "Draw")
        assertEquals(false, one.key == two.key)
    }
}
