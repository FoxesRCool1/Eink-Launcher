package io.github.foxesrcool1.einklauncher.core.apps

import java.util.Locale

/**
 * Finds an app by name as the user types. No Android in it, so a plain unit
 * test covers every rule.
 */
object AppSearch {

    /** Below this many letters, only the start of a word counts. */
    private const val ANYWHERE_FROM = 3

    /**
     * True when [query] is the start of the name or of a word in it. From
     * three letters on, anywhere in the name counts too. So "ca" finds
     * "Calculator" and "Google Calendar" but not "Scan", and "ind" finds
     * "Kindle". Capitals do not matter. An empty query matches everything.
     */
    fun matches(label: String, query: String): Boolean {
        val wanted = query.trim().lowercase(Locale.ROOT)
        if (wanted.isEmpty()) return true
        val name = label.lowercase(Locale.ROOT)
        if (name.startsWith(wanted)) return true
        if (name.split(' ', '-', '_', '.', '(', '/').any { it.startsWith(wanted) }) return true
        return wanted.length >= ANYWHERE_FROM && name.contains(wanted)
    }
}
