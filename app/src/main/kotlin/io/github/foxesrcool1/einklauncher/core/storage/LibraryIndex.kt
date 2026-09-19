package io.github.foxesrcool1.einklauncher.core.storage

import java.util.Locale

/** What kind of thing an index row points at. */
enum class IndexKind {
    Book,
    TypedNote,
    InkNote,
    JournalEntry,
    Other,
}

/** One row of the index. */
data class IndexEntry(
    val kind: IndexKind,
    val relativePath: String,
    val title: String,
    val lastModified: Long,
    val sizeBytes: Long,
)

/** The whole index at one moment. */
data class IndexSnapshot(
    val entries: List<IndexEntry>,
    val builtAtMillis: Long,
) {
    fun of(kind: IndexKind): List<IndexEntry> = entries.filter { it.kind == kind }

    fun byRecent(kind: IndexKind): List<IndexEntry> =
        of(kind).sortedByDescending { it.lastModified }

    fun byTitle(kind: IndexKind): List<IndexEntry> =
        of(kind).sortedBy { it.title.lowercase(Locale.ROOT) }
}

/**
 * A list of what is in the data folder.
 *
 * The files are the truth. This is only a faster way to ask what is there, and
 * it can always be thrown away and built again from the folder, which is why
 * it is kept outside the user's folder.
 *
 * It is written as lines rather than JSON on purpose: the format needs no
 * library, it reads the same in a text editor, and a damaged line can be
 * dropped without losing the rest.
 */
object LibraryIndex {

    private const val FORMAT_LINE = "eink-index-1"
    private const val SEPARATOR = '\u001f'

    /** Walks the data folder and works out what every file is. */
    fun build(store: FileStore, nowMillis: Long): IndexSnapshot {
        val entries = mutableListOf<IndexEntry>()

        store.listFilesRecursively(StorageLayout.BOOKS)
            .filter { StorageLayout.isBook(it.name) }
            .forEach { entries += it.toEntry(IndexKind.Book) }

        store.listFilesRecursively(StorageLayout.NOTES).forEach { file ->
            when {
                StorageLayout.isTypedNote(file.name) -> entries += file.toEntry(IndexKind.TypedNote)
                StorageLayout.isInkNote(file.name) -> entries += file.toEntry(IndexKind.InkNote)
                else -> entries += file.toEntry(IndexKind.Other)
            }
        }

        store.listFilesRecursively(StorageLayout.JOURNAL)
            .filter { StorageLayout.isTypedNote(it.name) || StorageLayout.isInkNote(it.name) }
            .forEach { entries += it.toEntry(IndexKind.JournalEntry) }

        return IndexSnapshot(
            entries = entries.sortedBy { it.relativePath.lowercase(Locale.ROOT) },
            builtAtMillis = nowMillis,
        )
    }

    private fun StoredEntry.toEntry(kind: IndexKind): IndexEntry = IndexEntry(
        kind = kind,
        relativePath = relativePath,
        title = name.substringBeforeLast('.', name),
        lastModified = lastModified,
        sizeBytes = sizeBytes,
    )

    fun serialise(snapshot: IndexSnapshot): String = buildString {
        append(FORMAT_LINE).append('\n')
        append(snapshot.builtAtMillis).append('\n')
        snapshot.entries.forEach { entry ->
            append(entry.kind.name).append(SEPARATOR)
            append(entry.relativePath).append(SEPARATOR)
            append(entry.lastModified).append(SEPARATOR)
            append(entry.sizeBytes).append(SEPARATOR)
            append(entry.title.replace('\n', ' ').replace(SEPARATOR, ' '))
            append('\n')
        }
    }

    /**
     * Reads an index back. Returns null when the text is not an index at all.
     * A single damaged line is dropped; the rest of the file still loads.
     */
    fun deserialise(text: String): IndexSnapshot? {
        val lines = text.lineSequence().iterator()
        if (!lines.hasNext() || lines.next().trim() != FORMAT_LINE) return null
        if (!lines.hasNext()) return null
        val builtAt = lines.next().trim().toLongOrNull() ?: return null

        val entries = mutableListOf<IndexEntry>()
        while (lines.hasNext()) {
            val line = lines.next()
            if (line.isBlank()) continue
            val parts = line.split(SEPARATOR)
            if (parts.size != 5) continue
            val kind = runCatching { IndexKind.valueOf(parts[0]) }.getOrNull() ?: continue
            val lastModified = parts[2].toLongOrNull() ?: continue
            val size = parts[3].toLongOrNull() ?: continue
            if (!RelativePaths.isSafe(parts[1])) continue
            entries += IndexEntry(kind, parts[1], parts[4], lastModified, size)
        }
        return IndexSnapshot(entries, builtAt)
    }
}
