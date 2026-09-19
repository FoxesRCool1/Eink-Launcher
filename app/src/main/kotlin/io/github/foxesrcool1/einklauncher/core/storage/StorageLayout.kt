package io.github.foxesrcool1.einklauncher.core.storage

import java.time.LocalDate
import java.util.Locale

/**
 * Where everything lives inside the data folder.
 *
 * Plain files are the source of truth. The layout is part of the promise to
 * the user: they can open the folder in a file manager, sync it, and read
 * every file with something else. Nothing here touches Android, so the tests
 * can check every path rule.
 *
 * ```
 * EinkLauncher/
 *   books/          imported EPUB and PDF copies
 *   annotations/    <book-id>.json, <book-id>/page-0001.strokes
 *   notes/          user folders, *.md typed, *.inknote handwritten
 *   journal/        YYYY/YYYY-MM-DD.md, YYYY/YYYY-MM-DD.inknote
 *   habits/         habits.json, log.csv
 *   exports/
 *   logs/
 * ```
 */
object StorageLayout {

    const val ROOT_FOLDER = "EinkLauncher"

    const val BOOKS = "books"
    const val ANNOTATIONS = "annotations"
    const val NOTES = "notes"
    const val JOURNAL = "journal"
    const val HABITS = "habits"
    const val EXPORTS = "exports"
    const val LOGS = "logs"

    /** Created on first run, so the folder reads as a finished thing. */
    val topLevelFolders: List<String> =
        listOf(BOOKS, ANNOTATIONS, NOTES, JOURNAL, HABITS, EXPORTS, LOGS)

    /** These go into a backup. Exports and logs are left out: both can be made again. */
    val backedUpFolders: List<String> =
        listOf(BOOKS, ANNOTATIONS, NOTES, JOURNAL, HABITS)

    const val TYPED_NOTE_EXTENSION = "md"
    const val INK_NOTE_EXTENSION = "inknote"

    fun bookPath(fileName: String): String = "$BOOKS/${safeName(fileName)}"

    fun annotationsPath(bookId: String): String = "$ANNOTATIONS/${safeName(bookId)}.json"

    /** One stroke file per page. Page numbers start at 1 and are padded to four digits. */
    fun strokesPath(bookId: String, pageNumber: Int): String {
        require(pageNumber >= 1) { "page numbers start at 1" }
        val padded = pageNumber.toString().padStart(4, '0')
        return "$ANNOTATIONS/${safeName(bookId)}/page-$padded.strokes"
    }

    fun journalFolder(date: LocalDate): String = "$JOURNAL/${date.year}"

    fun journalPath(date: LocalDate, handwritten: Boolean = false): String {
        val extension = if (handwritten) INK_NOTE_EXTENSION else TYPED_NOTE_EXTENSION
        return "${journalFolder(date)}/$date.$extension"
    }

    fun habitsPath(): String = "$HABITS/habits.json"

    fun habitLogPath(): String = "$HABITS/log.csv"

    /**
     * Turns anything into a name that is safe on every file system the folder
     * may end up on, including FAT32 memory cards and a synced Windows folder.
     *
     * The rules: keep letters, digits, space, dot, dash and underscore. Turn
     * everything else into a dash. Never end with a dot or a space, because
     * Windows refuses those. Never return an empty name.
     */
    fun safeName(raw: String): String {
        val cleaned = buildString {
            raw.trim().forEach { character ->
                when {
                    character.isLetterOrDigit() -> append(character)
                    character in ALLOWED_PUNCTUATION -> append(character)
                    else -> append('-')
                }
            }
        }
            .replace(Regex("-{2,}"), "-")
            .trim(' ', '.', '-')
            .take(MAX_NAME_LENGTH)
            .trim(' ', '.', '-')

        return cleaned.ifBlank { "untitled" }
    }

    /**
     * A stable id for an imported book.
     *
     * The name alone is not enough: two files can share a name. The size makes
     * a collision unlikely, and both parts survive a copy to another device,
     * which a random id would not.
     */
    fun bookIdFor(fileName: String, sizeBytes: Long): String {
        val stem = fileName.substringBeforeLast('.', fileName)
        val slug = safeName(stem)
            .lowercase(Locale.ROOT)
            .replace(' ', '-')
            .replace('.', '-')
            .replace('_', '-')
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .ifBlank { "book" }
        return "$slug-$sizeBytes"
    }

    /** True when the file is something the Reading tab can open. */
    fun isBook(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("epub", "pdf")

    fun isTypedNote(fileName: String): Boolean =
        fileName.endsWith(".$TYPED_NOTE_EXTENSION", ignoreCase = true)

    fun isInkNote(fileName: String): Boolean =
        fileName.endsWith(".$INK_NOTE_EXTENSION", ignoreCase = true)

    private const val MAX_NAME_LENGTH = 120
    private val ALLOWED_PUNCTUATION = setOf(' ', '.', '-', '_')
}
