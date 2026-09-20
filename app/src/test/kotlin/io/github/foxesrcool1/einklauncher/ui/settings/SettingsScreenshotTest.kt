package io.github.foxesrcool1.einklauncher.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.support.captureTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings does not scroll, so each of its pages has to fit the panel. These pictures show whether it does. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw480dp-w480dp-h640dp-port-xxhdpi")
class SettingsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shoot(page: Int, name: String) {
        compose.setContent {
            EinkTheme(botanicalArt = false) {
                SettingsScreen(onBack = {}, onOpenLog = {}, onOpenDemo = {}, initialPage = page)
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureTo(name)
    }

    @Test
    fun homePage() = shoot(0, "settings_home")

    @Test
    fun lookPage() = shoot(1, "settings_look")

    @Test
    fun penPage() = shoot(2, "settings_pen")

    @Test
    fun storagePage() = shoot(3, "settings_storage")

    @Test
    fun helpPage() = shoot(4, "settings_help")

    @Test
    fun aboutPage() = shoot(5, "settings_about")
}
