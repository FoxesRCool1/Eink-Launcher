package io.github.foxesrcool1.einklauncher.ui.reading.pdf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.foxesrcool1.einklauncher.ui.ink.InkCanvasView
import io.github.foxesrcool1.einklauncher.ui.ink.InkMode

/** The PDF reader screen with nothing behind it, so a test can show its layout. */
object PdfReaderScreenshotSupport {

    @Composable
    fun Screen(split: Boolean) {
        val state = remember {
            PdfUiState().apply {
                loading = false
                title = "Walden"
                pageIndex = 11
                pageCount = 240
                this.split = split
            }
        }
        val actions = remember {
            object : PdfActions {
                override fun close() = Unit
                override fun attach(canvas: InkCanvasView) = Unit
                override fun step(direction: Int) = Unit
                override fun goToPage(pageNumber: Int) = Unit
                override fun pick(mode: InkMode) { state.mode = mode }
                override fun nextZoom() = Unit
                override fun toggleCrop() = Unit
                override fun nextPenWidth() = Unit
                override fun undo() = Unit
                override fun redo() = Unit
                override fun show(panel: PdfPanel) = Unit
                override fun exportPage() = Unit
                override fun exportNotes() = Unit
                override fun toggleMouse() = Unit
                override fun toggleSplit() { state.split = !state.split }
                override fun noteCanvas(canvas: InkCanvasView?) = Unit
            }
        }
        PdfReaderScreen(state, actions)
    }
}
