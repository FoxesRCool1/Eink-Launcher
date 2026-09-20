package io.github.foxesrcool1.einklauncher.ui.writing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import io.github.foxesrcool1.einklauncher.design.components.EinkTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.threads.AppDispatchers
import io.github.foxesrcool1.einklauncher.core.notes.NoteSaver
import io.github.foxesrcool1.einklauncher.core.notes.NoteText
import io.github.foxesrcool1.einklauncher.core.notes.NotesRepository
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.storage.RelativePaths
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.einkFieldBorder
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "NoteEditorScreen"
private const val AUTOSAVE_QUIET_MILLIS = 1_500L

/** One open typed note: what it holds now, and whether it may be typed into yet. */
class NoteEditor(
    val text: String,
    val loaded: Boolean,
    val status: String,
    val onText: (String) -> Unit,
)

/**
 * Opens one typed note and keeps it saved.
 *
 * It saves itself once the typing stops for a moment and again when the screen
 * goes away, so a note is never lost to a Home key press. The save goes
 * through the storage layer, which writes to a temporary file and renames, so
 * a tablet that runs out of battery keeps the last good version.
 *
 * The full screen editor and the note beside a book in the split screen both
 * stand on this, so there is one place where a note can be lost, and not two.
 *
 * [textWhenNew] is what a note that does not exist yet starts with. Nothing is
 * written until the user types: opening the split screen and closing it again
 * must not leave an empty file behind.
 */
@Composable
fun rememberNoteEditor(
    notePath: String,
    saveRequests: Int = 0,
    textWhenNew: String = "",
): NoteEditor {
    val context = LocalContext.current
    val notes = remember(context) { NotesRepository(DataRoot.repository(context)) }

    var text by remember(notePath) { mutableStateOf("") }
    var savedText by remember(notePath) { mutableStateOf("") }
    var loaded by remember(notePath) { mutableStateOf(false) }
    var status by remember(notePath) { mutableStateOf("Opening") }

    // Every write of this note goes through here, so the autosave and the
    // save on the way out cannot cross. See NoteSaver.
    val saver = remember(notePath) { NoteSaver { notes.write(notePath, it) } }

    suspend fun save(reason: String) {
        if (text == savedText) return
        val written = withContext(AppDispatchers.io) { saver.save { text } }
        if (written) {
            savedText = saver.savedText ?: savedText
            status = "Saved"
            AppLog.i(TAG, "Saved $notePath ($reason)")
        } else {
            status = "Could not save"
            AppLog.e(TAG, "Could not save $notePath ($reason)")
        }
    }

    LaunchedEffect(notePath) {
        val loadedText = withContext(AppDispatchers.io) { notes.readOrNull(notePath) }
        if (loadedText == null) {
            // The field stays off. Typing into an empty page here would end
            // with that page saved over a note that is still on the disk.
            status = "This note could not be read"
            AppLog.e(TAG, "Could not read $notePath. The editor stays closed to typing.")
            return@LaunchedEffect
        }
        val start = loadedText.ifEmpty { textWhenNew }
        text = start
        savedText = start
        saver.loaded(start)
        loaded = true
        status = "Ready"
    }

    // Autosave once the typing stops. Every keystroke cancels this and starts
    // it again, so the disk is touched once at the end of a sentence and not
    // once per letter.
    LaunchedEffect(text, loaded) {
        if (!loaded) return@LaunchedEffect
        if (text == savedText) return@LaunchedEffect
        status = "Not saved yet"
        delay(AUTOSAVE_QUIET_MILLIS)
        save("autosave")
    }

    LaunchedEffect(saveRequests) {
        if (saveRequests > 0 && loaded) save("keyboard")
    }

    // The last chance to write, and it does not wait for a thread. A writing
    // app that loses the last sentence is not usable.
    fun saveOnTheWayOut(reason: String) {
        if (!loaded || text == saver.savedText) return
        runCatching { saver.save { text } }
            .onSuccess { written ->
                if (written) savedText = saver.savedText ?: savedText
                AppLog.i(TAG, "Saved $notePath ($reason): $written")
            }
            .onFailure { AppLog.e(TAG, "Lost the last edit of $notePath ($reason)", it) }
    }

    // "Done", Back and Escape close the screen, and this runs.
    DisposableEffect(notePath) {
        onDispose { saveOnTheWayOut("the editor closed") }
    }
    // The Home key does not close the screen, it only stops it, and Android
    // may end a stopped app without another word. So this is the save that
    // covers the Home key. The one above never runs for it.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        saveOnTheWayOut("the editor went to the back")
    }

    return NoteEditor(text = text, loaded = loaded, status = status, onText = { text = it })
}

/** The full screen editor: one note, and nothing else. */
@Composable
fun NoteEditorScreen(
    notePath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    saveRequests: Int = 0,
) {
    val editor = rememberNoteEditor(notePath, saveRequests)

    ScreenScaffold(
        title = NoteText.titleFrom(editor.text, RelativePaths.nameOf(notePath)),
        overline = RelativePaths.parentOf(notePath),
        modifier = modifier,
        onBack = onClose,
        backIcon = Lucide.ArrowLeft,
        backLabel = "Done",
    ) {
        NoteField(editor = editor, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

/** The box the note is typed in, with the word count and the save state under it. */
@Composable
fun NoteField(editor: NoteEditor, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        EinkTextField(
            value = editor.text,
            onValueChange = editor.onText,
            textStyle = EinkType.reading,
            enabled = editor.loaded,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .einkFieldBorder(EinkColors.Faded)
                .padding(14.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        CapsLabel(
            text = "${NoteText.wordCount(editor.text)} words  .  ${editor.status}",
            style = EinkType.capsSmall.copy(color = EinkColors.Faded),
        )
    }
}
