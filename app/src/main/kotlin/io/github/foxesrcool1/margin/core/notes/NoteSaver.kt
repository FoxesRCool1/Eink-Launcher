package io.github.foxesrcool1.margin.core.notes

/**
 * Writes one note so that the newest text is what ends up in the file.
 *
 * A note is saved from two sides: the autosave, on a background thread, and
 * the last save as the screen goes away, on the main thread. Each write is
 * atomic, but two of them can cross, and if the older text lands last, the
 * last words the user typed are gone. So the writes take turns here, and each
 * one asks for the text at the moment its turn comes, not the text from when
 * it was asked to save.
 */
class NoteSaver(private val write: (String) -> Boolean) {

    private val lock = Any()

    /** What the file holds, as far as this saver knows. Null until [loaded] is called. */
    @Volatile
    var savedText: String? = null
        private set

    /** Call with what was read from the file, before the first save. */
    fun loaded(text: String) {
        savedText = text
    }

    /**
     * Writes what [current] gives when the write starts. True when the file
     * holds that text afterwards, which includes the case where it already did.
     */
    fun save(current: () -> String): Boolean = synchronized(lock) {
        val text = current()
        if (text == savedText) return true
        val written = write(text)
        if (written) savedText = text
        written
    }
}
