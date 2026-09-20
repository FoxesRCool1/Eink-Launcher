package io.github.foxesrcool1.einklauncher.core.apps

import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRepository
import io.github.foxesrcool1.einklauncher.core.storage.StorageLayout

private const val TAG = "AppFoldersRepository"

/**
 * The app folders, on top of the data folder.
 *
 * They are a plain file, `apps/folders.json`, and not a setting. The user
 * makes them by hand, one app at a time, and that work should be in a backup
 * and come back on a new tablet. The pinned apps are eight taps to make again,
 * so they stay where they were, in the settings.
 */
class AppFoldersRepository(private val data: DataRepository) {

    /**
     * Every change reads the file, changes it and writes it back, and each tap
     * runs on a thread of its own. The changes take turns, so two quick taps
     * cannot both start from the old file.
     */
    private companion object {
        val LOCK = Any()
    }

    fun load(): AppFolders {
        val text = data.store.readText(StorageLayout.appFoldersPath()) ?: return AppFolders()
        val parsed = AppFoldersFile.parse(text)
        if (parsed == null) {
            AppLog.w(TAG, "folders.json could not be read. Starting with no folders.")
            // The next change writes a new file over this one. Keep what the
            // user had, typo and all.
            data.keepUnreadable(StorageLayout.appFoldersPath(), "${StorageLayout.APPS}/folders.unreadable.json")
            return AppFolders()
        }
        return parsed
    }

    /** Loads, applies [transform], and saves when something changed. Returns what is on the disk now. */
    fun change(transform: (AppFolders) -> AppFolders): AppFolders = synchronized(LOCK) {
        val current = load()
        val next = transform(current)
        if (next == current) return current
        if (data.store.writeText(StorageLayout.appFoldersPath(), AppFoldersFile.serialise(next))) {
            next
        } else {
            AppLog.e(TAG, "Could not save the app folders")
            current
        }
    }
}
