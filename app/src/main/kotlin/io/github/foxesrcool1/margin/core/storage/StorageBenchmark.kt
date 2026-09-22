package io.github.foxesrcool1.margin.core.storage

/** How long each part of the test took, in milliseconds. */
data class BenchmarkResult(
    val fileCount: Int,
    val writeMillis: Long,
    val listMillis: Long,
    val readMillis: Long,
    val deleteMillis: Long,
) {
    val totalMillis: Long get() = writeMillis + listMillis + readMillis + deleteMillis

    fun asLines(): List<String> = listOf(
        "Files: $fileCount",
        "Write: $writeMillis ms",
        "List: $listMillis ms",
        "Read: $readMillis ms",
        "Delete: $deleteMillis ms",
        "Total: $totalMillis ms",
    )
}

/**
 * Times a [FileStore] with a lot of small files.
 *
 * Plan step 4 asks for a measurement with 500 files before choosing between
 * the Storage Access Framework and a plain folder. That measurement has to
 * happen on the tablet, not on a computer: SAF goes through a content
 * provider, and the cost of that shows up on the device and nowhere else.
 *
 * The Dev screen runs this. It cleans up after itself.
 */
object StorageBenchmark {

    private const val FOLDER = "benchmark-scratch"

    fun run(store: FileStore, fileCount: Int = 500): BenchmarkResult {
        store.delete(FOLDER)
        store.createDirectories(FOLDER)

        val payload = ByteArray(2048) { (it % 251).toByte() }

        val writeMillis = timed {
            repeat(fileCount) { index ->
                store.write("$FOLDER/file-${index.toString().padStart(4, '0')}.bin", payload)
            }
        }

        var listed: List<StoredEntry> = emptyList()
        val listMillis = timed { listed = store.listFilesRecursively(FOLDER) }

        val readMillis = timed {
            listed.forEach { entry -> store.readBytes(entry.relativePath) }
        }

        val deleteMillis = timed { store.delete(FOLDER) }

        return BenchmarkResult(
            fileCount = listed.size,
            writeMillis = writeMillis,
            listMillis = listMillis,
            readMillis = readMillis,
            deleteMillis = deleteMillis,
        )
    }

    private inline fun timed(body: () -> Unit): Long {
        val start = System.nanoTime()
        body()
        return (System.nanoTime() - start) / 1_000_000
    }
}
