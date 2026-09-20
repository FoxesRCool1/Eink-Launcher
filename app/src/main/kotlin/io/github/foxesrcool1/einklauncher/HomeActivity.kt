package io.github.foxesrcool1.einklauncher

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.habits.DayBoundary
import io.github.foxesrcool1.einklauncher.core.habits.HabitsRepository
import io.github.foxesrcool1.einklauncher.core.notes.NotesRepository
import io.github.foxesrcool1.einklauncher.core.routine.RoutineRepository
import io.github.foxesrcool1.einklauncher.core.routine.RoutineStatus
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.window.ScreenWindow
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.ui.apps.AppsScreen
import io.github.foxesrcool1.einklauncher.ui.home.LauncherRoute
import io.github.foxesrcool1.einklauncher.ui.home.HomeScreen
import io.github.foxesrcool1.einklauncher.ui.journal.JournalScreen
import io.github.foxesrcool1.einklauncher.ui.reading.ReadingScreen
import io.github.foxesrcool1.einklauncher.ui.writing.NoteEditorActivity
import io.github.foxesrcool1.einklauncher.ui.writing.WritingScreen
import io.github.foxesrcool1.einklauncher.ui.log.LogViewerScreen
import io.github.foxesrcool1.einklauncher.ui.settings.SettingsScreen
import io.github.foxesrcool1.einklauncher.ui.update.UpdateScreen
import kotlinx.coroutines.launch

private const val TAG = "HomeActivity"

/**
 * The home screen.
 *
 * `singleTask` in the manifest means the Home key brings this same task
 * forward instead of starting a second copy. [onNewIntent] then fires, and
 * that is the moment to go back to Home, whatever screen was open.
 *
 * The reader and the editors will run in their own activities from Step 6 on,
 * so the Home key and Recents keep working the way the user expects.
 */
class HomeActivity : ComponentActivity() {

    /**
     * Held on the activity, not inside the composition, so [onNewIntent] can
     * reach it.
     */
    private val route: MutableState<LauncherRoute> = mutableStateOf(LauncherRoute.Home)

    /** Ctrl+N on a Bluetooth keyboard bumps this, and the Writing tab notices. */
    private val newNoteRequests = mutableIntStateOf(0)

    private var startLogged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.i(TAG, "onCreate")
        ScreenWindow.attach(this)
        logStartTime()

        setContent {
            val context = LocalContext.current
            val settings = remember(context) { SettingsStore(context) }
            val artOn by settings.botanicalArt.collectAsStateWithLifecycle(initialValue = true)

            EinkTheme(botanicalArt = artOn) {
                LauncherHost(
                    route = route.value,
                    onRoute = { route.value = it },
                    onOpenDemo = { openDemo() },
                    onOpenNote = { path -> openNote(path) },
                    onQuickNote = { quickNote() },
                    newNoteRequests = newNoteRequests.intValue,
                )
            }
        }
    }

    /**
     * How long the home screen took, from the start of the process to the
     * first frame. There is no profiler on this tablet, so the log is where
     * a slow start shows up. Step 10 asks for this number.
     */
    private fun logStartTime() {
        if (startLogged) return
        startLogged = true
        window.decorView.post {
            val sinceProcessStart = android.os.SystemClock.elapsedRealtime() -
                android.os.Process.getStartElapsedRealtime()
            AppLog.i(TAG, "Home screen up $sinceProcessStart ms after the process started")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The Home key sends us this. Always land on Home.
        AppLog.i(TAG, "onNewIntent: back to Home")
        route.value = LauncherRoute.Home
    }

    /**
     * Ctrl+N makes a note from anywhere, which is what a writer with a
     * keyboard expects. It lands in the Writing tab, so the new note goes into
     * the folder the user is looking at.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_N && event?.isCtrlPressed == true) {
            route.value = LauncherRoute.Writing
            newNoteRequests.intValue++
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun openNote(notePath: String) {
        runCatching {
            startActivity(NoteEditorActivity.intent(this, notePath))
        }.onFailure { AppLog.e(TAG, "Could not open the note $notePath", it) }
    }

    /**
     * One tap from Home makes a note and opens it. The name is the moment it
     * was made, because a note the user has not titled yet still needs to be
     * findable tomorrow.
     */
    private fun quickNote() {
        val notes = NotesRepository(DataRoot.repository(this))
        kotlin.concurrent.thread(name = "eink-quick-note", isDaemon = true) {
            val folder = notes.quickNotePath()
            val stamp = java.time.LocalDateTime.now()
                .withNano(0)
                .toString()
                .replace(':', '-')
            val path = runCatching { notes.createNote(folder, stamp) }.getOrNull()
            runOnUiThread {
                if (path != null) {
                    openNote(path)
                } else {
                    AppLog.e(TAG, "Could not make a quick note")
                }
            }
        }
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
    onOpenNote: (String) -> Unit,
    onQuickNote: () -> Unit,
    newNoteRequests: Int,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var next by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<RoutineStatus?>(null)
    }

    // The next routine item is read again every time Home comes back, so
    // ticking something off in the Journal shows up here at once. It is also
    // read again when the tablet wakes up on Home: a new day has a new list,
    // and the home screen is where a tablet spends the night.
    var wakeUps by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        wakeUps++
        onPauseOrDispose { }
    }
    LaunchedEffect(route, wakeUps) {
        if (route != LauncherRoute.Home) return@LaunchedEffect
        next = kotlinx.coroutines.withContext(io.github.foxesrcool1.einklauncher.core.threads.AppDispatchers.io) {
            runCatching {
                val data = DataRoot.repository(context)
                val hour = HabitsRepository(data).load().dayBoundaryHour
                val today = DayBoundary(hour).dateOf(
                    java.time.Instant.now(),
                    java.time.ZoneId.systemDefault(),
                )
                RoutineRepository(data).next(today)
            }.getOrNull()
        }
    }

    val homeLabels by remember(context) { SettingsStore(context).homeLabels }
        .collectAsStateWithLifecycle(initialValue = false)

    // E-ink rule 8: a full refresh after a big screen change, when the user
    // has turned it on. A change of tab is the big change of this activity.
    val fullRefreshOn by remember(context) { SettingsStore(context).fullRefreshOnBigChange }
        .collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(route, fullRefreshOn) {
        if (fullRefreshOn) {
            io.github.foxesrcool1.einklauncher.core.eink.EinkDevices.get(context).fullRefresh()
        }
    }

    // Back goes to Home. On Home it does nothing at all, because a home
    // screen has nowhere behind it.
    BackHandler(enabled = true) {
        if (route != LauncherRoute.Home) onRoute(LauncherRoute.Home)
    }

    when (route) {
        LauncherRoute.Home -> HomeScreen(
            showLabels = homeLabels,
            onOpenTab = onRoute,
            onOpenSettings = { onRoute(LauncherRoute.Settings) },
            onQuickNote = onQuickNote,
            nextRoutineLabel = next?.item?.label,
            onStartNext = {
                val item = next?.item ?: return@HomeScreen
                val tab = when (item.target) {
                    "read" -> LauncherRoute.Reading
                    "write" -> LauncherRoute.Writing
                    "journal" -> LauncherRoute.Journal
                    "apps" -> LauncherRoute.Apps
                    else -> null
                }
                val component = item.appComponent()
                when {
                    tab != null -> onRoute(tab)
                    component != null -> startApp(context, component.first, component.second)
                    // An item with nothing to open is just something to tick
                    // off, so Start marks it done and moves to the next one.
                    else -> scope.launch {
                        kotlinx.coroutines.withContext(io.github.foxesrcool1.einklauncher.core.threads.AppDispatchers.io) {
                            val data = DataRoot.repository(context)
                            val hour = HabitsRepository(data).load().dayBoundaryHour
                            val today = DayBoundary(hour).dateOf(
                                java.time.Instant.now(),
                                java.time.ZoneId.systemDefault(),
                            )
                            val routine = RoutineRepository(data)
                            routine.toggle(item.id, today)
                            next = routine.next(today)
                        }
                    }
                }
            },
        )

        LauncherRoute.Reading -> ReadingScreen(
            onBack = { onRoute(LauncherRoute.Home) },
        )

        LauncherRoute.Writing -> WritingScreen(
            onBack = { onRoute(LauncherRoute.Home) },
            onOpenNote = onOpenNote,
            newNoteRequests = newNoteRequests,
            onOpenInkNote = { path, title ->
                runCatching {
                    context.startActivity(
                        io.github.foxesrcool1.einklauncher.ui.ink.InkNoteActivity.intent(context, path, title),
                    )
                }.onFailure { AppLog.e(TAG, "Could not open the ink note $path", it) }
            },
        )

        LauncherRoute.Journal -> JournalScreen(
            onBack = { onRoute(LauncherRoute.Home) },
        )

        LauncherRoute.Apps -> AppsScreen(
            onBack = { onRoute(LauncherRoute.Home) },
        )

        LauncherRoute.Settings -> SettingsScreen(
            onBack = { onRoute(LauncherRoute.Home) },
            onOpenLog = { onRoute(LauncherRoute.Log) },
            onOpenDemo = onOpenDemo,
            onOpenUpdates = { onRoute(LauncherRoute.Update) },
        )

        LauncherRoute.Log -> LogViewerScreen(
            onBack = { onRoute(LauncherRoute.Settings) },
        )

        LauncherRoute.Update -> UpdateScreen(
            onBack = { onRoute(LauncherRoute.Settings) },
        )
    }
}

/** Starts another app from a routine item. */
private fun startApp(context: android.content.Context, packageName: String, className: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(packageName, className)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure { AppLog.e(TAG, "Could not start $packageName from the routine", it) }
}
