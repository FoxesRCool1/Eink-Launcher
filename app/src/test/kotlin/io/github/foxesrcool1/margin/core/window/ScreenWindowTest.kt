package io.github.foxesrcool1.margin.core.window

import android.content.pm.ActivityInfo
import io.github.foxesrcool1.margin.core.settings.WindowSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenWindowTest {

    @Test
    fun `the screen is upright until the user turns it`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ScreenWindow.orientationOf(WindowSettings()))
    }

    @Test
    fun `landscape turns one way, and the other way when it is flipped`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ScreenWindow.orientationOf(WindowSettings(landscape = true)),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            ScreenWindow.orientationOf(WindowSettings(landscape = true, landscapeFlipped = true)),
        )
    }

    @Test
    fun `the flip alone does nothing to an upright screen`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ScreenWindow.orientationOf(WindowSettings(landscape = false, landscapeFlipped = true)),
        )
    }

    @Test
    fun `the status bar starts hidden`() {
        assertEquals(true, WindowSettings().statusBarHidden)
    }
}
