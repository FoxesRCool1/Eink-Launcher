package io.github.foxesrcool1.margin.ui.home

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

    @Test
    fun `back from the log and the updates goes to settings, from anything else to home`() {
        assertEquals(LauncherRoute.Settings, LauncherRoute.Log.parent)
        assertEquals(LauncherRoute.Settings, LauncherRoute.Update.parent)
        LauncherRoute.tabs.forEach { assertEquals(LauncherRoute.Home, it.parent) }
        assertEquals(LauncherRoute.Home, LauncherRoute.Settings.parent)
    }
}
