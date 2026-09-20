package io.github.foxesrcool1.einklauncher.ui.ink

import android.os.Handler
import android.os.Looper
import io.github.foxesrcool1.einklauncher.core.ink.InkNote
import io.github.foxesrcool1.einklauncher.core.ink.InkNotesRepository
import io.github.foxesrcool1.einklauncher.core.ink.InkPageData
import io.github.foxesrcool1.einklauncher.core.ink.InkStroke
import io.github.foxesrcool1.einklauncher.core.ink.PageTemplate
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "InkNoteController"

/**
 * Holds one open ink note: its pages, which page is showing, and the saving.
 *
 * The canvas owns the strokes of the page on screen. This class owns all the
 * other pages, and takes the strokes back from the canvas on a page turn and
 * on a save.
 *
 * Saving never blocks the pen. A save takes a snapshot on the main thread,
 * which is only a list copy, and does the slow part (the preview picture, the
 * zip, the disk) on one background thread. One thread, so two saves can never
 * land in the wrong order.
 */
class InkNoteController(
    private val repository: InkNotesRepository,
    private val path: String,
    initial: InkNote,
    /** False for a file that could not be read. It is shown as empty and never written over. */
    private val mayWrite: Boolean,
) {
    private class PageSlot(var strokes: List<InkStroke>, var preview: ByteArray?, var previewStale: Boolean)

    private val pages = initial.pages.map { PageSlot(it.strokes, it.preview, it.preview == null) }.toMutableList()
    private var pageWidth = initial.pageWidth
    private var pageHeight = initial.pageHeight

    var template: PageTemplate = initial.template
        private set

    var pageIndex: Int = 0
        private set

    val pageCount: Int get() = pages.size

    private var canvas: InkCanvasView? = null
    private var unsaved = false
    private var collectedFrom: io.github.foxesrcool1.einklauncher.core.ink.InkPageEditor? = null
    private var collectedRevision = -1L
    private var shapeSettled = false

    private val main = Handler(Looper.getMainLooper())
    private val saver = Executors.newSingleThreadExecutor { Thread(it, "eink-ink-save").apply { isDaemon = true } }
    private val autosave = Runnable { save() }

    fun attach(view: InkCanvasView) {
        canvas = view
        view.onInkChanged = {
            pages[pageIndex].previewStale = true
            unsaved = true
            main.removeCallbacks(autosave)
            main.postDelayed(autosave, AUTOSAVE_IDLE_MILLIS)
        }
        view.onSized = { width, height -> adoptShape(width, height) }
        showCurrentPage()
        if (view.width > 0) adoptShape(view.width, view.height)
    }

    /**
     * A note with no ink yet takes the shape of the canvas it is first shown
     * on, so the page fills the screen instead of sitting in a box. Once there
     * is ink, the shape is part of the note and never changes again.
     */
    private fun adoptShape(viewWidth: Int, viewHeight: Int) {
        if (shapeSettled || viewWidth <= 0 || viewHeight <= 0) return
        shapeSettled = true
        if (pages.any { it.strokes.isNotEmpty() }) return
        val wanted = (pageWidth * viewHeight / viewWidth).toInt().toFloat()
        if (wanted == pageHeight || wanted < pageWidth / 2f) return
        pageHeight = wanted
        showCurrentPage()
    }

    private fun showCurrentPage() {
        val view = canvas ?: return
        view.setPage(pages[pageIndex].strokes, pageWidth, pageHeight, template)
        collectedFrom = view.editor
        collectedRevision = view.editor.revision
    }

    /** Takes the strokes back from the canvas into the page list. */
    private fun collect() {
        val view = canvas ?: return
        view.settle()
        // A fresh list only when something changed, so a save can tell by
        // identity whether the page was drawn on while it worked.
        val editor = view.editor
        if (editor === collectedFrom && editor.revision == collectedRevision) return
        collectedFrom = editor
        collectedRevision = editor.revision
        pages[pageIndex].strokes = editor.strokes
    }

    fun goTo(index: Int): Boolean {
        if (index !in pages.indices || index == pageIndex) return false
        collect()
        pageIndex = index
        showCurrentPage()
        return true
    }

    /** Adds an empty page after the one on screen and goes to it. */
    fun addPage() {
        collect()
        pages.add(pageIndex + 1, PageSlot(emptyList(), null, true))
        pageIndex++
        unsaved = true
        showCurrentPage()
        save()
    }

    /** Deletes the page on screen. The last page is emptied instead, so a note always has one. */
    fun deletePage() {
        collect()
        if (pages.size == 1) {
            pages[0] = PageSlot(emptyList(), null, true)
        } else {
            pages.removeAt(pageIndex)
            pageIndex = pageIndex.coerceAtMost(pages.size - 1)
        }
        unsaved = true
        showCurrentPage()
        save()
    }

    fun changeTemplate(next: PageTemplate) {
        if (next == template) return
        template = next
        pages.forEach { it.previewStale = true }
        unsaved = true
        canvas?.setTemplate(next)
        save()
    }

    /** The note as it stands, for an export. */
    fun snapshot(): InkNote {
        collect()
        return InkNote(pageWidth, pageHeight, template, pages.map { InkPageData(it.strokes, it.preview) })
    }

    fun save() {
        main.removeCallbacks(autosave)
        if (!unsaved) return
        if (!mayWrite) {
            AppLog.w(TAG, "Not saving $path: the file on disk could not be read and must not be lost")
            return
        }
        collect()
        unsaved = false

        val template = template
        val frozen = pages.map { Triple(it, it.strokes, it.previewStale) }
        val width = pageWidth
        val height = pageHeight

        saver.execute {
            runCatching {
                val started = System.currentTimeMillis()
                val data = frozen.map { (slot, strokes, stale) ->
                    val preview = if (stale || slot.preview == null) {
                        InkExport.pagePng(
                            InkNote(width, height, template),
                            InkPageData(strokes),
                            InkExport.PREVIEW_SCALE,
                        ).also { fresh ->
                            main.post {
                                // Only if nobody drew on the page in the meantime.
                                if (slot.strokes === strokes) {
                                    slot.preview = fresh
                                    slot.previewStale = false
                                }
                            }
                        }
                    } else {
                        slot.preview
                    }
                    InkPageData(strokes, preview)
                }
                val ok = repository.save(path, InkNote(width, height, template, data))
                val took = System.currentTimeMillis() - started
                if (!ok || took > 500) AppLog.i(TAG, "Saved $path: ok=$ok, ${data.size} pages, $took ms")
                if (!ok) main.post { unsaved = true }
            }.onFailure {
                AppLog.e(TAG, "Saving $path failed", it)
                // Still not on the disk, so the next save has to try again.
                main.post { unsaved = true }
            }
        }
    }

    /** Saves and waits a short while, for the moment the screen goes away. */
    fun saveAndWait() {
        save()
        runCatching {
            saver.submit { }.get(SAVE_WAIT_SECONDS, TimeUnit.SECONDS)
        }.onFailure { AppLog.w(TAG, "The last save of $path did not finish in time", it) }
    }

    fun close() {
        main.removeCallbacks(autosave)
        saver.shutdown()
    }

    private companion object {
        const val AUTOSAVE_IDLE_MILLIS = 2_500L
        const val SAVE_WAIT_SECONDS = 4L
    }
}
