package io.github.foxesrcool1.margin.core.notes

/**
 * Reading facts out of a note.
 *
 * Plain Kotlin, so the word count rules are pinned down by tests rather than
 * by whatever a text field happened to report.
 */
object NoteText {

    /**
     * Words, counted the way a writer counts them.
     *
     * A run of any whitespace separates words. A hyphenated word is one word.
     * A Markdown heading marker on its own is not a word, because "# " in
     * front of a title should not make the count go up.
     */
    fun wordCount(text: String): Int =
        text.split(WHITESPACE)
            .count { token -> token.any { it.isLetterOrDigit() } }

    fun characterCount(text: String): Int = text.length

    /** Characters with the whitespace taken out, which is what a typographer counts. */
    fun characterCountWithoutSpaces(text: String): Int = text.count { !it.isWhitespace() }

    /**
     * A title for a note.
     *
     * The first Markdown heading wins. Failing that, the first line with
     * something on it. Failing that, the file name.
     */
    fun titleFrom(text: String, fallback: String): String {
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val heading = line.dropWhile { it == '#' }.trim()
            return if (line.startsWith("#") && heading.isNotEmpty()) {
                heading.take(MAX_TITLE)
            } else {
                line.take(MAX_TITLE)
            }
        }
        return fallback
    }

    /** One line of the note, for a row in the browser. */
    fun preview(text: String, maxCharacters: Int = 90): String {
        val flattened = text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .drop(1)
            .joinToString(" ")
            .replace(WHITESPACE, " ")
            .trim()

        return if (flattened.length <= maxCharacters) {
            flattened
        } else {
            flattened.take(maxCharacters).trimEnd() + "..."
        }
    }

    private const val MAX_TITLE = 80
    private val WHITESPACE = Regex("\\s+")
}
