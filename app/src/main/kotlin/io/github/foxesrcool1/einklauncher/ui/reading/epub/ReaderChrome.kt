package io.github.foxesrcool1.einklauncher.ui.reading.epub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.reading.Highlight
import io.github.foxesrcool1.einklauncher.core.reading.ReadingLog
import io.github.foxesrcool1.einklauncher.core.settings.ReaderSettings
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkRow
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.PagedList

private val BarPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)

/**
 * Everything the reader draws on top of the page.
 *
 * When nothing is open this draws nothing at all, and the book view under it
 * gets every touch. The page is the whole screen.
 */
@Composable
fun ReaderChrome(state: ReaderUiState, actions: ReaderActions) {
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        when {
            state.loading -> FullPanel(title = state.title.ifBlank { "Opening" }, onClose = actions::close, closeText = "Cancel") {
                EinkText(text = "Opening the book.", style = EinkType.body)
            }

            state.failure != null -> FullPanel(title = "Read", onClose = actions::close, closeText = "Back") {
                EinkText(text = state.failure.orEmpty(), style = EinkType.body)
            }

            state.selecting -> SelectionBar(actions, Modifier.align(Alignment.BottomCenter))

            else -> when (val panel = state.panel) {
                ReaderPanel.None -> Unit
                ReaderPanel.Menu -> MenuBar(state, actions, Modifier.align(Alignment.TopCenter))
                ReaderPanel.Contents -> ContentsPanel(state, actions)
                ReaderPanel.Notes -> NotesPanel(state, actions)
                ReaderPanel.Text -> TextPanel(state, actions, Modifier.align(Alignment.BottomCenter))
                is ReaderPanel.HighlightOptions -> HighlightPanel(state, panel.highlightId, actions, Modifier.align(Alignment.BottomCenter))
                is ReaderPanel.NoteEditor -> NoteEditorPanel(state, panel.highlightId, actions)
            }
        }
    }
}

@Composable
private fun MenuBar(state: ReaderUiState, actions: ReaderActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(EinkColors.Paper)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InvertPressButton(text = "Close book", onClick = actions::close, contentPadding = BarPadding, compact = true)
            InvertPressButton(text = "Contents", onClick = { actions.show(ReaderPanel.Contents) }, contentPadding = BarPadding, compact = true)
            InvertPressButton(text = "Notes", onClick = { actions.show(ReaderPanel.Notes) }, contentPadding = BarPadding, compact = true)
            InvertPressButton(text = "Text", onClick = { actions.show(ReaderPanel.Text) }, contentPadding = BarPadding, compact = true)
            InvertPressButton(text = "Back to page", onClick = { actions.show(ReaderPanel.None) }, contentPadding = BarPadding, compact = true, bordered = false)
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
            EinkText(text = state.title, style = EinkType.rowTitle, maxLines = 1)
            CapsLabel(text = state.placeLabel, style = EinkType.capsSmall)
            if (state.settings.goalMinutes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                CapsLabel(
                    text = "Today ${state.secondsToday / 60} of ${state.settings.goalMinutes} minutes",
                    style = EinkType.capsSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                GoalLine(ReadingLog.goalFraction(state.secondsToday, state.settings.goalMinutes))
            }
        }
        HairlineDivider(thickness = EinkDimens.rule)
    }
}

/** The daily goal as a thin line: a hairline for the whole goal, a heavy rule for the part done. */
@Composable
fun GoalLine(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(4.dp)) {
        Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth().height(1.dp).background(EinkColors.Ink))
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(EinkColors.Ink),
            )
        }
    }
}

@Composable
private fun SelectionBar(actions: ReaderActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(EinkColors.Paper)) {
        HairlineDivider(thickness = EinkDimens.rule)
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
        ) {
            InvertPressButton(text = "Highlight", onClick = actions::highlight)
            InvertPressButton(text = "Note", onClick = actions::highlightWithNote)
            InvertPressButton(text = "Cancel", onClick = actions::cancelSelection, bordered = false)
        }
    }
}

@Composable
private fun FullPanel(
    title: String,
    onClose: () -> Unit,
    closeText: String = "Back to page",
    footer: @Composable () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EinkColors.Paper)
            .padding(EinkDimens.screenMargin),
    ) {
        EinkText(text = title, style = EinkType.title, maxLines = 1)
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        HairlineDivider(thickness = EinkDimens.rule)
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        Column(modifier = Modifier.weight(1f)) { content() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
        ) {
            footer()
            Spacer(modifier = Modifier.weight(1f))
            InvertPressButton(text = closeText, onClick = onClose, bordered = false)
        }
    }
}

@Composable
private fun ContentsPanel(state: ReaderUiState, actions: ReaderActions) {
    FullPanel(title = "Contents", onClose = { actions.show(ReaderPanel.None) }) {
        PagedList(
            items = state.contents,
            pageSize = 9,
            emptyText = "This book has no table of contents",
        ) { _, row ->
            EinkRow(onClick = { actions.goTo(row) }) { pressed ->
                EinkText(
                    text = row.title,
                    style = (if (row.depth == 0) EinkType.rowTitle else EinkType.body)
                        .copy(color = if (pressed) EinkColors.Paper else EinkColors.Ink),
                    maxLines = 1,
                    modifier = Modifier.padding(start = (row.depth * 20).coerceAtMost(60).dp),
                )
            }
            HairlineDivider(color = EinkColors.Faded)
        }
    }
}

@Composable
private fun NotesPanel(state: ReaderUiState, actions: ReaderActions) {
    FullPanel(
        title = "Notes",
        onClose = { actions.show(ReaderPanel.None) },
        footer = {
            InvertPressButton(
                text = "Export as Markdown",
                enabled = state.annotations.highlights.isNotEmpty(),
                onClick = actions::exportNotes,
            )
        },
    ) {
        state.notice?.let {
            CapsLabel(text = it, style = EinkType.capsSmall, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
        }
        PagedList(
            items = state.annotations.highlights,
            pageSize = 5,
            emptyText = "Hold a word to select text, then press Highlight",
        ) { _, highlight ->
            EinkRow(
                onClick = { actions.goTo(highlight) },
                onLongClick = { actions.show(ReaderPanel.HighlightOptions(highlight.id)) },
            ) { pressed ->
                val colour = if (pressed) EinkColors.Paper else EinkColors.Ink
                EinkText(text = highlight.text.ifBlank { "(no text)" }, style = EinkType.body.copy(color = colour), maxLines = 2)
                CapsLabel(
                    text = noteSummary(highlight),
                    style = EinkType.capsSmall.copy(color = if (pressed) EinkColors.Paper else EinkColors.Faded),
                )
            }
            HairlineDivider(color = EinkColors.Faded)
        }
    }
}

private fun noteSummary(highlight: Highlight): String = buildString {
    append("${(highlight.progression * 100).toInt()} %")
    if (highlight.chapter.isNotBlank()) append("  .  ").append(highlight.chapter)
    if (highlight.note.isNotBlank()) append("  .  ").append(highlight.note.lineSequence().first())
    if (highlight.inkNotePath.isNotBlank()) append("  .  handwritten note")
}

@Composable
private fun HighlightPanel(state: ReaderUiState, highlightId: String, actions: ReaderActions, modifier: Modifier = Modifier) {
    val highlight = state.annotations.highlight(highlightId)
    Column(modifier = modifier.fillMaxWidth().background(EinkColors.Paper)) {
        HairlineDivider(thickness = EinkDimens.rule)
        Column(modifier = Modifier.padding(16.dp)) {
            EinkText(text = highlight?.text.orEmpty(), style = EinkType.body, maxLines = 3)
            if (!highlight?.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                EinkText(text = "Note: ${highlight?.note}", style = EinkType.body, maxLines = 4)
            }
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                InvertPressButton(
                    text = if (highlight?.note.isNullOrBlank()) "Add note" else "Edit note",
                    onClick = { actions.show(ReaderPanel.NoteEditor(highlightId)) },
                )
                InvertPressButton(
                    text = if (highlight?.inkNotePath.isNullOrBlank()) "Handwrite" else "Open card",
                    onClick = { actions.handwrite(highlightId) },
                )
                InvertPressButton(text = "Remove", onClick = { actions.remove(highlightId) })
                InvertPressButton(text = "Close", onClick = { actions.show(ReaderPanel.None) }, bordered = false)
            }
        }
    }
}

@Composable
private fun NoteEditorPanel(state: ReaderUiState, highlightId: String, actions: ReaderActions) {
    val highlight = state.annotations.highlight(highlightId)
    var text by remember(highlightId) { mutableStateOf(highlight?.note.orEmpty()) }

    FullPanel(
        title = "Note",
        onClose = { actions.show(ReaderPanel.None) },
        closeText = "Cancel",
        footer = { InvertPressButton(text = "Save", onClick = { actions.saveNote(highlightId, text) }) },
    ) {
        EinkText(text = highlight?.text.orEmpty(), style = EinkType.body, maxLines = 4)
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = EinkType.body,
            cursorBrush = SolidColor(EinkColors.Ink),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(width = EinkDimens.hairline, color = EinkColors.Ink)
                .padding(12.dp),
        )
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
    }
}

@Composable
private fun TextPanel(state: ReaderUiState, actions: ReaderActions, modifier: Modifier = Modifier) {
    val settings = state.settings
    // Two short pages and not one tall one, so half of the book page stays in
    // view and the reader can see what a change does.
    var readingPage by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().background(EinkColors.Paper)) {
        HairlineDivider(thickness = EinkDimens.rule)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!readingPage) {
                SettingRow("Font", settings.fontLabel) {
                    InvertPressButton(text = "Change", onClick = { actions.change(settings.nextFont()) }, contentPadding = BarPadding)
                }
                StepRow("Size", "${settings.fontSizePercent} %", { actions.change(settings.smaller()) }, { actions.change(settings.larger()) })
                StepRow("Margins", "${settings.marginPercent} %", { actions.change(settings.narrowerMargins()) }, { actions.change(settings.widerMargins()) })
                if (settings.font != ReaderSettings.FONT_PUBLISHER) {
                    StepRow("Line spacing", "${settings.lineHeightPercent} %", { actions.change(settings.tighterLines()) }, { actions.change(settings.looserLines()) })
                    SettingRow("Justify", if (settings.justify) "On" else "Off") {
                        InvertPressButton(text = "Change", onClick = { actions.change(settings.copy(justify = !settings.justify)) }, contentPadding = BarPadding)
                    }
                }
            } else {
                SettingRow("Highlights", if (settings.underlineHighlights) "Underline" else "Grey block") {
                    InvertPressButton(text = "Change", onClick = { actions.change(settings.copy(underlineHighlights = !settings.underlineHighlights)) }, contentPadding = BarPadding)
                }
                SettingRow("Full refresh", if (settings.refreshEveryPages == 0) "Never" else "Every ${settings.refreshEveryPages} pages") {
                    InvertPressButton(text = "Change", onClick = { actions.change(settings.nextRefresh()) }, contentPadding = BarPadding)
                }
                SettingRow("Daily goal", if (settings.goalMinutes == 0) "None" else "${settings.goalMinutes} minutes") {
                    InvertPressButton(text = "Change", onClick = { actions.change(settings.nextGoal()) }, contentPadding = BarPadding)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                InvertPressButton(text = "Type", selected = !readingPage, onClick = { readingPage = false }, contentPadding = BarPadding)
                InvertPressButton(text = "Reading", selected = readingPage, onClick = { readingPage = true }, contentPadding = BarPadding)
                Spacer(modifier = Modifier.weight(1f))
                InvertPressButton(text = "Back to page", onClick = { actions.show(ReaderPanel.None) }, bordered = false)
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, controls: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            CapsLabel(text = label, style = EinkType.capsSmall)
            EinkText(text = value, style = EinkType.body, maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { controls() }
    }
}

@Composable
private fun StepRow(label: String, value: String, onLess: () -> Unit, onMore: () -> Unit) {
    SettingRow(label, value) {
        InvertPressButton(text = "Less", onClick = onLess, contentPadding = BarPadding)
        InvertPressButton(text = "More", onClick = onMore, contentPadding = BarPadding)
    }
}
