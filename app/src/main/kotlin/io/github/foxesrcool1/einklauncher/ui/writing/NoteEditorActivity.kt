package io.github.foxesrcool1.einklauncher.ui.writing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.github.foxesrcool1.einklauncher.core.books.LibraryBook
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.ui.ink.InkCanvasView
import io.github.foxesrcool1.einklauncher.ui.split.BesideActivity
import io.github.foxesrcool1.einklauncher.ui.split.PaneHost
import io.github.foxesrcool1.einklauncher.ui.split.PaneInk
import io.github.foxesrcool1.einklauncher.ui.split.PanePage
import io.github.foxesrcool1.einklauncher.ui.split.SplitLayout
import io.github.foxesrcool1.einklauncher.ui.split.SplitPane
import io.github.foxesrcool1.einklauncher.ui.split.SplitState
import io.github.foxesrcool1.einklauncher.ui.split.openBookBeside
import io.github.foxesrcool1.einklauncher.ui.split.splitCarry
import io.github.foxesrcool1.einklauncher.ui.split.watchAndroidSplit

private const val TAG = "NoteEditorActivity"

/**
 * The typed note editor.
 *
 * Its own activity, as the plan says. If it lived inside the launcher task,
 * the Home key would close the note the user was in the middle of writing.
 *
 * A Bluetooth keyboard can press Escape to leave and Ctrl+S to save. Those are
 * handled here rather than in the composition, because an activity key handler
 * catches them wherever the focus happens to be.
 */
class NoteEditorActivity : ComponentActivity() {

    private val saveRequests = androidx.compose.runtime.mutableIntStateOf(0)
    private var notePath = ""
    private val split = SplitState(mainPage = { PanePage.TypedNote(notePath) })
    private lateinit var paneInk: PaneInk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        io.github.foxesrcool1.einklauncher.core.window.ScreenWindow.attach(this)

        val path = intent.getStringExtra(EXTRA_NOTE_PATH)
        if (path.isNullOrBlank()) {
            AppLog.e(TAG, "Opened with no note path")
            finish()
            return
        }

        AppLog.i(TAG, "Editing $path")
        notePath = path
        paneInk = PaneInk(this, lifecycleScope)
        watchAndroidSplit(split)
        intent.splitCarry()?.let { split.open(it.page, it.swapped) }

        val paneHost = object : PaneHost {
            override fun openBook(book: LibraryBook) = openBookBeside(this@NoteEditorActivity, split, book, this)

            override fun paneInk(canvas: InkCanvasView?) = paneInk.serve(canvas)

            override fun keeper(): Intent = BesideActivity.then(this@NoteEditorActivity, NoteEditorActivity.intent(this@NoteEditorActivity, path))

            override fun leave() = finish()
        }

        setContent {
            EinkTheme {
                SplitLayout(
                    split = split,
                    main = {
                        NoteEditorScreen(
                            notePath = path,
                            onClose = { finish() },
                            saveRequests = saveRequests.intValue,
                        )
                    },
                    pane = { SplitPane(split, paneHost) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::paneInk.isInitialized) paneInk.resume()
        io.github.foxesrcool1.einklauncher.ui.split.AdjacentApps.takeRequest(this)
    }

    override fun onPause() {
        if (::paneInk.isInitialized) paneInk.pause()
        super.onPause()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            keyCode == KeyEvent.KEYCODE_ESCAPE -> {
                finish()
                true
            }

            keyCode == KeyEvent.KEYCODE_S && event?.isCtrlPressed == true -> {
                saveRequests.intValue++
                true
            }

            else -> super.onKeyUp(keyCode, event)
        }
    }

    companion object {
        const val EXTRA_NOTE_PATH = "note_path"

        fun intent(context: Context, notePath: String): Intent =
            Intent(context, NoteEditorActivity::class.java)
                .putExtra(EXTRA_NOTE_PATH, notePath)
    }
}
