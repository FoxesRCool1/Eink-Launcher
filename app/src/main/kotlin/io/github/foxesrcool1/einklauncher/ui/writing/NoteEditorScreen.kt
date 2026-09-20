package io.github.foxesrcool1.einklauncher.ui.writing

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.notes.NoteSaver
import io.github.foxesrcool1.einklauncher.core.notes.NoteText
import io.github.foxesrcool1.einklauncher.core.notes.NotesRepository
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.storage.RelativePaths
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "NoteEditorScreen"
private const val AUTOSAVE_QUIET_MILLIS = 1_500L

/**
 * Writing one note.
 *
 * It saves itself once the typing stops for a moment and again when the screen
 * goes away, so a note is never lost to a Home key press. The save goes
 * through the storage layer, which writes to a temporary file and renames, so
 * a tablet that runs out of battery keeps the last good version.
 */
@Composable
fun NoteEditorScreen(
    notePath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    saveRequests: Int = 0,
) {
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
        val written = withContext(Dispatchers.IO) { saver.save { text } }
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
        val loadedText = withContext(Dispatchers.IO) { notes.readOrNull(notePath) }
        if (loadedText == null) {
            // The field stays off. Typing into an empty page here would end
            // with that page saved over a note that is still on the disk.
            status = "This note could not be read"
            AppLog.e(TAG, "Could not read $notePath. The editor stays closed to typing.")
            return@LaunchedEffect
        }
        text = loadedText
        savedText = loadedText
        saver.loaded(loadedText)
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

    ScreenScaffold(
        title = NoteText.titleFrom(text, RelativePaths.nameOf(notePath)),
        overline = RelativePaths.parentOf(notePath),
        corner = null,
        modifier = modifier,
    ) {
        EinkTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = EinkType.reading,
            enabled = loaded,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(EinkDimens.hairline, EinkColors.Faded)
                .padding(12.dp),
        )

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CapsLabel(
                text = "${NoteText.wordCount(text)} words  .  $status",
                style = EinkType.capsSmall.copy(color = EinkColors.Faded),
            )
            InvertPressButton(text = "Done", onClick = onClose, bordered = false)
        }
    }
}

