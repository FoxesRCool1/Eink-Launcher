package io.github.foxesrcool1.einklauncher.ui.writing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.notes.NoteEntry
import io.github.foxesrcool1.einklauncher.core.notes.NoteText
import io.github.foxesrcool1.einklauncher.core.notes.NotesRepository
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.storage.RelativePaths
import io.github.foxesrcool1.einklauncher.core.storage.StorageLayout
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialog
import io.github.foxesrcool1.einklauncher.design.components.DialogOption
import io.github.foxesrcool1.einklauncher.design.components.EinkRow
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.OptionsDialog
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.TextPromptDialog
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "WritingScreen"
private const val ROWS_PER_PAGE = 5

/**
 * The Writing tab: a file browser over the notes folder.
 *
 * Moving is done by marking something and then pressing Paste in the folder it
 * should land in. Typing a path on a tablet with no keyboard is worse than two
 * taps, and the pen cannot drag and drop on e-ink without an animation.
 *
 * The handwritten notebook is not here. It needs the ink engine, which is
 * step 5.
 */
@Composable
fun WritingScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    newNoteRequests: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes = remember(context) { NotesRepository(DataRoot.repository(context)) }

    var folder by remember { mutableStateOf(notes.rootPath) }
    var rows by remember { mutableStateOf<List<NoteEntry>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    var optionsFor by remember { mutableStateOf<NoteEntry?>(null) }
    var renaming by remember { mutableStateOf<NoteEntry?>(null) }
    var deleting by remember { mutableStateOf<NoteEntry?>(null) }
    var marked by remember { mutableStateOf<NoteEntry?>(null) }
    var addingNote by remember { mutableStateOf(false) }
    var addingFolder by remember { mutableStateOf(false) }

    LaunchedEffect(folder, refresh) {
        rows = withContext(Dispatchers.IO) { notes.list(folder) }
    }

    LaunchedEffect(newNoteRequests) {
        if (newNoteRequests > 0) addingNote = true
    }

    fun openNewNote(title: String) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { notes.createNote(folder, title) }
            refresh++
            if (path != null) {
                onOpenNote(path)
            } else {
                status = "The note could not be made"
            }
        }
    }

    ScreenScaffold(
        title = "Write",
        overline = folder.removePrefix("${StorageLayout.NOTES}/").ifBlank { "All notes" },
        corner = null,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(text = "New note", onClick = { addingNote = true })
            InvertPressButton(text = "New folder", onClick = { addingFolder = true })
            InvertPressButton(
                text = "Up",
                enabled = !notes.isRoot(folder),
                onClick = { folder = notes.parentOf(folder) },
            )
        }

        if (marked != null || status != null) {
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
            ) {
                val pending = marked
                if (pending != null) {
                    InvertPressButton(
                        text = "Paste here",
                        onClick = {
                            scope.launch {
                                val moved = withContext(Dispatchers.IO) {
                                    notes.move(pending.path, folder)
                                }
                                status = if (moved) {
                                    "Moved ${pending.name}"
                                } else {
                                    "That move is not possible"
                                }
                                marked = null
                                refresh++
                            }
                        },
                    )
                    InvertPressButton(
                        text = "Cancel move",
                        onClick = {
                            marked = null
                            status = null
                        },
                        bordered = false,
                    )
                }
                if (status != null) {
                    CapsLabel(
                        text = status.orEmpty(),
                        style = EinkType.capsSmall.copy(color = EinkColors.Faded),
                        maxLines = 2,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        HairlineDivider(color = EinkColors.Faded)

        PagedList(
            items = rows,
            pageSize = ROWS_PER_PAGE,
            emptyText = "No notes in here yet",
            modifier = Modifier.weight(1f),
        ) { _, row ->
            NoteRow(
                entry = row,
                marked = marked?.path == row.path,
                notes = notes,
                onOpen = {
                    if (row.isFolder) {
                        folder = row.path
                        status = null
                    } else if (StorageLayout.isTypedNote(row.name)) {
                        onOpenNote(row.path)
                    } else {
                        status = "${row.name} needs the ink editor, which is step 5"
                    }
                },
                onOptions = { optionsFor = row },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            InvertPressButton(text = "Today", onClick = onBack, bordered = false)
        }
    }

    val selected = optionsFor
    if (selected != null) {
        OptionsDialog(
            title = selected.title,
            onDismiss = { optionsFor = null },
            options = listOf(
                DialogOption(label = "Rename") {
                    renaming = selected
                    optionsFor = null
                },
                DialogOption(label = "Move") {
                    marked = selected
                    status = "Open a folder, then press Paste here"
                    optionsFor = null
                },
                DialogOption(label = "Delete") {
                    deleting = selected
                    optionsFor = null
                },
            ),
        )
    }

    if (addingNote) {
        TextPromptDialog(
            title = "New note",
            confirmText = "Write",
            onConfirm = { title ->
                addingNote = false
                openNewNote(title)
            },
            onDismiss = { addingNote = false },
        )
    }

    if (addingFolder) {
        TextPromptDialog(
            title = "New folder",
            confirmText = "Make",
            onConfirm = { name ->
                addingFolder = false
                scope.launch {
                    val made = withContext(Dispatchers.IO) { notes.createFolder(folder, name) }
                    if (!made) status = "The folder could not be made"
                    refresh++
                }
            },
            onDismiss = { addingFolder = false },
        )
    }

    val beingRenamed = renaming
    if (beingRenamed != null) {
        TextPromptDialog(
            title = "Rename",
            initialValue = beingRenamed.name.substringBeforeLast('.'),
            onConfirm = { name ->
                renaming = null
                scope.launch {
                    withContext(Dispatchers.IO) { notes.rename(beingRenamed.path, name) }
                    refresh++
                }
            },
            onDismiss = { renaming = null },
        )
    }

    val beingDeleted = deleting
    if (beingDeleted != null) {
        ConfirmDialog(
            title = "Delete ${beingDeleted.name}?",
            message = if (beingDeleted.isFolder) {
                "The folder and everything in it goes away and cannot come back."
            } else {
                "The file goes away and cannot come back."
            },
            confirmText = "Delete",
            cancelText = "Keep",
            onConfirm = {
                deleting = null
                scope.launch {
                    val gone = withContext(Dispatchers.IO) { notes.delete(beingDeleted.path) }
                    status = if (gone) "Deleted ${beingDeleted.name}" else "Could not delete it"
                    AppLog.i(TAG, "Delete of ${beingDeleted.path}: $gone")
                    if (marked?.path == beingDeleted.path) marked = null
                    refresh++
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun NoteRow(
    entry: NoteEntry,
    marked: Boolean,
    notes: NotesRepository,
    onOpen: () -> Unit,
    onOptions: () -> Unit,
) {
    var preview by remember(entry.path) { mutableStateOf("") }

    LaunchedEffect(entry.path) {
        if (!entry.isFolder && StorageLayout.isTypedNote(entry.name)) {
            preview = withContext(Dispatchers.IO) { NoteText.preview(notes.read(entry.path)) }
        }
    }

    EinkRow(onClick = onOpen, onLongClick = onOptions) { pressed ->
        val foreground = if (pressed) EinkColors.Paper else EinkColors.Ink
        val faded = if (pressed) EinkColors.Paper else EinkColors.Faded

        Column(modifier = Modifier.fillMaxWidth()) {
            EinkText(
                text = buildString {
                    if (entry.isFolder) append("/ ")
                    append(entry.title)
                    if (marked) append("   (moving)")
                },
                style = EinkType.rowTitle.copy(color = foreground),
                maxLines = 1,
            )
            val second = when {
                entry.isFolder -> "Folder"
                preview.isNotBlank() -> preview
                StorageLayout.isInkNote(entry.name) -> "Handwritten note"
                else -> RelativePaths.extensionOf(entry.name).uppercase()
            }
            CapsLabel(
                text = second,
                style = EinkType.capsSmall.copy(color = faded),
                maxLines = 1,
            )
        }
    }
    HairlineDivider(color = EinkColors.Faded)
}
