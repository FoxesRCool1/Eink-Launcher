package io.github.foxesrcool1.einklauncher.core.storage

/**
 * Path rules for the data folder.
 *
 * No Android here, so the tests can check every way a path could escape the
 * root: `..`, a leading slash, a Windows drive letter, a backslash, an empty
 * segment, a name that is only dots.
 */
object RelativePaths {

    /**
     * Cleans a relative path, or throws.
     *
     * Returns "" for the root itself. The result never starts or ends with a
     * separator and never holds an empty or dotted segment.
     */
    fun normalise(path: String): String {
        val unified = path.replace('\\', '/')

        if (unified.startsWith("/")) throw UnsafePathException(path)
        if (unified.length >= 2 && unified[1] == ':') throw UnsafePathException(path)

        val parts = unified.split('/').filter { it.isNotEmpty() }
        parts.forEach { part ->
            if (part == "." || part == "..") throw UnsafePathException(path)
            if (part.trim() != part) throw UnsafePathException(path)
        }
        return parts.joinToString("/")
    }

    /** True when [path] can be used. Never throws. */
    fun isSafe(path: String): Boolean = runCatching { normalise(path) }.isSuccess

    fun join(parent: String, child: String): String {
        val cleanParent = normalise(parent)
        val cleanChild = normalise(child)
        return when {
            cleanParent.isEmpty() -> cleanChild
            cleanChild.isEmpty() -> cleanParent
            else -> "$cleanParent/$cleanChild"
        }
    }

    fun parentOf(path: String): String {
        val clean = normalise(path)
        val cut = clean.lastIndexOf('/')
        return if (cut < 0) "" else clean.substring(0, cut)
    }

    fun nameOf(path: String): String = normalise(path).substringAfterLast('/')

    fun extensionOf(path: String): String = nameOf(path).substringAfterLast('.', "")
}
