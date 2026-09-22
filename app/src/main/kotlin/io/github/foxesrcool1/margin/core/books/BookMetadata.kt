package io.github.foxesrcool1.margin.core.books

import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

/** What the library knows about a file before anything opens it. */
data class BookMetadata(
    val title: String,
    val author: String?,
) {
    companion object {
        fun fromFileName(fileName: String): BookMetadata =
            BookMetadata(title = fileName.substringBeforeLast('.', fileName), author = null)
    }
}

/**
 * Reads the title and the author out of an EPUB.
 *
 * An EPUB is a zip. `META-INF/container.xml` says which file inside it is the
 * package document, and that document holds `dc:title` and `dc:creator`.
 *
 * This is deliberately small. The reader in step 7 uses the Readium toolkit,
 * which reads far more than this, but the library list has to show a real
 * title before any reader exists, and a list of file names is not a library.
 *
 * It holds no Android class, so the tests build real EPUB files in a temporary
 * folder and read them back.
 */
object EpubMetadata {

    private const val CONTAINER_PATH = "META-INF/container.xml"
    private const val MAX_XML_BYTES = 2 * 1024 * 1024

    /** Returns null when the file is not an EPUB this can read. */
    fun read(open: () -> InputStream?): BookMetadata? = runCatching {
        val containerXml = readEntry(open, CONTAINER_PATH) ?: return null
        val packagePath = packagePathFrom(parse(containerXml) ?: return null) ?: return null
        val packageXml = readEntry(open, packagePath) ?: return null
        metadataFrom(parse(packageXml) ?: return null)
    }.getOrNull()

    /** Pulls one entry out of the zip by name. */
    private fun readEntry(open: () -> InputStream?, name: String): ByteArray? {
        val stream = open() ?: return null
        stream.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.equals(name, ignoreCase = true)) {
                        val bytes = zip.readBytes()
                        return if (bytes.size > MAX_XML_BYTES) {
                            bytes.copyOf(MAX_XML_BYTES)
                        } else {
                            bytes
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    /**
     * Parses XML with the door shut.
     *
     * A book is a file from somewhere else. An XML document can name an
     * external entity and make a naive parser read a file off the device or
     * open a network connection. Doctypes are refused outright, which is the
     * simplest way to make that impossible.
     */
    private fun parse(bytes: ByteArray): Document? = runCatching {
        // The doctype is refused here, by looking, and not by asking the
        // parser to refuse it. The parser on Android does not know the
        // feature names the desktop one does, and throws on every one of
        // them. Found on the emulator: the desktop tests passed and every
        // real book lost its title and author.
        if (declaresDoctype(bytes)) return@runCatching null

        val factory = DocumentBuilderFactory.newInstance()
        // Belt and braces where the platform has them. Each one alone, so a
        // parser that lacks one still gets the others.
        listOf(
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
        ).forEach { (feature, value) -> runCatching { factory.setFeature(feature, value) } }
        runCatching { factory.isXIncludeAware = false }
        runCatching { factory.isExpandEntityReferences = false }
        factory.isNamespaceAware = false
        factory.newDocumentBuilder().parse(bytes.inputStream())
    }.getOrNull()

    /** True when the document has a doctype anywhere in it, in any of the encodings an EPUB may use. */
    private fun declaresDoctype(bytes: ByteArray): Boolean {
        val marker = "<!DOCTYPE"
        return listOf(Charsets.UTF_8, Charsets.UTF_16LE, Charsets.UTF_16BE).any { charset ->
            String(bytes, charset).contains(marker, ignoreCase = true)
        }
    }

    private fun packagePathFrom(container: Document): String? {
        val rootFiles = container.getElementsByTagName("rootfile")
        for (index in 0 until rootFiles.length) {
            val element = rootFiles.item(index) as? Element ?: continue
            val path = element.getAttribute("full-path")
            if (path.isNotBlank()) return path.trimStart('/')
        }
        return null
    }

    private fun metadataFrom(packageDocument: Document): BookMetadata? {
        val title = firstText(packageDocument, "dc:title", "title") ?: return null
        val author = firstText(packageDocument, "dc:creator", "creator")
        return BookMetadata(title = title, author = author)
    }

    private fun firstText(document: Document, vararg tagNames: String): String? {
        tagNames.forEach { tag ->
            val nodes = document.getElementsByTagName(tag)
            for (index in 0 until nodes.length) {
                val text = nodes.item(index)?.textContent?.trim()
                if (!text.isNullOrBlank()) return text.take(200)
            }
        }
        return null
    }

    fun looksLikeEpub(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) == "epub"
}
