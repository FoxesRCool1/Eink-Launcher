package io.github.foxesrcool1.einklauncher.ui.ink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import io.github.foxesrcool1.einklauncher.core.ink.InkNote
import io.github.foxesrcool1.einklauncher.core.ink.InkNoteLoad
import io.github.foxesrcool1.einklauncher.core.ink.InkNotesRepository
import io.github.foxesrcool1.einklauncher.core.ink.PageTemplate
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialog
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "InkNoteActivity"

/**
 * A handwritten note: pages of ink.
 *
 * The Writing tab opens it for a notebook, the Journal for the entry of a day,
 * and the reader for a note card on a highlight. They differ only in the path
 * of the file, so they share this one screen.
 *
 * Its own activity, like the typed editor, so the Home key closes the launcher
 * and not the page being written on.
 */
class InkNoteActivity : ComponentActivity() {

    private var controller: InkNoteController? = null
    private var session: FastPenSession? = null
    private var canvasView: InkCanvasView? = null
    private var fastPenMode = SettingsStore.FAST_PEN_OFF
    private var redrawDelay = SettingsStore.DEFAULT_INK_DELAY_MILLIS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank()) {
            AppLog.e(TAG, "Opened with no path")
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val newTemplate = PageTemplate.fromId(intent.getStringExtra(EXTRA_TEMPLATE))
        AppLog.i(TAG, "Opening $path")

        lifecycleScope.launch {
            val settings = SettingsStore(this@InkNoteActivity)
            fastPenMode = settings.fastPenMode.first()
            redrawDelay = settings.inkRedrawDelayMillis.first()

            val repository = InkNotesRepository(DataRoot.repository(this@InkNoteActivity))
            val started = System.currentTimeMillis()
            val load = withContext(Dispatchers.IO) { repository.load(path) }
            val (note, problem) = when (load) {
                is InkNoteLoad.Loaded -> load.note to null
                InkNoteLoad.Missing -> InkNote(template = newTemplate) to null
                is InkNoteLoad.Damaged -> InkNote() to load.reason
            }
            AppLog.i(
                TAG,
                "Read $path in ${System.currentTimeMillis() - started} ms: " +
                    "${note.pages.size} pages, ${note.pages.sumOf { it.strokes.size }} strokes",
            )

            val made = InkNoteController(repository, path, note, mayWrite = problem == null)
            controller = made
            setContent {
                EinkTheme {
                    InkNoteScreen(
                        title = title,
                        controller = made,
                        problem = problem,
                        onCanvas = { view ->
                            canvasView = view
                            made.attach(view)
                            view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> startFastPen() }
                        },
                        onDialog = { open -> if (open) session?.stop() else startFastPen() },
                        onWidth = { session?.applyWidth(it) },
                        onExport = { asPdf -> export(made, repository, title.ifBlank { "note" }, asPdf) },
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    private fun startFastPen() {
        val view = canvasView ?: return
        if (fastPenMode == SettingsStore.FAST_PEN_OFF || view.width == 0) return
        val current = session ?: FastPenSession(this, view, fastPenMode, redrawDelay).also { session = it }
        current.start()
    }

    private fun export(
        controller: InkNoteController,
        repository: InkNotesRepository,
        name: String,
        asPdf: Boolean,
    ): String {
        val note = controller.snapshot()
        val page = controller.pageIndex
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val written = if (asPdf) {
                    repository.writeExport("$name.pdf", InkExport.notePdf(note))
                } else {
                    repository.writeExport("$name page ${page + 1}.png", InkExport.pagePng(note, note.pages[page]))
                }
                AppLog.i(TAG, "Exported to $written")
            }.onFailure { AppLog.e(TAG, "Export failed", it) }
        }
        return if (asPdf) "The PDF goes to the exports folder" else "The picture goes to the exports folder"
    }

    override fun onResume() {
        super.onResume()
        startFastPen()
    }

    override fun onPause() {
        // The tablet must stop drawing on the glass before another screen shows.
        session?.stop()
        super.onPause()
    }

    override fun onStop() {
        controller?.saveAndWait()
        super.onStop()
    }

    override fun onDestroy() {
        controller?.close()
        super.onDestroy()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            finish()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        const val EXTRA_PATH = "ink_path"
        const val EXTRA_TITLE = "ink_title"
        const val EXTRA_TEMPLATE = "ink_template"

        fun intent(
            context: Context,
            path: String,
            title: String,
            templateForNew: PageTemplate = PageTemplate.Blank,
        ): Intent = Intent(context, InkNoteActivity::class.java)
            .putExtra(EXTRA_PATH, path)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEMPLATE, templateForNew.id)
    }
}

private val ToolPadding = PaddingValues(horizontal = 9.dp, vertical = 16.dp)

@Composable
internal fun InkNoteScreen(
    title: String,
    controller: InkNoteController,
    problem: String?,
    onCanvas: (InkCanvasView) -> Unit,
    onDialog: (open: Boolean) -> Unit,
    onWidth: (Float) -> Unit,
    onExport: (asPdf: Boolean) -> String,
    onClose: () -> Unit,
) {
    var canvas by remember { mutableStateOf<InkCanvasView?>(null) }
    var mode by remember { mutableStateOf(InkMode.Pen) }
    var width by remember { mutableFloatStateOf(PenWidths.MEDIUM) }
    var pageIndex by remember { mutableIntStateOf(controller.pageIndex) }
    var pageCount by remember { mutableIntStateOf(controller.pageCount) }
    var menuOpen by remember { mutableStateOf(false) }
    var templatesOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf(problem?.let { "This note could not be read and will not be changed: $it" }) }

    fun refreshPages() {
        pageIndex = controller.pageIndex
        pageCount = controller.pageCount
    }

    fun turn(by: Int) {
        if (controller.goTo(controller.pageIndex + by)) refreshPages()
    }

    fun pick(next: InkMode) {
        mode = next
        canvas?.mode = next
    }

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
            InvertPressButton(text = "Close", onClick = onClose, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Pen", selected = mode == InkMode.Pen, onClick = { pick(InkMode.Pen) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Marker", selected = mode == InkMode.Highlighter, onClick = { pick(InkMode.Highlighter) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Eraser", selected = mode == InkMode.Eraser, onClick = { pick(InkMode.Eraser) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(
                text = PenWidths.label(width),
                onClick = {
                    width = PenWidths.all[(PenWidths.all.indexOf(width) + 1) % PenWidths.all.size]
                    canvas?.penWidth = width
                    onWidth(width)
                },
                contentPadding = ToolPadding,
                compact = true,
            )
            InvertPressButton(text = "Undo", onClick = { canvas?.undo() }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Redo", onClick = { canvas?.redo() }, contentPadding = ToolPadding, compact = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InvertPressButton(text = "Previous", enabled = pageIndex > 0, onClick = { turn(-1) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(text = "Next", enabled = pageIndex < pageCount - 1, onClick = { turn(1) }, contentPadding = ToolPadding, compact = true)
            InvertPressButton(
                text = "Add page",
                onClick = {
                    controller.addPage()
                    refreshPages()
                },
                contentPadding = ToolPadding,
                compact = true,
            )
            InvertPressButton(
                text = "More",
                onClick = {
                    onDialog(true)
                    menuOpen = true
                },
                contentPadding = ToolPadding,
                compact = true,
            )
            CapsLabel(
                text = "Page ${pageIndex + 1} of $pageCount" + if (title.isBlank()) "" else "  .  $title",
                style = EinkType.capsSmall,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        notice?.let {
            EinkText(text = it, style = EinkType.body, maxLines = 2, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        HairlineDivider()

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                InkCanvasView(context).also { view ->
                    view.onPageSwipe = { direction -> turn(direction) }
                    canvas = view
                    onCanvas(view)
                }
            },
        )
    }

    if (menuOpen) {
        OptionsDialog(
            title = "Page ${pageIndex + 1}",
            options = listOf(
                DialogOption("Page template") { menuOpen = false; templatesOpen = true },
                DialogOption("Export this page as a picture") { menuOpen = false; onDialog(false); notice = onExport(false) },
                DialogOption("Export the note as a PDF") { menuOpen = false; onDialog(false); notice = onExport(true) },
                DialogOption("Clear this page") { menuOpen = false; confirmClear = true },
                DialogOption("Delete this page") { menuOpen = false; confirmDelete = true },
            ),
            onDismiss = { menuOpen = false; onDialog(false) },
        )
    }

    if (templatesOpen) {
        OptionsDialog(
            title = "Page template",
            options = PageTemplate.entries.map { template ->
                DialogOption(template.label) {
                    controller.changeTemplate(template)
                    templatesOpen = false
                    onDialog(false)
                }
            },
            onDismiss = { templatesOpen = false; onDialog(false) },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear this page?",
            message = "All ink on this page goes. Undo brings it back until you leave the page.",
            confirmText = "Clear",
            cancelText = "Keep",
            onConfirm = { canvas?.clearPage(); confirmClear = false; onDialog(false) },
            onDismiss = { confirmClear = false; onDialog(false) },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this page?",
            message = "The page and its ink go for good.",
            confirmText = "Delete",
            cancelText = "Keep",
            onConfirm = { controller.deletePage(); refreshPages(); confirmDelete = false; onDialog(false) },
            onDismiss = { confirmDelete = false; onDialog(false) },
        )
    }
}
