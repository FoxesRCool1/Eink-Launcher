package io.github.foxesrcool1.einklauncher.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import io.github.foxesrcool1.einklauncher.design.components.EinkTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.foxesrcool1.einklauncher.core.ink.PageTemplate
import io.github.foxesrcool1.einklauncher.core.storage.StorageLayout
import io.github.foxesrcool1.einklauncher.ui.ink.InkNoteActivity
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.habits.DayBoundary
import io.github.foxesrcool1.einklauncher.core.habits.HabitSummary
import io.github.foxesrcool1.einklauncher.core.habits.HabitsRepository
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.threads.AppDispatchers
import io.github.foxesrcool1.einklauncher.core.routine.RoutineRepository
import io.github.foxesrcool1.einklauncher.core.routine.RoutineStatus
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.IconPressButton
import io.github.foxesrcool1.einklauncher.design.components.Plants
import io.github.foxesrcool1.einklauncher.design.components.einkFieldBorder
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.ui.common.LocalWideScreen
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.TextPromptDialog
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "JournalScreen"
private const val HABITS_PER_PAGE = 3

private enum class JournalMode { Day, Month, Routine }

/**
 * The Journal tab: one entry per day, a month view, and the habits.
 *
 * A day can hold a typed entry, a handwritten one, or both. The handwritten
 * one opens in the ink screen, as a file of its own next to the typed one.
 * Nothing on this screen scrolls: the day fits, and the month is one page by
 * definition.
 */
@Composable
fun JournalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    fixedToday: LocalDate? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data = remember(context) { DataRoot.repository(context) }
    val habits = remember(data) { HabitsRepository(data) }
    val routine = remember(data) { RoutineRepository(data) }

    var boundaryHour by remember { mutableIntStateOf(DayBoundary.DEFAULT_HOUR) }
    var refresh by remember { mutableIntStateOf(0) }

    // Worked out again each time the screen comes back and after each change.
    // A tablet that is left on this screen overnight wakes up on it in the
    // morning, and with a date worked out only once, the first ticks of the
    // new day went onto yesterday.
    val today = fixedToday
        ?: remember(boundaryHour, refresh) {
            DayBoundary(boundaryHour).dateOf(Instant.now(), ZoneId.systemDefault())
        }

    var mode by remember { mutableStateOf(JournalMode.Day) }
    var viewDate by remember { mutableStateOf(today) }
    var shownToday by remember { mutableStateOf(today) }

    var entryText by remember { mutableStateOf("") }
    var savedText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }

    // A new day has started. The view follows it if it was on "today", and if
    // nothing is being written: an open edit stays on the day it belongs to.
    LaunchedEffect(today) {
        if (today != shownToday) {
            if (!editing && viewDate == shownToday) viewDate = today
            shownToday = today
        }
    }
    var summaries by remember { mutableStateOf<List<HabitSummary>>(emptyList()) }
    var routineRows by remember { mutableStateOf<List<RoutineStatus>>(emptyList()) }
    var daysWithEntries by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var hasInkEntry by remember { mutableStateOf(false) }

    // Coming back from the ink screen has to show that the day now has a
    // handwritten page, in the day view and in the month view.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }

    var optionsFor by remember { mutableStateOf<HabitSummary?>(null) }
    var addingHabit by remember { mutableStateOf(false) }
    var addingRoutineItem by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<HabitSummary?>(null) }

    LaunchedEffect(viewDate, refresh) {
        val loaded = withContext(AppDispatchers.io) {
            val document = habits.load()
            LoadedDay(
                entry = data.journalEntry(viewDate).orEmpty(),
                summaries = habits.summaries(today),
                monthDays = data.journalDatesIn(viewDate.year).toSet(),
                boundaryHour = document.dayBoundaryHour,
                routine = routine.statuses(today),
            ) to data.hasInkJournalEntry(viewDate)
        }.let { (day, ink) ->
            hasInkEntry = ink
            day
        }
        // An edit in progress is not thrown away by coming back to the screen.
        if (editing) {
            summaries = loaded.summaries
            daysWithEntries = loaded.monthDays
            routineRows = loaded.routine
            return@LaunchedEffect
        }
        entryText = loaded.entry
        savedText = loaded.entry
        summaries = loaded.summaries
        daysWithEntries = loaded.monthDays
        boundaryHour = loaded.boundaryHour
        routineRows = loaded.routine
    }

    fun saveEntry() {
        // Both taken now. The day on show can change before the write runs,
        // and the text belongs to the day it was written for.
        val text = entryText
        val date = viewDate
        scope.launch {
            val written = withContext(AppDispatchers.io) { data.writeJournalEntry(date, text) }
            if (written) {
                if (date == viewDate) savedText = text
                AppLog.i(TAG, "Saved the entry for $date")
            } else {
                AppLog.e(TAG, "Could not save the entry for $date")
            }
            refresh++
        }
    }

    /**
     * Ends an edit by saving it. Every control that shows another day or
     * another view calls this first. Without it, Previous and Next moved the
     * day under an open edit, and Save then wrote today's text over the entry
     * of another day.
     */
    fun commitEdit() {
        if (!editing) return
        if (entryText != savedText) saveEntry()
        editing = false
    }

    // The Home key, Back and "Today" all take this screen away with no
    // warning, and an entry that was being written must not go with it. The
    // coroutine scope is already gone by then, so this write is a plain one.
    fun saveOnTheWayOut(reason: String) {
        if (!editing || entryText == savedText) return
        val date = viewDate
        val text = entryText
        runCatching { data.writeJournalEntry(date, text) }
            .onSuccess { written ->
                if (written) savedText = text
                AppLog.i(TAG, "Saved the entry for $date ($reason): $written")
            }
            .onFailure { AppLog.e(TAG, "Lost the entry for $date ($reason)", it) }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { saveOnTheWayOut("the screen closed") }
    }
    // Another app in front: Android may stop this process without a word.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) {
        saveOnTheWayOut("the app went to the back")
    }

    fun openHandwriting() {
        runCatching {
            context.startActivity(
                InkNoteActivity.intent(
                    context,
                    StorageLayout.journalPath(viewDate, handwritten = true),
                    JournalStrings.dayTitle(viewDate),
                    PageTemplate.Lined,
                ),
            )
        }.onFailure { AppLog.e(TAG, "Could not open the handwritten entry", it) }
    }

    fun step(by: Long) {
        commitEdit()
        viewDate = if (mode == JournalMode.Day) viewDate.plusDays(by) else viewDate.plusMonths(by)
    }

    ScreenScaffold(
        title = when (mode) {
            JournalMode.Day -> JournalStrings.dayTitle(viewDate)
            JournalMode.Month -> JournalStrings.monthTitle(viewDate)
            JournalMode.Routine -> "Routine"
        },
        overline = if (mode == JournalMode.Routine) "Journal" else JournalStrings.dayOverline(viewDate, today),
        plant = Plants.Journal,
        modifier = modifier,
        // The routine is a page inside the Journal, so its way back leads to
        // the day, and only the day leads Home.
        onBack = if (mode == JournalMode.Routine) ({ mode = JournalMode.Day }) else onBack,
        backIcon = if (mode == JournalMode.Routine) Lucide.ArrowLeft else Lucide.House,
        backLabel = if (mode == JournalMode.Routine) "Back to the day" else "Home",
        actions = {
            if (mode == JournalMode.Routine) {
                IconPressButton(
                    icon = Lucide.Plus,
                    label = "Add a routine item",
                    bordered = true,
                    onClick = { addingRoutineItem = true },
                )
            } else {
                IconPressButton(
                    icon = Lucide.ChevronLeft,
                    label = if (mode == JournalMode.Day) "The day before" else "The month before",
                    onClick = { step(-1) },
                )
                IconPressButton(
                    icon = Lucide.CalendarDays,
                    label = "Month",
                    selected = mode == JournalMode.Month,
                    onClick = {
                        commitEdit()
                        mode = if (mode == JournalMode.Day) JournalMode.Month else JournalMode.Day
                    },
                )
                IconPressButton(
                    icon = Lucide.ChevronRight,
                    label = if (mode == JournalMode.Day) "The day after" else "The month after",
                    onClick = { step(1) },
                )
            }
        },
    ) {
        val wide = LocalWideScreen.current

        when (mode) {
            JournalMode.Routine -> PagedList(
                items = routineRows,
                pageSize = 4,
                rowHeight = RoutineRowHeight,
                emptyText = "No routine yet. Press the plus sign to add the first item.",
                modifier = Modifier.weight(1f),
            ) { index, status ->
                RoutineRow(
                    status = status,
                    isFirst = index == 0,
                    isLast = index == routineRows.size - 1,
                    onToggle = {
                        scope.launch {
                            withContext(AppDispatchers.io) {
                                routine.toggle(status.item.id, today)
                            }
                            refresh++
                        }
                    },
                    onMove = { by ->
                        scope.launch {
                            withContext(AppDispatchers.io) { routine.move(status.item.id, by) }
                            refresh++
                        }
                    },
                    onRemove = {
                        scope.launch {
                            withContext(AppDispatchers.io) { routine.remove(status.item.id) }
                            refresh++
                        }
                    },
                )
            }

            JournalMode.Month -> MonthView(
                month = viewDate,
                daysWithEntries = daysWithEntries,
                today = today,
                selected = viewDate,
                onSelectDay = { day ->
                    viewDate = day
                    mode = JournalMode.Day
                },
                modifier = Modifier.weight(1f),
            )

            JournalMode.Day -> {
                val entry: @Composable (Modifier) -> Unit = { entryModifier ->
                    DayEntry(
                        text = entryText,
                        savedText = savedText,
                        editing = editing,
                        hasInkEntry = hasInkEntry,
                        onText = { entryText = it },
                        onEdit = { editing = true },
                        onSave = {
                            saveEntry()
                            editing = false
                        },
                        onCancel = {
                            entryText = savedText
                            editing = false
                        },
                        onHandwrite = { openHandwriting() },
                        modifier = entryModifier,
                    )
                }
                val habitList: @Composable (Modifier) -> Unit = { listModifier ->
                    Column(modifier = listModifier) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CapsLabel(text = "Habits", style = EinkType.capsSmall, modifier = Modifier.weight(1f))
                            IconPressButton(icon = Lucide.ListPlus, label = "Add a habit", onClick = { addingHabit = true })
                            IconPressButton(
                                icon = Lucide.ListChecks,
                                label = "Routine",
                                onClick = {
                                    commitEdit()
                                    mode = JournalMode.Routine
                                },
                            )
                        }
                        HairlineDivider(color = EinkColors.Faded)
                        PagedList(
                            items = summaries,
                            pageSize = HABITS_PER_PAGE,
                            rowHeight = HabitRowHeight,
                            emptyText = "No habits yet",
                            modifier = Modifier.weight(1f),
                        ) { _, summary ->
                            HabitRow(
                                summary = summary,
                                onToggle = {
                                    scope.launch {
                                        withContext(AppDispatchers.io) {
                                            habits.toggle(summary.habit.id, today)
                                        }
                                        refresh++
                                    }
                                },
                                onOptions = { optionsFor = summary },
                            )
                        }
                    }
                }

                if (wide) {
                    // On its side the tablet has room for both halves of the
                    // day next to each other, and not for one under the other.
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(EinkDimens.blockGap),
                    ) {
                        entry(Modifier.weight(1f))
                        habitList(Modifier.weight(1f))
                    }
                } else {
                    entry(Modifier.fillMaxWidth())
                    // While the entry is being typed it has the page to itself.
                    // The keyboard covers the habits then anyway.
                    if (!editing) {
                        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                        habitList(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }

    val selected = optionsFor
    if (selected != null) {
        OptionsDialog(
            title = selected.habit.name,
            onDismiss = { optionsFor = null },
            options = listOf(
                DialogOption(label = "Rename", icon = Lucide.Pencil) {
                    renaming = selected
                    optionsFor = null
                },
                DialogOption(label = "Archive", icon = Lucide.Archive) {
                    scope.launch {
                        withContext(AppDispatchers.io) {
                            habits.setArchived(selected.habit.id, true)
                        }
                        refresh++
                    }
                    optionsFor = null
                },
                DialogOption(
                    label = "Longest streak: ${selected.longestStreak}",
                    enabled = false,
                    icon = Lucide.Flame,
                    onSelect = { },
                ),
            ),
        )
    }

    if (addingRoutineItem) {
        TextPromptDialog(
            title = "New routine item",
            confirmText = "Add",
            onConfirm = { label ->
                addingRoutineItem = false
                scope.launch {
                    withContext(AppDispatchers.io) { routine.add(label) }
                    refresh++
                }
            },
            onDismiss = { addingRoutineItem = false },
        )
    }

    if (addingHabit) {
        TextPromptDialog(
            title = "New habit",
            confirmText = "Add",
            onConfirm = { name ->
                addingHabit = false
                scope.launch {
                    withContext(AppDispatchers.io) { habits.add(name, today) }
                    refresh++
                }
            },
            onDismiss = { addingHabit = false },
        )
    }

    val beingRenamed = renaming
    if (beingRenamed != null) {
        TextPromptDialog(
            title = "Rename habit",
            initialValue = beingRenamed.habit.name,
            onConfirm = { name ->
                renaming = null
                scope.launch {
                    withContext(AppDispatchers.io) {
                        habits.rename(beingRenamed.habit.id, name)
                    }
                    refresh++
                }
            },
            onDismiss = { renaming = null },
        )
    }
}

/** Everything one load of the screen needs, read in one trip to the disk. */
private data class LoadedDay(
    val entry: String,
    val summaries: List<HabitSummary>,
    val monthDays: Set<LocalDate>,
    val boundaryHour: Int,
    val routine: List<RoutineStatus>,
)

/** A habit row: the name, the dots under it, and the rule. */
private val HabitRowHeight = 74.dp

/** A routine row: the name, where it leads, the two arrows, and the rule. */
private val RoutineRowHeight = 86.dp

/**
 * The written entry of one day.
 *
 * At rest it is the first lines of the entry with two icons beside them: one
 * to type, one to write by hand. While it is being typed it is a text box with
 * a tick to save and a cross to give up.
 */
@Composable
private fun DayEntry(
    text: String,
    savedText: String,
    editing: Boolean,
    hasInkEntry: Boolean,
    onText: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onHandwrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CapsLabel(text = "Entry", style = EinkType.capsSmall, modifier = Modifier.weight(1f))
            if (editing) {
                IconPressButton(icon = Lucide.X, label = "Cancel", onClick = onCancel)
                IconPressButton(icon = Lucide.Check, label = "Save", bordered = true, onClick = onSave)
            } else {
                IconPressButton(
                    icon = Lucide.Keyboard,
                    label = if (savedText.isBlank()) "Type an entry" else "Edit the entry",
                    onClick = onEdit,
                )
                IconPressButton(
                    icon = Lucide.Signature,
                    label = if (hasInkEntry) "Open the handwritten entry" else "Write by hand",
                    selected = hasInkEntry,
                    onClick = onHandwrite,
                )
            }
        }
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(10.dp))

        if (editing) {
            EinkTextField(
                value = text,
                onValueChange = onText,
                textStyle = EinkType.body,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp)
                    .einkFieldBorder()
                    .padding(14.dp),
            )
        } else {
            EinkText(
                text = savedText.ifBlank { "Nothing written yet." },
                style = EinkType.body.copy(
                    color = if (savedText.isBlank()) EinkColors.Faded else EinkColors.Ink,
                ),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
        }
    }
}
