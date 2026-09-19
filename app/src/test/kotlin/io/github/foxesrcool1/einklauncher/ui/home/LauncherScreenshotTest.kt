package io.github.foxesrcool1.einklauncher.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Screenshots of the launcher screens at the panel size.
 *
 * The time, the battery and the Wi-Fi state are all passed in, so the picture
 * is the same on every run and a change in the picture means a change in the
 * code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw480dp-w480dp-h640dp-port-xxhdpi")
class LauncherScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val fixedTime = LocalDateTime.of(2026, 9, 19, 8, 4)

    private fun capture(name: String) {
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun todayScreen() {
        compose.setContent {
            EinkTheme {
                TodayScreen(
                    onOpenTab = {},
                    onOpenSettings = {},
                    now = fixedTime,
                    battery = 82,
                    wifi = true,
                )
            }
        }
        capture("today")
    }

    @Test
    fun todayScreenWithoutCornerArt() {
        compose.setContent {
            EinkTheme(botanicalArt = false) {
                TodayScreen(
                    onOpenTab = {},
                    onOpenSettings = {},
                    now = fixedTime,
                    battery = 9,
                    wifi = false,
                )
            }
        }
        capture("today_no_art")
    }

    @Test
    fun placeholderTab() {
        compose.setContent {
            EinkTheme {
                PlaceholderScreen(
                    route = LauncherRoute.Writing,
                    plannedStep = "Step 6",
                    summary = "A file browser, a plain Markdown editor and " +
                        "handwritten notebooks live here.",
                    onBack = {},
                )
            }
        }
        capture("tab_placeholder")
    }
}
