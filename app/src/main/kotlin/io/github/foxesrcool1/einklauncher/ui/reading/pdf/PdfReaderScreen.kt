package io.github.foxesrcool1.einklauncher.ui.reading.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkDialog
import io.github.foxesrcool1.einklauncher.design.components.EinkRow
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.TextPromptDialog
import io.github.foxesrcool1.einklauncher.ui.ink.InkCanvasView
import io.github.foxesrcool1.einklauncher.ui.ink.InkMode
import io.github.foxesrcool1.einklauncher.ui.ink.PenWidths

private val ToolPadding = PaddingValues(horizontal = 9.dp, vertical = 16.dp)

@Composable
fun PdfReaderScreen(state: PdfUiState, actions: PdfActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EinkColors.Paper)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InvertPressButton(text = "Close", onClick = actions::close, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Pen", selected = state.mode == InkMode.Pen, onClick = { actions.pick(InkMode.Pen) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Marker", selected = state.mode == InkMode.Highlighter, onClick = { actions.pick(InkMode.Highlighter) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Eraser", selected = state.mode == InkMode.Eraser, onClick = { actions.pick(InkMode.Eraser) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Undo", onClick = actions::undo, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = state.zoom.label, onClick = actions::nextZoom, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "More", onClick = { actions.show(PdfPanel.More) }, contentPadding = ToolPadding, compact = true)
        }
        HairlineDivider()

        when {
            state.failure != null -> Message(state.failure.orEmpty())
            else -> AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { context -> InkCanvasView(context).also(actions::attach) },
            )
        }

        HairlineDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapsLabel(
                text = when {
                    state.loading -> "Opening"
                    state.notice != null -> state.notice.orEmpty()
                    else -> buildString {
                        append("Page ${state.pageIndex + 1} of ${state.pageCount}")
                        if (state.screenCount > 1) append("  .  part ${state.screenIndex + 1} of ${state.screenCount}")
                        if (state.cropMargins) append("  .  margins cropped")
                    }
                },
                style = EinkType.capsSmall,
                modifier = Modifier.weight(1f),
            )
            CapsLabel(text = state.title, style = EinkType.capsSmall.copy(color = EinkColors.Faded))
        }
    }

    when (state.panel) {
        PdfPanel.None -> Unit

        PdfPanel.More -> OptionsDialog(
            title = "Page ${state.pageIndex + 1} of ${state.pageCount}",
            onDismiss = { actions.show(PdfPanel.None) },
            options = listOf(
                DialogOption("Go to page") { actions.show(PdfPanel.GoTo) },
                DialogOption(if (state.cropMargins) "Show the whole page" else "Crop the margins") { actions.toggleCrop() },
                DialogOption("Pen width: ${PenWidths.label(state.penWidth)}") { actions.nextPenWidth() },
                DialogOption("Redo") { actions.redo(); actions.show(PdfPanel.None) },
                DialogOption("Pages with handwriting") { actions.show(PdfPanel.InkPages) },
                DialogOption("Export this page as a picture") { actions.exportPage() },
            ),
        )

        PdfPanel.GoTo -> TextPromptDialog(
            title = "Go to page, 1 to ${state.pageCount}",
            initialValue = (state.pageIndex + 1).toString(),
            confirmText = "Go",
            onConfirm = { typed -> actions.goToPage(typed.trim().toIntOrNull() ?: (state.pageIndex + 1)) },
            onDismiss = { actions.show(PdfPanel.None) },
        )

        PdfPanel.InkPages -> InkPagesDialog(state, actions)
    }
}

@Composable
private fun Message(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(EinkDimens.screenMargin)) {
        EinkText(text = text, style = EinkType.body)
    }
}

/** The annotation list of a PDF: every page that has ink, with a jump to it. */
@Composable
private fun InkPagesDialog(state: PdfUiState, actions: PdfActions) {
    EinkDialog(
        onDismissRequest = { actions.show(PdfPanel.None) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EinkColors.Paper)
                .padding(EinkDimens.screenMargin),
        ) {
            EinkText(text = "Handwriting", style = EinkType.title, maxLines = 1)
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            HairlineDivider(thickness = EinkDimens.rule)
            state.notice?.let {
                Spacer(modifier = Modifier.height(8.dp))
                CapsLabel(text = it, style = EinkType.capsSmall, maxLines = 2)
            }
            PagedList(
                items = state.inkPages,
                pageSize = 8,
                emptyText = "No page of this PDF has handwriting on it yet",
                modifier = Modifier.weight(1f),
            ) { _, page ->
                EinkRow(onClick = { actions.goToPage(page.pageNumber) }) { pressed ->
                    val colour = if (pressed) EinkColors.Paper else EinkColors.Ink
                    EinkText(text = "Page ${page.pageNumber}", style = EinkType.rowTitle.copy(color = colour), maxLines = 1)
                    CapsLabel(
                        text = "${page.strokeCount} pen strokes",
                        style = EinkType.capsSmall.copy(color = if (pressed) EinkColors.Paper else EinkColors.Faded),
                    )
                }
                HairlineDivider(color = EinkColors.Faded)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                InvertPressButton(text = "Export as Markdown", enabled = state.inkPages.isNotEmpty(), onClick = actions::exportNotes)
                Spacer(modifier = Modifier.weight(1f))
                InvertPressButton(text = "Back to page", onClick = { actions.show(PdfPanel.None) }, bordered = false)
            }
        }
    }
}
