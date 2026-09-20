package io.github.foxesrcool1.einklauncher.ui.ink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import io.github.foxesrcool1.einklauncher.core.threads.AppDispatchers
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.window.ScreenWindow
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialog
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.IconPressButton
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.ui.common.RotateButton
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
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
        ScreenWindow.attach(this)

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
            val load = withContext(AppDispatchers.io) { repository.load(path) }
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
                            // A new canvas after a turn of the screen. The
                            // fast pen was tied to the old one.
                            if (canvasView !== view) {
                                session?.stop()
                                session = null
                            }
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
        lifecycleScope.launch(AppDispatchers.io) {
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
        // Saved here and not in onStop. The screen that opened this one
        // resumes before this one stops, and it looks for the file when it
        // resumes. Found on the emulator: the Journal said "Handwrite" for a
        // day that had just been written on.
        controller?.saveAndWait()
        super.onPause()
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

/** The fine line between a rail of tools and the page. */
@Composable
internal fun VerticalRule() {
    Box(modifier = Modifier.fillMaxHeight().width(EinkDimens.hairline).background(EinkColors.Ink))
}

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

    val drawTools: @Composable () -> Unit = {
        IconPressButton(icon = Lucide.X, label = "Close", onClick = onClose)
        InkModeButtons(mode = mode, onPick = ::pick)
        PenWidthButton(
            width = width,
            onNext = {
                width = PenWidths.all[(PenWidths.all.indexOf(width) + 1) % PenWidths.all.size]
                canvas?.penWidth = width
                onWidth(width)
            },
        )
        IconPressButton(icon = Lucide.Undo2, label = "Undo", onClick = { canvas?.undo() })
        IconPressButton(icon = Lucide.Redo2, label = "Redo", onClick = { canvas?.redo() })
    }
    val pageTools: @Composable () -> Unit = {
        IconPressButton(icon = Lucide.ChevronLeft, label = "Previous page", enabled = pageIndex > 0, onClick = { turn(-1) })
        IconPressButton(icon = Lucide.ChevronRight, label = "Next page", enabled = pageIndex < pageCount - 1, onClick = { turn(1) })
        IconPressButton(
            icon = Lucide.FilePlus,
            label = "Add a page",
            onClick = {
                controller.addPage()
                refreshPages()
            },
        )
        IconPressButton(
            icon = Lucide.Ellipsis,
            label = "More",
            onClick = {
                onDialog(true)
                menuOpen = true
            },
        )
    }
    val page: @Composable (Modifier) -> Unit = { pageModifier ->
        AndroidView(
            modifier = pageModifier,
            factory = { context ->
                InkCanvasView(context).also { view ->
                    view.onPageSwipe = { direction -> turn(direction) }
                    canvas = view
                    onCanvas(view)
                }
            },
        )
    }
    val pageLabel = "${pageIndex + 1} of $pageCount"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EinkColors.Paper)
            .systemBarsPadding(),
    ) {
        if (maxWidth > maxHeight) {
            // On its side the tablet has width to spare and no height, so the
            // tools stand in a rail down each edge and the page keeps the
            // whole height.
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) { drawTools() }
                VerticalRule()
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    notice?.let {
                        EinkText(text = it, style = EinkType.body, maxLines = 2, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    page(Modifier.fillMaxSize())
                }
                VerticalRule()
                Column(
                    modifier = Modifier.fillMaxHeight().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RotateButton()
                    pageTools()
                    Spacer(modifier = Modifier.weight(1f))
                    CapsLabel(text = pageLabel, style = EinkType.capsSmall)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) { drawTools() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pageTools()
                    CapsLabel(
                        text = pageLabel + if (title.isBlank()) "" else "  .  $title",
                        style = EinkType.capsSmall,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    RotateButton()
                }
                notice?.let {
                    EinkText(text = it, style = EinkType.body, maxLines = 2, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
                HairlineDivider()
                page(Modifier.fillMaxSize())
            }
        }
    }

    if (menuOpen) {
        OptionsDialog(
            title = "Page ${pageIndex + 1}",
            options = listOf(
                DialogOption("Page template", icon = Lucide.FileText) { menuOpen = false; templatesOpen = true },
                DialogOption("Export this page as a picture", icon = Lucide.Image) { menuOpen = false; onDialog(false); notice = onExport(false) },
                DialogOption("Export the note as a PDF", icon = Lucide.Share) { menuOpen = false; onDialog(false); notice = onExport(true) },
                DialogOption("Clear this page", icon = Lucide.Eraser) { menuOpen = false; confirmClear = true },
                DialogOption("Delete this page", icon = Lucide.Trash2) { menuOpen = false; confirmDelete = true },
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
