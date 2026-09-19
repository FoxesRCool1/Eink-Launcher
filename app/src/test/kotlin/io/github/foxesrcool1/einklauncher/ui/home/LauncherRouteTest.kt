package io.github.foxesrcool1.einklauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherRouteTest {

    @Test
    fun `today shows the four tabs in plan order`() {
        assertEquals(
            listOf("Read", "Write", "Journal", "Apps"),
            LauncherRoute.tabs.map { it.title },
        )
    }

    @Test
    fun `today, settings and log are not tabs`() {
        assertFalse(LauncherRoute.tabs.contains(LauncherRoute.Today))
        assertFalse(LauncherRoute.tabs.contains(LauncherRoute.Settings))
        assertFalse(LauncherRoute.tabs.contains(LauncherRoute.Log))
    }
}
