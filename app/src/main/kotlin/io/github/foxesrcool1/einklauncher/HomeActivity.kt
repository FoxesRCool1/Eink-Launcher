package io.github.foxesrcool1.einklauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.ui.apps.AppsScreen
import io.github.foxesrcool1.einklauncher.ui.home.LauncherRoute
import io.github.foxesrcool1.einklauncher.ui.home.PlaceholderScreen
import io.github.foxesrcool1.einklauncher.ui.home.TodayScreen
import io.github.foxesrcool1.einklauncher.ui.journal.JournalScreen
import io.github.foxesrcool1.einklauncher.ui.log.LogViewerScreen
import io.github.foxesrcool1.einklauncher.ui.settings.SettingsScreen

private const val TAG = "HomeActivity"

/**
 * The home screen.
 *
 * `singleTask` in the manifest means the Home key brings this same task
 * forward instead of starting a second copy. [onNewIntent] then fires, and
 * that is the moment to go back to Today, whatever screen was open.
 *
 * The reader and the editors will run in their own activities from Step 6 on,
 * so the Home key and Recents keep working the way the user expects.
 */
class HomeActivity : ComponentActivity() {

    /**
     * Held on the activity, not inside the composition, so [onNewIntent] can
     * reach it.
     */
    private val route: MutableState<LauncherRoute> = mutableStateOf(LauncherRoute.Today)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.i(TAG, "onCreate")

        setContent {
            val context = LocalContext.current
            val settings = remember(context) { SettingsStore(context) }
            val artOn by settings.botanicalArt.collectAsStateWithLifecycle(initialValue = true)

            EinkTheme(botanicalArt = artOn) {
                LauncherHost(
                    route = route.value,
                    onRoute = { route.value = it },
                    onOpenDemo = { openDemo() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The Home key sends us this. Always land on Today.
        AppLog.i(TAG, "onNewIntent: back to Today")
        route.value = LauncherRoute.Today
    }

    private fun openDemo() {
        runCatching {
            startActivity(Intent(this, DemoActivity::class.java))
        }.onFailure { AppLog.e(TAG, "Could not open the demo screen", it) }
    }
}

@Composable
private fun LauncherHost(
    route: LauncherRoute,
    onRoute: (LauncherRoute) -> Unit,
    onOpenDemo: () -> Unit,
) {
    // Back goes to Today. On Today it does nothing at all, because a home
    // screen has nowhere behind it.
    BackHandler(enabled = true) {
        if (route != LauncherRoute.Today) onRoute(LauncherRoute.Today)
    }

    when (route) {
        LauncherRoute.Today -> TodayScreen(
            onOpenTab = onRoute,
            onOpenSettings = { onRoute(LauncherRoute.Settings) },
        )

        LauncherRoute.Reading -> PlaceholderScreen(
            route = route,
            plannedStep = "Step 7 and step 8",
            summary = "The library, the EPUB reader and the PDF reader live here. " +
                "Import, highlights, typed notes and handwritten notes come with them.",
            onBack = { onRoute(LauncherRoute.Today) },
        )

        LauncherRoute.Writing -> PlaceholderScreen(
            route = route,
            plannedStep = "Step 6",
            summary = "A file browser, a plain Markdown editor and handwritten " +
                "notebooks live here.",
            onBack = { onRoute(LauncherRoute.Today) },
        )

        LauncherRoute.Journal -> JournalScreen(
            onBack = { onRoute(LauncherRoute.Today) },
        )

        LauncherRoute.Apps -> AppsScreen(
            onBack = { onRoute(LauncherRoute.Today) },
        )

        LauncherRoute.Settings -> SettingsScreen(
            onBack = { onRoute(LauncherRoute.Today) },
            onOpenLog = { onRoute(LauncherRoute.Log) },
            onOpenDemo = onOpenDemo,
        )

        LauncherRoute.Log -> LogViewerScreen(
            onBack = { onRoute(LauncherRoute.Settings) },
        )
    }
}
