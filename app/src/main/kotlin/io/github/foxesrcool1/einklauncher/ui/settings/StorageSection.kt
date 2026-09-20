package io.github.foxesrcool1.einklauncher.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.threads.AppDispatchers
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val TAG = "StorageSection"

/**
 * What the "Backup and files" page of Settings can do. The page itself is
 * rows like every other page, so this holds no layout, only the work.
 */
class StorageActions(
    val folderPath: String,
    val busy: Boolean,
    val backUp: () -> Unit,
    val restore: () -> Unit,
    val rebuildIndex: () -> Unit,
    /** Not null while the app waits for a yes to a restore. Call it for the yes. */
    val confirmRestore: (() -> Unit)?,
    val cancelRestore: () -> Unit,
)

/**
 * Backup, restore, and a rebuild of the index.
 *
 * A restore overwrites what is on the tablet, so it asks first. Both jobs run
 * off the main thread: a backup with a few books in it is not quick.
 */
@Composable
fun rememberStorageActions(onStatus: (String) -> Unit): StorageActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { DataRoot.repository(context) }

    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) {
            onStatus("Backup cancelled")
            return@rememberLauncherForActivityResult
        }
        busy = true
        onStatus("Writing the backup")
        scope.launch {
            onStatus(withContext(AppDispatchers.io) { writeBackup(context, uri) })
            busy = false
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) onStatus("Restore cancelled") else pendingRestore = uri
    }

    val restoreUri = pendingRestore
    return StorageActions(
        folderPath = repository.store.displayPath,
        busy = busy,
        backUp = {
            val name = "eink-launcher-${LocalDate.now()}.zip"
            runCatching { backupPicker.launch(name) }
                .onFailure {
                    AppLog.e(TAG, "No app could take the backup file", it)
                    onStatus("This tablet has no file picker for saving")
                }
        },
        restore = {
            runCatching { restorePicker.launch(arrayOf("application/zip")) }
                .onFailure {
                    AppLog.e(TAG, "No app could open a file", it)
                    onStatus("This tablet has no file picker for opening")
                }
        },
        rebuildIndex = {
            busy = true
            onStatus("Reading the data folder")
            scope.launch {
                val snapshot = withContext(AppDispatchers.io) { repository.buildIndex() }
                AppLog.i(TAG, "Index rebuilt with ${snapshot.entries.size} entries")
                onStatus("Found ${snapshot.entries.size} files")
                busy = false
            }
        },
        confirmRestore = if (restoreUri == null) {
            null
        } else {
            {
                pendingRestore = null
                busy = true
                onStatus("Reading the backup")
                scope.launch {
                    onStatus(withContext(AppDispatchers.io) { readBackup(context, restoreUri) })
                    busy = false
                }
            }
        },
        cancelRestore = {
            pendingRestore = null
            onStatus("Restore cancelled")
        },
    )
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
