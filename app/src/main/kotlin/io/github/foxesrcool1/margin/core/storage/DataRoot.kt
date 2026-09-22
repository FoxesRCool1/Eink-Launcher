package io.github.foxesrcool1.margin.core.storage

import android.content.Context
import io.github.foxesrcool1.margin.core.log.AppLog
import java.io.File

private const val TAG = "DataRoot"

/**
 * Where the data folder lives.
 *
 * Today it is `Android/data/<package>/files/Margin` on the memory card,
 * which needs no permission and no picker. The user can see it with a file
 * manager on the tablet.
 *
 * That is not the final answer. A folder the user picks themselves, through
 * the Storage Access Framework, would also be reachable by a sync program, and
 * this one is not on Android 11 and later. The choice needs a measurement on
 * the tablet first. See `docs/decisions/0005-storage-layer.md`.
 */
object DataRoot {

    @Volatile
    private var cached: DataRepository? = null

    fun folder(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, StorageLayout.ROOT_FOLDER)
    }

    fun repository(context: Context): DataRepository {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: DataRepository(LocalFileStore(folder(context.applicationContext)))
                .also { cached = it }
        }
    }

    /** Called once at start-up, off the main thread. */
    fun prepare(context: Context) {
        runCatching {
            val repository = repository(context)
            repository.ensureFolders()
            AppLog.i(TAG, "Data folder ready at ${repository.store.displayPath}")
        }.onFailure {
            AppLog.e(TAG, "Could not prepare the data folder", it)
        }
    }
}
