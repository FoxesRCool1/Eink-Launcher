package io.github.foxesrcool1.margin.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.margin.BuildConfig
import io.github.foxesrcool1.margin.core.eink.EinkDevice
import io.github.foxesrcool1.margin.core.eink.EinkDevices
import io.github.foxesrcool1.margin.core.eink.FastPenPath
import io.github.foxesrcool1.margin.core.eink.ScreenRefresh
import io.github.foxesrcool1.margin.core.launcher.DefaultLauncher
import io.github.foxesrcool1.margin.core.log.AppLog
import io.github.foxesrcool1.margin.core.settings.SettingsStore
import io.github.foxesrcool1.margin.core.settings.WindowSettings
import io.github.foxesrcool1.margin.core.threads.AppDispatchers
import io.github.foxesrcool1.margin.core.window.ScreenWindow
import io.github.foxesrcool1.margin.core.window.findActivity
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.CapsLabel
import io.github.foxesrcool1.margin.design.components.ConfirmDialog
import io.github.foxesrcool1.margin.design.components.DialogOption
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.OptionsDialog
import io.github.foxesrcool1.margin.design.components.Plants
import io.github.foxesrcool1.margin.design.icons.Lucide
import io.github.foxesrcool1.margin.design.icons.LucideIcon
import io.github.foxesrcool1.margin.ui.common.ScreenScaffold
import io.github.foxesrcool1.margin.ui.common.WebLinks
import io.github.foxesrcool1.margin.ui.devicetest.PenTestActivity
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SettingsScreen"

/**
 * The pages of Settings. The first one is the menu: the list of all the
 * others, each with one line that says what is in it.
 */
enum class SettingsPage(val title: String, val summary: String, val icon: LucideIcon) {
    Menu("Settings", "", Lucide.Settings),
    HomeApp("Home app", "Make this the home screen.", Lucide.House),
    Look("Look and screen", "Plants, status bar, landscape, refresh.", Lucide.Eye),
    Pen("Pen", "Normal or fast pen, and a page to try it.", Lucide.PenLine),
    Backup("Backup and files", "Save a copy, or bring one back.", Lucide.Archive),
    Updates("Updates", "Look for a newer version.", Lucide.Download),
    Help("Help", "The log, the tests, the credits.", Lucide.LifeBuoy),
    About("Credits", "Who made what, and the licences.", Lucide.Info),
}

/**
 * The settings screen.
 *
 * It is a menu of six groups, and each group is one page of rows. Every row
 * has a short name, one or two lines that say what it does, and at the end
 * either a switch, the value it has now, or an arrow. The whole row is the
 * target. The first version was six tabs of buttons with the state written
 * into the button text, and the owner could not find his way around it.
 *
 * Nothing scrolls: a group with more rows than fit gets a second page.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenDemo: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenUpdates: () -> Unit = {},
    /** Which page to open on, as an index into [SettingsPage]. The screenshot tests use it. */
    initialPage: Int = 0,
    /** The panel. The screenshot tests hand in one with vendor control, to draw the Pen page. */
    device: EinkDevice = EinkDevices.get(LocalContext.current),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember(context) { SettingsStore(context) }

    var page by rememberSaveable {
        mutableStateOf(SettingsPage.entries[initialPage.coerceIn(0, SettingsPage.entries.size - 1)])
    }
    var status by remember { mutableStateOf<String?>(null) }
    var choice by remember { mutableStateOf<Choice?>(null) }

    val artOn by settings.botanicalArt.collectAsStateWithLifecycle(initialValue = true)
    val homeLabels by settings.homeLabels.collectAsStateWithLifecycle(initialValue = false)
    val window by settings.window.collectAsStateWithLifecycle(initialValue = ScreenWindow.settings(context))
    val fullRefresh by settings.fullRefreshOnBigChange.collectAsStateWithLifecycle(initialValue = false)
    val fastPen by settings.fastPenMode.collectAsStateWithLifecycle(initialValue = SettingsStore.FAST_PEN_OFF)
    val inkDelay by settings.inkRedrawDelayMillis.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_INK_DELAY_MILLIS)

    // Which fast pen modes closed the app. Read from a small file each time
    // the Pen page opens, off the main thread.
    val penGuard = remember(context) { EinkDevices.guard(context) }
    var crashedPens by remember { mutableStateOf(emptySet<FastPenPath>()) }
    LaunchedEffect(page) {
        if (page != SettingsPage.Pen) return@LaunchedEffect
        crashedPens = withContext(AppDispatchers.io) {
            FastPenPath.entries.filterNot(penGuard::mayTry).toSet()
        }
    }

    // Asking the package manager is a binder call, so it runs off the main
    // thread: once when the screen opens, and again when the role request
    // comes back.
    var isDefault by remember { mutableStateOf(false) }
    var isDefaultChecks by remember { mutableStateOf(0) }
    LaunchedEffect(isDefaultChecks) {
        isDefault = withContext(AppDispatchers.io) { DefaultLauncher.isDefault(context) }
        if (isDefaultChecks > 0) {
            AppLog.i(TAG, "Home role checked again. Default now: $isDefault")
            status = if (isDefault) "Margin is the home app" else "Margin is not the home app yet"
        }
    }
    var showManualSteps by remember { mutableStateOf(false) }
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefaultChecks++
    }

    val storage = rememberStorageActions(onStatus = { status = it })

    fun open(next: SettingsPage) {
        status = null
        page = next
    }

    fun changeWindow(transform: (WindowSettings) -> WindowSettings) {
        context.findActivity()?.let { ScreenWindow.change(it, transform) }
    }

    // Back goes up one level first. Home's own handler takes it from the menu.
    val parent = if (page == SettingsPage.About) SettingsPage.Help else SettingsPage.Menu
    BackHandler(enabled = page != SettingsPage.Menu) { open(parent) }

    val rows: List<Setting> = when (page) {
        SettingsPage.Menu -> SettingsPage.entries
            .filter { it != SettingsPage.Menu && it != SettingsPage.About }
            .map { entry ->
                Setting(title = entry.title, help = entry.summary, icon = entry.icon, onClick = { open(entry) })
            }

        SettingsPage.HomeApp -> listOf(
            Setting(
                title = "Set as home app",
                help = if (isDefault) {
                    "Done. Margin is the home app."
                } else {
                    "Android asks which home app you want. Choose Margin."
                },
                icon = Lucide.House,
                enabled = !isDefault,
                trailing = if (isDefault) Lucide.Check else Lucide.ChevronRight,
                onClick = {
                    status = setAsHome(
                        context = context,
                        startRoleRequest = { intent -> roleLauncher.launch(intent) },
                        showSteps = { showManualSteps = true },
                    )
                },
            ),
            Setting(
                title = "Check again",
                help = "Press this after you changed the home app in the tablet settings.",
                icon = Lucide.RefreshCw,
                trailing = null,
                // The answer comes from the check above, off the main thread.
                onClick = { isDefaultChecks++ },
            ),
        )

        SettingsPage.Look -> listOf(
            Setting.switch(
                title = "Plants",
                help = "A small plant drawing at the top of each screen.",
                icon = Lucide.Sprout,
                on = artOn,
                onChange = { scope.launch { settings.setBotanicalArt(it) } },
            ),
            Setting.switch(
                title = "Icon Names",
                help = "Show the words Read, Write, Journal and Apps under the icons on Home.",
                icon = Lucide.Type,
                on = homeLabels,
                onChange = { scope.launch { settings.setHomeLabels(it) } },
            ),
            Setting.switch(
                title = "Status Bar",
                help = "The Android bar with the clock. When off, swipe down to see it.",
                icon = Lucide.BatteryMedium,
                on = !window.statusBarHidden,
                onChange = { show -> changeWindow { it.copy(statusBarHidden = !show) } },
            ),
            Setting.switch(
                title = "Landscape Mode",
                help = "Turn the screen sideways. The icon at the top does it too.",
                icon = Lucide.RotateCwSquare,
                on = window.landscape,
                onChange = { on -> changeWindow { it.copy(landscape = on) } },
            ),
            Setting.switch(
                title = "Flip Landscape",
                help = "Use it if the tablet keys end up under your hand.",
                icon = Lucide.RefreshCw,
                on = window.landscapeFlipped,
                enabled = window.landscape,
                onChange = { on -> changeWindow { it.copy(landscapeFlipped = on) } },
            ),
            Setting.switch(
                title = "Auto Refresh",
                help = "A full refresh each time you open another screen.",
                icon = Lucide.Monitor,
                on = fullRefresh,
                onChange = { scope.launch { settings.setFullRefreshOnBigChange(it) } },
            ),
            Setting(
                title = "Force Refresh",
                help = "The screen goes black for a moment and comes back clean.",
                icon = Lucide.RefreshCw,
                trailing = null,
                onClick = { context.findActivity()?.let(ScreenRefresh::run) },
            ),
        )

        SettingsPage.Pen -> if (!device.hasVendorControl) {
            listOf(
                Setting(
                    title = "Pen Mode",
                    help = "This tablet has no fast pen that the app knows. The app draws the line.",
                    icon = Lucide.PenLine,
                    value = penModeLabel(SettingsStore.FAST_PEN_OFF),
                    enabled = false,
                    trailing = null,
                    onClick = {},
                ),
            )
        } else {
            val closedApp = penModePath(fastPen)?.takeIf { it in crashedPens }
            listOf(
                Setting(
                    title = "Pen Mode",
                    help = if (closedApp != null) {
                        "${penModeLabel(fastPen)} closed the app once. The app draws the line now."
                    } else {
                        "Normal is safe. Fast is quicker, but it is a test."
                    },
                    icon = Lucide.PenLine,
                    value = penModeLabel(fastPen),
                    onClick = {
                        choice = Choice(
                            title = "Pen Mode",
                            message = PEN_MODE_STEPS,
                            options = PEN_MODES.map { mode ->
                                val path = penModePath(mode)
                                val label = penModeLabel(mode) + if (path in crashedPens) ", closed the app once" else ""
                                label to {
                                    scope.launch {
                                        // Both or neither: leaving Settings at once must not
                                        // clear the guard and lose the choice.
                                        withContext(NonCancellable) {
                                            settings.setFastPenMode(mode)
                                            // Picking a mode that crashed is the owner asking for one more try.
                                            if (path != null) withContext(AppDispatchers.io) { penGuard.reset(path) }
                                        }
                                        crashedPens = crashedPens - setOfNotNull(path)
                                    }
                                }
                            },
                            current = PEN_MODES.indexOf(fastPen),
                        )
                    },
                ),
                Setting(
                    title = "Try the Pen",
                    help = "Write a few words with the mode you picked. Nothing is saved.",
                    icon = Lucide.Pencil,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                PenTestActivity.intent(context, penTestMode(fastPen))
                                    .putExtra(PenTestActivity.EXTRA_DELAY, inkDelay),
                            )
                        }.onFailure { AppLog.e(TAG, "Could not open the pen page", it) }
                    },
                ),
                Setting(
                    title = "Redraw Delay",
                    help = "Fast modes only. Longer if a line blinks or shows twice.",
                    icon = Lucide.History,
                    value = "$inkDelay ms",
                    enabled = fastPen != SettingsStore.FAST_PEN_OFF,
                    onClick = {
                        choice = Choice(
                            title = "Redraw Delay",
                            message = REDRAW_DELAY_HELP,
                            options = REDRAW_DELAYS.map { millis ->
                                "$millis ms" to { scope.launch { settings.setInkRedrawDelayMillis(millis) } }
                            },
                            current = REDRAW_DELAYS.indexOf(inkDelay),
                        )
                    },
                ),
            )
        }

        SettingsPage.Backup -> listOf(
            Setting(
                title = "Back up",
                help = "Save every book, note and journal page into one zip file.",
                icon = Lucide.Upload,
                enabled = !storage.busy,
                onClick = storage.backUp,
            ),
            Setting(
                title = "Restore",
                help = "Bring back the files of a backup zip. Files with the same name are replaced.",
                icon = Lucide.Download,
                enabled = !storage.busy,
                onClick = storage.restore,
            ),
            Setting(
                title = "Read the folder again",
                help = "Use it when a book or note that you copied in by hand does not show.",
                icon = Lucide.RefreshCw,
                enabled = !storage.busy,
                trailing = null,
                onClick = storage.rebuildIndex,
            ),
            Setting(
                title = "Where the files are",
                // Without the start that every tablet shares, so the part that
                // matters fits in two lines.
                help = storage.folderPath.removePrefix("/storage/emulated/0/"),
                icon = Lucide.HardDrive,
                enabled = false,
                trailing = null,
                onClick = {},
            ),
        )

        SettingsPage.Updates -> listOf(
            Setting(
                title = "Check for updates",
                help = "You have ${BuildConfig.VERSION_NAME}. The app goes online only when you press this.",
                icon = Lucide.Download,
                onClick = onOpenUpdates,
            ),
        )

        SettingsPage.Help -> listOf(
            Setting(
                title = "Log",
                help = "What the app did and what went wrong. Send it with a problem report.",
                icon = Lucide.ScrollText,
                onClick = onOpenLog,
            ),
            Setting(
                title = "Device test",
                help = "Tests of the screen and the pen. The results go to the log.",
                icon = Lucide.Wrench,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(context, io.github.foxesrcool1.margin.ui.devicetest.DeviceTestActivity::class.java),
                        )
                    }.onFailure { AppLog.e(TAG, "Could not open the device test", it) }
                },
            ),
            Setting(
                title = "Design demo",
                help = "Every button, list and dialog of the app on one screen.",
                icon = Lucide.LayoutGrid,
                onClick = onOpenDemo,
            ),
            Setting(
                title = "Credits",
                help = SettingsPage.About.summary,
                icon = Lucide.Info,
                onClick = { open(SettingsPage.About) },
            ),
            Setting(
                title = "Support this app",
                help = "It is free and stays free. If it helps you, you can buy me a coffee.",
                icon = Lucide.Coffee,
                trailing = null,
                onClick = { WebLinks.open(context, WebLinks.KO_FI) },
            ),
            Setting(
                title = "Source code",
                help = "Open source, Apache-2.0, on GitHub. Bug reports and changes are welcome.",
                icon = Lucide.FileText,
                trailing = null,
                onClick = { WebLinks.open(context, WebLinks.SOURCE) },
            ),
        )

        SettingsPage.About -> emptyList()
    }

    ScreenScaffold(
        title = page.title,
        overline = if (page == SettingsPage.Menu) "Margin ${BuildConfig.VERSION_NAME}" else "Settings",
        plant = Plants.Settings,
        modifier = modifier,
        onBack = if (page == SettingsPage.Menu) onBack else ({ open(parent) }),
        backIcon = if (page == SettingsPage.Menu) Lucide.House else Lucide.ArrowLeft,
        backLabel = if (page == SettingsPage.Menu) "Home" else "Back",
    ) {
        if (status != null) {
            CapsLabel(
                text = status.orEmpty(),
                style = EinkType.capsSmall.copy(color = EinkColors.Faded),
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            page == SettingsPage.About -> AboutSection(modifier = Modifier.weight(1f))

            page == SettingsPage.HomeApp && showManualSteps -> {
                DefaultLauncher.manualSteps.forEachIndexed { index, step ->
                    EinkText(
                        text = "${index + 1}. $step",
                        style = EinkType.body,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                key(page) { SettingsList(rows = rows, modifier = Modifier.weight(1f)) }
            }

            else -> key(page) {
                SettingsList(
                    rows = rows,
                    modifier = Modifier.weight(1f),
                    helpLines = if (page == SettingsPage.Menu) 1 else 2,
                )
            }
        }
    }

    val asking = choice
    if (asking != null) {
        OptionsDialog(
            title = asking.title,
            message = asking.message,
            onDismiss = { choice = null },
            options = asking.options.mapIndexed { index, (label, pick) ->
                DialogOption(label = label, icon = if (index == asking.current) Lucide.Check else Lucide.Minus) {
                    pick()
                    choice = null
                }
            },
        )
    }

    if (storage.confirmRestore != null) {
        ConfirmDialog(
            title = "Restore from a backup?",
            message = "Every file in the backup replaces the one on this tablet with " +
                "the same name. Files that are not in the backup stay where they are.",
            confirmText = "Restore",
            cancelText = "Cancel",
            onConfirm = storage.confirmRestore,
            onDismiss = storage.cancelRestore,
        )
    }
}

/**
 * A setting with a short list of values to choose from. [current] is the
 * index of the value it has now, which gets a tick, or -1.
 */
private class Choice(
    val title: String,
    val options: List<Pair<String, () -> Unit>>,
    val message: String? = null,
    val current: Int = -1,
)

/**
 * The pen modes, in the order the dialog shows them. The stored names stay
 * the old ones, so a tablet keeps its choice through the update.
 */
internal val PEN_MODES = listOf(
    SettingsStore.FAST_PEN_OFF,
    SettingsStore.FAST_PEN_WRITING,
    SettingsStore.FAST_PEN_AUTODRAW,
)

/**
 * What the pen modes are and what to do, above the choice. Short: the dialog
 * does not scroll, and in landscape a long text pushed the last choice off.
 */
internal const val PEN_MODE_STEPS =
    "Normal: the app draws the line. Safe.\n" +
        "Fast: the tablet draws it. Quicker, but a test.\n" +
        "Try Fast 1, then Fast 2. Keep the best one."

internal val REDRAW_DELAYS = listOf(600L, 900L, 1200L, 1600L)

internal const val REDRAW_DELAY_HELP = "The wait after you lift the pen. Longer is safer."

internal fun penModeLabel(mode: String): String = when (mode) {
    SettingsStore.FAST_PEN_WRITING -> "Fast 1"
    SettingsStore.FAST_PEN_AUTODRAW -> "Fast 2"
    else -> "Normal"
}

private fun penModePath(mode: String): FastPenPath? = when (mode) {
    SettingsStore.FAST_PEN_WRITING -> FastPenPath.Writing
    SettingsStore.FAST_PEN_AUTODRAW -> FastPenPath.AutoDraw
    else -> null
}

private fun penTestMode(mode: String): String = when (mode) {
    SettingsStore.FAST_PEN_WRITING -> PenTestActivity.MODE_WRITING
    SettingsStore.FAST_PEN_AUTODRAW -> PenTestActivity.MODE_AUTODRAW
    else -> PenTestActivity.MODE_APP
}

/**
 * The three ways to become the home app, in order. Returns the line to show
 * the user.
 */
private fun setAsHome(
    context: android.content.Context,
    startRoleRequest: (Intent) -> Unit,
    showSteps: () -> Unit,
): String {
    DefaultLauncher.roleRequestIntent(context)?.let { intent ->
        AppLog.i(TAG, "Asking for the home role")
        return runCatching {
            startRoleRequest(intent)
            "The system is asking. Choose Margin."
        }.getOrElse {
            AppLog.w(TAG, "The home role request would not start", it)
            openSettingsOrSteps(context, showSteps)
        }
    }
    return openSettingsOrSteps(context, showSteps)
}

private fun openSettingsOrSteps(
    context: android.content.Context,
    showSteps: () -> Unit,
): String {
    val intent = DefaultLauncher.homeSettingsIntent(context)
        ?: DefaultLauncher.allSettingsIntent(context)

    if (intent != null) {
        val started = runCatching { context.startActivity(intent) }.isSuccess
        if (started) {
            AppLog.i(TAG, "Opened a settings screen to pick the home app")
            return "Settings is open. Pick Margin as the home app."
        }
    }

    AppLog.w(TAG, "No settings route worked. Showing the written steps.")
    showSteps()
    return "This tablet hides the settings app. Follow these steps."
}
