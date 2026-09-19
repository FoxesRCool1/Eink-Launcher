package io.github.foxesrcool1.einklauncher.ui.journal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.habits.DayBoundary
import io.github.foxesrcool1.einklauncher.core.habits.HabitSummary
import io.github.foxesrcool1.einklauncher.core.habits.HabitsRepository
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.TextPromptDialog
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "JournalScreen"
private const val HABITS_PER_PAGE = 3

private enum class JournalMode { Day, Month }

/**
 * The Journal tab: one entry per day, a month view, and the habits.
 *
 * The handwritten entry is not here. It needs the ink engine, which is step 5.
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

    var boundaryHour by remember { mutableIntStateOf(DayBoundary.DEFAULT_HOUR) }
    val today = fixedToday
        ?: remember(boundaryHour) {
            DayBoundary(boundaryHour).dateOf(Instant.now(), ZoneId.systemDefault())
        }

    var mode by remember { mutableStateOf(JournalMode.Day) }
    var viewDate by remember(today) { mutableStateOf(today) }
    var refresh by remember { mutableIntStateOf(0) }

    var entryText by remember { mutableStateOf("") }
    var savedText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var summaries by remember { mutableStateOf<List<HabitSummary>>(emptyList()) }
    var daysWithEntries by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }

    var optionsFor by remember { mutableStateOf<HabitSummary?>(null) }
    var addingHabit by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<HabitSummary?>(null) }

    LaunchedEffect(viewDate, refresh) {
        val loaded = withContext(Dispatchers.IO) {
            val document = habits.load()
            LoadedDay(
                entry = data.journalEntry(viewDate).orEmpty(),
                summaries = habits.summaries(today),
                monthDays = data.journalDatesIn(viewDate.year).toSet(),
                boundaryHour = document.dayBoundaryHour,
            )
        }
        entryText = loaded.entry
        savedText = loaded.entry
        summaries = loaded.summaries
        daysWithEntries = loaded.monthDays
        boundaryHour = loaded.boundaryHour
    }

    fun saveEntry() {
        val text = entryText
        scope.launch {
            val written = withContext(Dispatchers.IO) { data.writeJournalEntry(viewDate, text) }
            if (written) {
                savedText = text
                AppLog.i(TAG, "Saved the entry for $viewDate")
            } else {
                AppLog.e(TAG, "Could not save the entry for $viewDate")
            }
            refresh++
        }
    }

    ScreenScaffold(
        title = if (mode == JournalMode.Day) {
            JournalStrings.dayTitle(viewDate)
        } else {
            JournalStrings.monthTitle(viewDate)
        },
        overline = JournalStrings.dayOverline(viewDate, today),
        corner = null,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(
                text = "Previous",
                onClick = {
                    viewDate = if (mode == JournalMode.Day) {
                        viewDate.minusDays(1)
                    } else {
                        viewDate.minusMonths(1)
                    }
                },
            )
            InvertPressButton(
                text = if (mode == JournalMode.Day) "Month" else "Day",
                selected = mode == JournalMode.Month,
                onClick = {
                    mode = if (mode == JournalMode.Day) JournalMode.Month else JournalMode.Day
                },
            )
            InvertPressButton(
                text = "Next",
                onClick = {
                    viewDate = if (mode == JournalMode.Day) {
                        viewDate.plusDays(1)
                    } else {
                        viewDate.plusMonths(1)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))

        when (mode) {
            JournalMode.Month -> {
                MonthView(
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
            }

            JournalMode.Day -> {
                CapsLabel(text = "Entry", style = EinkType.capsSmall)
                HairlineDivider(color = EinkColors.Faded)
                Spacer(modifier = Modifier.height(10.dp))

                if (editing) {
                    BasicTextField(
                        value = entryText,
                        onValueChange = { entryText = it },
                        textStyle = EinkType.body,
                        cursorBrush = SolidColor(EinkColors.Ink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .border(EinkDimens.hairline, EinkColors.Ink)
                            .padding(12.dp),
                    )
                    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                        InvertPressButton(
                            text = "Save",
                            onClick = {
                                saveEntry()
                                editing = false
                            },
                        )
                        InvertPressButton(
                            text = "Cancel",
                            onClick = {
                                entryText = savedText
                                editing = false
                            },
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
                        EinkText(
                            text = savedText.ifBlank { "Nothing written yet." },
                            style = EinkType.body.copy(
                                color = if (savedText.isBlank()) {
                                    EinkColors.Faded
                                } else {
                                    EinkColors.Ink
                                },
                            ),
                            maxLines = 3,
                        )
                    }
                    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                        InvertPressButton(
                            text = if (savedText.isBlank()) "Write" else "Edit",
                            onClick = { editing = true },
                        )
                        InvertPressButton(
                            text = "Handwrite",
                            enabled = false,
                            onClick = { },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(EinkDimens.blockGap))
                CapsLabel(text = "Habits", style = EinkType.capsSmall)
                HairlineDivider(color = EinkColors.Faded)

                PagedList(
                    items = summaries,
                    pageSize = HABITS_PER_PAGE,
                    emptyText = "No habits yet",
                    modifier = Modifier.weight(1f),
                ) { _, summary ->
                    HabitRow(
                        summary = summary,
                        onToggle = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = EinkDimens.targetGap),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InvertPressButton(text = "Add habit", onClick = { addingHabit = true })
            InvertPressButton(text = "Today", onClick = onBack, bordered = false)
        }
    }

    val selected = optionsFor
    if (selected != null) {
        OptionsDialog(
            title = selected.habit.name,
            onDismiss = { optionsFor = null },
            options = listOf(
                DialogOption(label = "Rename") {
                    renaming = selected
                    optionsFor = null
                },
                DialogOption(label = "Archive") {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            habits.setArchived(selected.habit.id, true)
                        }
                        refresh++
                    }
                    optionsFor = null
                },
                DialogOption(
                    label = "Longest streak: ${selected.longestStreak}",
                    enabled = false,
                    onSelect = { },
                ),
            ),
        )
    }

    if (addingHabit) {
        TextPromptDialog(
            title = "New habit",
            confirmText = "Add",
            onConfirm = { name ->
                addingHabit = false
                scope.launch {
                    withContext(Dispatchers.IO) { habits.add(name, today) }
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
                    withContext(Dispatchers.IO) {
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
)
