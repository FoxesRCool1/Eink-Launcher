package io.github.foxesrcool1.einklauncher.ui.writing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.design.EinkTheme

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
        setContent {
            EinkTheme {
                NoteEditorScreen(
                    notePath = path,
                    onClose = { finish() },
                    saveRequests = saveRequests.intValue,
                )
            }
        }
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
