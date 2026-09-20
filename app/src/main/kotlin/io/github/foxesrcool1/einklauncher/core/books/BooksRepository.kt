package io.github.foxesrcool1.einklauncher.core.books

import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRepository
import io.github.foxesrcool1.einklauncher.core.storage.StorageLayout
import io.github.foxesrcool1.einklauncher.core.storage.StoredEntry
import java.io.InputStream
import java.util.Locale

private const val TAG = "BooksRepository"

/** One book as the library sees it. */
data class LibraryBook(
    val stored: StoredEntry,
    val metadata: BookMetadata,
    val bookId: String,
) {
    val path: String get() = stored.relativePath
    val isPdf: Boolean
        get() = stored.name.substringAfterLast('.', "").lowercase(Locale.ROOT) == "pdf"

    /** "12 MB", so the owner can see which file is eating the card. */
    fun sizeLabel(): String {
        val megabytes = stored.sizeBytes / (1024.0 * 1024.0)
        return if (megabytes < 1.0) {
            "${(stored.sizeBytes / 1024).coerceAtLeast(1)} KB"
        } else {
            "${"%.1f".format(megabytes)} MB"
        }
    }
}

/**
 * The library, on top of the data folder.
 *
 * It reads the title and the author out of an EPUB itself. The reader in step
 * 7 will use the Readium toolkit, which reads far more than this, but a
 * library has to show a real title before any reader exists, and a list of
 * file names is not a library.
 */
class BooksRepository(private val data: DataRepository) {

    fun list(): List<LibraryBook> =
        data.listBooks().map { stored ->
            val metadata = if (EpubMetadata.looksLikeEpub(stored.name)) {
                EpubMetadata.read { data.store.openInput(stored.relativePath) }
                    ?: BookMetadata.fromFileName(stored.name)
            } else {
                BookMetadata.fromFileName(stored.name)
            }

            LibraryBook(
                stored = stored,
                metadata = metadata,
                bookId = StorageLayout.bookIdFor(stored.name, stored.sizeBytes),
            )
        }

    fun byTitle(): List<LibraryBook> =
        list().sortedBy { it.metadata.title.lowercase(Locale.ROOT) }

    fun byRecent(): List<LibraryBook> = list().sortedByDescending { it.stored.lastModified }

    /** Copies a picked file into `books/`. */
    fun import(input: InputStream, displayName: String, sizeHintBytes: Long = -1L) =
        data.importBook(input, displayName, sizeHintBytes)

    fun delete(book: LibraryBook): Boolean {
        AppLog.i(TAG, "Deleting ${book.path}")
        // The book first. If it will not go, the user is told "could not
        // delete", and then the highlights and the handwriting must still be
        // there. The other way round, a failed delete still ate the notes.
        if (!data.deleteBook(book.path)) {
            AppLog.e(TAG, "Could not delete ${book.path}. Its notes were left alone.")
            return false
        }
        // The notes the user made about it go too, or they become orphans that
        // nothing can ever open again.
        data.store.delete(StorageLayout.annotationsPath(book.bookId))
        data.store.delete("${StorageLayout.ANNOTATIONS}/${book.bookId}")
        return true
    }
}
