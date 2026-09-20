package io.github.foxesrcool1.einklauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherRouteTest {

    @Test
    fun `home shows the four tabs in plan order`() {
        assertEquals(
            listOf("Read", "Write", "Journal", "Apps"),
            LauncherRoute.tabs.map { it.title },
        )
    }

    @Test
    fun `home, settings and log are not tabs`() {
        assertFalse(LauncherRoute.tabs.contains(LauncherRoute.Home))
        assertFalse(LauncherRoute.tabs.contains(LauncherRoute.Settings))
        assertFalse(LauncherRoute.tabs.contains(LauncherRoute.Log))
    }
}
