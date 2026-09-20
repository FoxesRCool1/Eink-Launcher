package io.github.foxesrcool1.einklauncher.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import io.github.foxesrcool1.einklauncher.BuildConfig
import io.github.foxesrcool1.einklauncher.core.eink.EinkDevices
import io.github.foxesrcool1.einklauncher.core.launcher.DefaultLauncher
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.core.settings.WindowSettings
import io.github.foxesrcool1.einklauncher.core.window.ScreenWindow
import io.github.foxesrcool1.einklauncher.core.window.findActivity
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialog
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import io.github.foxesrcool1.einklauncher.design.components.Plants
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.design.icons.LucideIcon
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.launch

private const val TAG = "SettingsScreen"

/**
 * The pages of Settings. The first one is the menu: the list of all the
 * others, each with one line that says what is in it.
 */
enum class SettingsPage(val title: String, val summary: String, val icon: LucideIcon) {
    Menu("Settings", "", Lucide.Settings),
    HomeApp("Home app", "Make this the home screen.", Lucide.House),
    Look("Look and screen", "Plants, names, status bar, landscape.", Lucide.Eye),
    Pen("Pen", "How fast the pen line shows.", Lucide.PenLine),
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember(context) { SettingsStore(context) }
    val device = remember(context) { EinkDevices.get(context) }

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

    var isDefault by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }
    var showManualSteps by remember { mutableStateOf(false) }
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefault = DefaultLauncher.isDefault(context)
        AppLog.i(TAG, "Home role request came back. Default now: $isDefault")
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
                    "Done. Eink Launcher is the home app."
                } else {
                    "Android asks which home app you want. Choose Eink Launcher."
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
                onClick = {
                    isDefault = DefaultLauncher.isDefault(context)
                    status = if (isDefault) "Eink Launcher is the home app" else "Eink Launcher is not the home app yet"
                },
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
                title = "Names under the icons",
                help = "Show the words Read, Write, Journal and Apps on Home.",
                icon = Lucide.Type,
                on = homeLabels,
                onChange = { scope.launch { settings.setHomeLabels(it) } },
            ),
            Setting.switch(
                title = "Android status bar",
                help = "The top bar with the clock. When it is off, swipe down from the top to see it.",
                icon = Lucide.BatteryMedium,
                on = !window.statusBarHidden,
                onChange = { show -> changeWindow { it.copy(statusBarHidden = !show) } },
            ),
            Setting.switch(
                title = "Screen on its side",
                help = "Landscape. The small grey icon at the top of each screen does the same.",
                icon = Lucide.RotateCwSquare,
                on = window.landscape,
                onChange = { on -> changeWindow { it.copy(landscape = on) } },
            ),
            Setting.switch(
                title = "Turn it the other way",
                help = "For landscape. Use it if the tablet keys end up under your hand.",
                icon = Lucide.RefreshCw,
                on = window.landscapeFlipped,
                enabled = window.landscape,
                onChange = { on -> changeWindow { it.copy(landscapeFlipped = on) } },
            ),
            Setting.switch(
                title = "Clean the screen on each change",
                help = if (device.hasVendorControl) {
                    "A full refresh when you open another screen. It removes grey marks."
                } else {
                    "This device has no screen refresh that the app can control."
                },
                icon = Lucide.Monitor,
                on = fullRefresh,
                enabled = device.hasVendorControl,
                onChange = { scope.launch { settings.setFullRefreshOnBigChange(it) } },
            ),
            Setting(
                title = "Clean the screen now",
                help = "One full refresh, now.",
                icon = Lucide.RefreshCw,
                enabled = device.hasVendorControl,
                trailing = null,
                onClick = { device.fullRefresh() },
            ),
        )

        SettingsPage.Pen -> if (!device.hasVendorControl) {
            listOf(
                Setting(
                    title = "Pen line",
                    help = "This device has no fast pen that the app knows. The app draws the line itself.",
                    icon = Lucide.PenLine,
                    enabled = false,
                    trailing = null,
                    onClick = {},
                ),
            )
        } else {
            listOf(
                Setting(
                    title = "Who draws the pen line",
                    help = "The tablet can draw faster than the app. Run the device test in Help first.",
                    icon = Lucide.PenLine,
                    value = fastPenLabel(fastPen),
                    onClick = {
                        choice = Choice(
                            title = "Who draws the pen line",
                            options = listOf(
                                SettingsStore.FAST_PEN_OFF,
                                SettingsStore.FAST_PEN_WRITING,
                                SettingsStore.FAST_PEN_AUTODRAW,
                            ).map { mode -> fastPenLabel(mode) to { scope.launch { settings.setFastPenMode(mode) } } },
                        )
                    },
                ),
                Setting(
                    title = "Wait before the final line",
                    help = "After the pen lifts, the app draws the line again. Longer is safer.",
                    icon = Lucide.History,
                    value = "$inkDelay ms",
                    enabled = fastPen != SettingsStore.FAST_PEN_OFF,
                    onClick = {
                        choice = Choice(
                            title = "Wait before the final line",
                            options = listOf(600L, 900L, 1200L, 1600L).map { millis ->
                                "$millis ms" to { scope.launch { settings.setInkRedrawDelayMillis(millis) } }
                            },
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
                            Intent(context, io.github.foxesrcool1.einklauncher.ui.devicetest.DeviceTestActivity::class.java),
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
        )

        SettingsPage.About -> emptyList()
    }

    ScreenScaffold(
        title = page.title,
        overline = if (page == SettingsPage.Menu) "Eink Launcher ${BuildConfig.VERSION_NAME}" else "Settings",
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
            onDismiss = { choice = null },
            options = asking.options.map { (label, pick) ->
                DialogOption(label = label) {
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

/** A setting with a short list of values to choose from. */
private class Choice(val title: String, val options: List<Pair<String, () -> Unit>>)

private fun fastPenLabel(mode: String): String = when (mode) {
    SettingsStore.FAST_PEN_WRITING -> "Tablet, way A"
    SettingsStore.FAST_PEN_AUTODRAW -> "Tablet, way B"
    else -> "This app"
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
            "The system is asking. Choose Eink Launcher."
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
            return "Settings is open. Pick Eink Launcher as the home app."
        }
    }

    AppLog.w(TAG, "No settings route worked. Showing the written steps.")
    showSteps()
    return "This tablet hides the settings app. Follow these steps."
}
