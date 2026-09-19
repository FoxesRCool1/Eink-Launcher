package io.github.foxesrcool1.einklauncher.core.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * A [FileStore] over plain `java.io.File`.
 *
 * It holds no Android class, so every rule in it is checked by a plain JUnit
 * test against a temporary folder. The only thing the app has to decide is
 * which folder to hand it.
 */
class LocalFileStore(private val root: File) : FileStore {

    init {
        root.mkdirs()
    }

    override val displayPath: String get() = root.absolutePath

    /** Turns a relative path into a real file, and refuses anything outside the root. */
    private fun resolve(relativePath: String): File {
        val clean = RelativePaths.normalise(relativePath)
        if (clean.isEmpty()) return root
        val target = File(root, clean)

        // A second check, in case a symbolic link points out of the folder.
        val rootPath = root.canonicalFile.path
        val targetPath = target.canonicalFile.path
        if (targetPath != rootPath && !targetPath.startsWith(rootPath + File.separator)) {
            throw UnsafePathException(relativePath)
        }
        return target
    }

    private fun relativeOf(file: File): String =
        file.canonicalFile.path
            .removePrefix(root.canonicalFile.path)
            .removePrefix(File.separator)
            .replace(File.separatorChar, '/')

    private fun entryOf(file: File): StoredEntry = StoredEntry(
        relativePath = relativeOf(file),
        name = file.name,
        isDirectory = file.isDirectory,
        sizeBytes = if (file.isDirectory) 0L else file.length(),
        lastModified = file.lastModified(),
    )

    override fun exists(relativePath: String): Boolean =
        runCatching { resolve(relativePath).exists() }.getOrDefault(false)

    override fun isDirectory(relativePath: String): Boolean =
        runCatching { resolve(relativePath).isDirectory }.getOrDefault(false)

    override fun sizeOf(relativePath: String): Long =
        runCatching { resolve(relativePath).let { if (it.isFile) it.length() else 0L } }
            .getOrDefault(0L)

    override fun lastModified(relativePath: String): Long =
        runCatching { resolve(relativePath).lastModified() }.getOrDefault(0L)

    override fun list(relativePath: String): List<StoredEntry> {
        val directory = runCatching { resolve(relativePath) }.getOrNull() ?: return emptyList()
        val children = directory.listFiles() ?: return emptyList()
        return children
            .map(::entryOf)
            .sortedWith(compareByDescending<StoredEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    override fun listFilesRecursively(relativePath: String): List<StoredEntry> {
        val start = runCatching { resolve(relativePath) }.getOrNull() ?: return emptyList()
        if (!start.exists()) return emptyList()

        val found = mutableListOf<StoredEntry>()
        // An explicit stack rather than recursion: a deep folder tree that a
        // sync program made should not overflow the stack.
        val pending = ArrayDeque<File>()
        pending.addLast(start)
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            val children = current.listFiles() ?: continue
            children.forEach { child ->
                if (child.isDirectory) pending.addLast(child) else found += entryOf(child)
            }
        }
        return found.sortedBy { it.relativePath.lowercase() }
    }

    override fun createDirectories(relativePath: String): Boolean = runCatching {
        val directory = resolve(relativePath)
        directory.isDirectory || directory.mkdirs()
    }.getOrDefault(false)

    override fun readBytes(relativePath: String): ByteArray? = runCatching {
        val file = resolve(relativePath)
        if (file.isFile) file.readBytes() else null
    }.getOrNull()

    override fun readText(relativePath: String): String? = runCatching {
        val file = resolve(relativePath)
        if (file.isFile) file.readText() else null
    }.getOrNull()

    override fun write(relativePath: String, bytes: ByteArray): Boolean =
        writeAtomically(relativePath) { output -> output.write(bytes) }

    override fun writeText(relativePath: String, text: String): Boolean =
        write(relativePath, text.toByteArray())

    override fun writeFrom(relativePath: String, input: InputStream): Boolean =
        writeAtomically(relativePath) { output -> input.copyTo(output) }

    /**
     * Writes through a temporary file in the same folder, then renames it over
     * the target. A rename inside one folder is atomic on every file system
     * this app will meet, so a reader sees either the old file or the new one
     * and never a half written one.
     */
    private fun writeAtomically(relativePath: String, body: (OutputStream) -> Unit): Boolean {
        return runCatching {
            val target = resolve(relativePath)
            target.parentFile?.mkdirs()

            val temporary = File(target.parentFile, "${target.name}.part-${System.nanoTime()}")
            try {
                temporary.outputStream().use { output ->
                    body(output)
                    output.flush()
                }
                if (target.exists() && !target.delete()) {
                    // Some file systems refuse a rename onto an existing file.
                    temporary.delete()
                    return@runCatching false
                }
                if (temporary.renameTo(target)) {
                    true
                } else {
                    // Last resort: copy the bytes across and drop the temporary.
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                    target.exists()
                }
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            }
        }.getOrDefault(false)
    }

    override fun openInput(relativePath: String): InputStream? = runCatching {
        val file = resolve(relativePath)
        if (file.isFile) file.inputStream() else null
    }.getOrNull()

    override fun delete(relativePath: String): Boolean = runCatching {
        val file = resolve(relativePath)
        if (!file.exists()) return@runCatching true
        // Never let a delete walk out of the data folder.
        if (file.canonicalFile == root.canonicalFile) return@runCatching false
        file.deleteRecursively()
    }.getOrDefault(false)

    override fun move(fromRelativePath: String, toRelativePath: String): Boolean = runCatching {
        val from = resolve(fromRelativePath)
        val to = resolve(toRelativePath)
        if (!from.exists()) return@runCatching false
        if (to.exists()) return@runCatching false
        to.parentFile?.mkdirs()
        if (from.renameTo(to)) return@runCatching true

        // A rename across two file systems fails. Copy, then remove.
        if (from.isDirectory) {
            from.copyRecursively(to, overwrite = false)
            from.deleteRecursively()
        } else {
            from.copyTo(to, overwrite = false)
            from.delete()
        }
        to.exists()
    }.getOrDefault(false)

    override fun copy(fromRelativePath: String, toRelativePath: String): Boolean = runCatching {
        val from = resolve(fromRelativePath)
        if (!from.isFile) return@runCatching false
        from.inputStream().use { input -> writeFrom(toRelativePath, input) }
    }.getOrDefault(false)

    override fun copyTo(relativePath: String, output: OutputStream): Boolean = runCatching {
        val file = resolve(relativePath)
        if (!file.isFile) return@runCatching false
        file.inputStream().use { input -> input.copyTo(output) }
        true
    }.getOrDefault(false)
}
