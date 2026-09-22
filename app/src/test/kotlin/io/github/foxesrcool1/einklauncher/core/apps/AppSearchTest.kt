package io.github.foxesrcool1.einklauncher.core.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    @Test
    fun `one or two letters find the start of the name or of a word in it`() {
        assertTrue(AppSearch.matches("Calculator", "ca"))
        assertTrue(AppSearch.matches("Google Calendar", "ca"))
        assertTrue(AppSearch.matches("Google Calendar", "C"))
        assertTrue(AppSearch.matches("E-Reader Pro", "re"))
        assertFalse(AppSearch.matches("Scan", "ca"))
        assertFalse(AppSearch.matches("Kindle", "in"))
    }

    @Test
    fun `three letters find the name anywhere`() {
        assertTrue(AppSearch.matches("Kindle", "ind"))
        assertTrue(AppSearch.matches("KOReader", "read"))
        assertFalse(AppSearch.matches("Kindle", "indy"))
    }

    @Test
    fun `capitals and spaces around the query do not matter, and nothing typed matches everything`() {
        assertTrue(AppSearch.matches("KOReader", "koread"))
        assertTrue(AppSearch.matches("Kindle", "  KIN "))
        assertTrue(AppSearch.matches("Anything", ""))
        assertTrue(AppSearch.matches("Anything", "   "))
    }
}
