package io.github.foxesrcool1.einklauncher.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialog
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val TAG = "StorageSection"

/**
 * The data folder part of Settings: where the files are, backup, restore, and
 * a rebuild of the index.
 *
 * A restore overwrites what is on the tablet, so it asks first. Both jobs run
 * off the main thread: a backup with a few books in it is not quick.
 */
@Composable
fun StorageSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { DataRoot.repository(context) }

    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) {
            status = "Backup cancelled"
            return@rememberLauncherForActivityResult
        }
        busy = true
        status = "Writing the backup"
        scope.launch {
            status = withContext(Dispatchers.IO) { writeBackup(context, uri) }
            busy = false
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            status = "Restore cancelled"
        } else {
            pendingRestore = uri
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CapsLabel(text = "Data folder", style = EinkType.capsSmall)
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(10.dp))

        EinkText(
            text = repository.store.displayPath,
            style = EinkType.body.copy(color = EinkColors.Faded),
        )
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))

        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(
                text = "Back up",
                enabled = !busy,
                onClick = {
                    val name = "eink-launcher-${LocalDate.now()}.zip"
                    runCatching { backupPicker.launch(name) }
                        .onFailure {
                            AppLog.e(TAG, "No app could take the backup file", it)
                            status = "This tablet has no file picker for saving"
                        }
                },
            )
            InvertPressButton(
                text = "Restore",
                enabled = !busy,
                onClick = {
                    runCatching { restorePicker.launch(arrayOf("application/zip")) }
                        .onFailure {
                            AppLog.e(TAG, "No app could open a file", it)
                            status = "This tablet has no file picker for opening"
                        }
                },
            )
            InvertPressButton(
                text = "Rebuild index",
                enabled = !busy,
                onClick = {
                    busy = true
                    status = "Reading the data folder"
                    scope.launch {
                        val snapshot = withContext(Dispatchers.IO) { repository.buildIndex() }
                        AppLog.i(TAG, "Index rebuilt with ${snapshot.entries.size} entries")
                        status = "Index holds ${snapshot.entries.size} files"
                        busy = false
                    }
                },
            )
        }

        if (status != null) {
            Spacer(modifier = Modifier.height(10.dp))
            EinkText(
                text = status.orEmpty(),
                style = EinkType.body.copy(color = EinkColors.Faded),
            )
        }
    }

    val restoreUri = pendingRestore
    if (restoreUri != null) {
        ConfirmDialog(
            title = "Restore from a backup?",
            message = "Every file in the backup replaces the one on this tablet with " +
                "the same name. Files that are not in the backup stay where they are.",
            confirmText = "Restore",
            cancelText = "Cancel",
            onConfirm = {
                pendingRestore = null
                busy = true
                status = "Reading the backup"
                scope.launch {
                    status = withContext(Dispatchers.IO) { readBackup(context, restoreUri) }
                    busy = false
                }
            },
            onDismiss = {
                pendingRestore = null
                status = "Restore cancelled"
            },
        )
    }
}

private fun writeBackup(context: Context, uri: Uri): String {
    val repository = DataRoot.repository(context)
    return runCatching {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: return "The file could not be opened for writing"
        stream.use { output ->
            val result = repository.backupTo(output)
            "Backed up ${result.fileCount} files"
        }
    }.getOrElse { error ->
        AppLog.e(TAG, "Backup failed", error)
        "Backup failed: ${error.message}"
    }
}

private fun readBackup(context: Context, uri: Uri): String {
    val repository = DataRoot.repository(context)
    return runCatching {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return "The file could not be opened for reading"
        stream.use { input ->
            val result = repository.restoreFrom(input)
            if (result.refused.isEmpty()) {
                "Restored ${result.fileCount} files"
            } else {
                "Restored ${result.fileCount} files. ${result.refused.size} were refused."
            }
        }
    }.getOrElse { error ->
        AppLog.e(TAG, "Restore failed", error)
        "Restore failed: ${error.message}"
    }
}
