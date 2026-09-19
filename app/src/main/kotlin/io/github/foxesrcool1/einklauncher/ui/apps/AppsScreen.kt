package io.github.foxesrcool1.einklauncher.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkRow
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.rememberPagedListState
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AppsScreen"
private const val PAGE_SIZE = 8

private enum class AppsPage { Pinned, All }

/**
 * The Apps tab.
 *
 * Two pages, because 640 dp of height cannot hold eight pinned apps, the ways
 * out and a full list at 56 dp per row. Page one holds the short lists the
 * user needs every day. Page two is the whole list, A to Z, paginated.
 *
 * App names are text. There are no colour icons. Plan section 5.
 */
@Composable
fun AppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { AppsRepository(context) }
    val settings = remember(context) { SettingsStore(context) }

    var page by remember { mutableStateOf(AppsPage.Pinned) }
    var apps by remember { mutableStateOf<List<LauncherEntry>>(emptyList()) }
    var escapes by remember { mutableStateOf<List<EscapeEntry>>(emptyList()) }
    var menuFor by remember { mutableStateOf<LauncherEntry?>(null) }

    val pinnedKeys by settings.pinnedApps.collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberPagedListState()

    LaunchedEffect(repository) {
        repository.packageChanges().collect {
            val loaded = withContext(Dispatchers.IO) { repository.loadAll() }
            val ways = withContext(Dispatchers.IO) { repository.escapeEntries(loaded) }
            apps = loaded
            escapes = ways
        }
    }

    val byKey = remember(apps) { apps.associateBy { it.key } }
    val pinned = remember(pinnedKeys, byKey) { pinnedKeys.mapNotNull { byKey[it] } }

    ScreenScaffold(
        title = "Apps",
        overline = "${apps.size} apps",
        corner = null,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(
                text = "Pinned",
                selected = page == AppsPage.Pinned,
                onClick = { page = AppsPage.Pinned },
            )
            InvertPressButton(
                text = "All apps",
                selected = page == AppsPage.All,
                onClick = { page = AppsPage.All },
            )
            InvertPressButton(text = "Today", onClick = onBack, bordered = false)
        }

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))

        when (page) {
            AppsPage.Pinned -> PinnedPage(
                pinned = pinned,
                escapes = escapes,
                onOpen = { repository.launch(it) },
                onLongPress = { menuFor = it },
                onOpenEscape = { repository.launchEscape(it) },
            )

            AppsPage.All -> PagedList(
                items = apps,
                pageSize = PAGE_SIZE,
                state = listState,
                emptyText = "No apps found",
                modifier = Modifier.weight(1f),
            ) { _, entry ->
                AppRow(
                    entry = entry,
                    pinnedMark = pinnedKeys.contains(entry.key),
                    onOpen = { repository.launch(entry) },
                    onLongPress = { menuFor = entry },
                )
            }
        }
    }

    val selected = menuFor
    if (selected != null) {
        val isPinned = pinnedKeys.contains(selected.key)
        val pinnedFull = pinnedKeys.size >= SettingsStore.MAX_PINNED
        OptionsDialog(
            title = selected.label,
            onDismiss = { menuFor = null },
            options = listOf(
                DialogOption(
                    label = when {
                        isPinned -> "Unpin"
                        pinnedFull -> "Pinned list is full"
                        else -> "Pin"
                    },
                    enabled = isPinned || !pinnedFull,
                    onSelect = {
                        scope.launch { settings.togglePinned(selected.key) }
                        AppLog.i(TAG, "Toggled pin for ${selected.key}")
                        menuFor = null
                    },
                ),
                DialogOption(
                    label = "App info",
                    onSelect = {
                        repository.openAppInfo(selected)
                        menuFor = null
                    },
                ),
                DialogOption(
                    label = "Uninstall",
                    onSelect = {
                        repository.requestUninstall(selected)
                        menuFor = null
                    },
                ),
            ),
        )
    }
}

@Composable
private fun PinnedPage(
    pinned: List<LauncherEntry>,
    escapes: List<EscapeEntry>,
    onOpen: (LauncherEntry) -> Unit,
    onLongPress: (LauncherEntry) -> Unit,
    onOpenEscape: (EscapeEntry) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CapsLabel(text = "Pinned", style = EinkType.capsSmall)
        HairlineDivider(color = EinkColors.Faded)

        if (pinned.isEmpty()) {
            EinkText(
                text = "Nothing pinned yet. Hold an app on the All apps page and choose Pin.",
                style = EinkType.body.copy(color = EinkColors.Faded),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            pinned.forEach { entry ->
                AppRow(
                    entry = entry,
                    pinnedMark = false,
                    onOpen = { onOpen(entry) },
                    onLongPress = { onLongPress(entry) },
                )
            }
        }

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))

        CapsLabel(text = "Always available", style = EinkType.capsSmall)
        HairlineDivider(color = EinkColors.Faded)

        if (escapes.isEmpty()) {
            EinkText(
                text = "No other home app or settings app was found on this tablet.",
                style = EinkType.body.copy(color = EinkColors.Faded),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            escapes.forEach { escape ->
                EinkRow(onClick = { onOpenEscape(escape) }) { pressed ->
                    val colour = if (pressed) EinkColors.Paper else EinkColors.Ink
                    EinkText(
                        text = escape.label,
                        style = EinkType.rowTitle.copy(color = colour),
                        maxLines = 1,
                    )
                    CapsLabel(
                        text = escape.hint,
                        style = EinkType.capsSmall.copy(
                            color = if (pressed) EinkColors.Paper else EinkColors.Faded,
                        ),
                    )
                }
                HairlineDivider(color = EinkColors.Faded)
            }
        }
    }
}

@Composable
private fun AppRow(
    entry: LauncherEntry,
    pinnedMark: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    EinkRow(onClick = onOpen, onLongClick = onLongPress) { pressed ->
        val colour = if (pressed) EinkColors.Paper else EinkColors.Ink
        EinkText(
            text = if (pinnedMark) "${entry.label}  *" else entry.label,
            style = EinkType.rowTitle.copy(color = colour),
            maxLines = 1,
        )
    }
    HairlineDivider(color = EinkColors.Faded)
}
