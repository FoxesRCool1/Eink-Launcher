package io.github.foxesrcool1.margin.core.update

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The answer to a GET that was read as text. Header names are lower case. */
data class HttpText(val code: Int, val body: String, val headers: Map<String, String>)

/** The two things the updater asks of the network. A test gives it a fake. */
interface UpdateHttp {

    fun getText(url: String, headers: Map<String, String>): HttpText

    /**
     * Writes the body to [target] and returns the HTTP code. The file is only
     * written when the code is 200. [onBytes] gets the running total.
     */
    fun download(url: String, headers: Map<String, String>, target: File, onBytes: (Long) -> Unit): Int
}

/**
 * The real network, on `HttpURLConnection`, so the updater adds no library.
 *
 * Redirects are followed by hand. GitHub answers a request for a release file
 * with a redirect to a storage server, and that address is signed. The access
 * token belongs to GitHub alone: the storage server refuses a request that
 * carries it, and no other host has any business seeing it. So the
 * `Authorization` header is dropped as soon as a redirect leaves the origin
 * it was meant for.
 */
class UrlConnectionHttp(
    /** Only a test turns this off, to talk to a server on the same machine. */
    private val requireHttps: Boolean = true,
) : UpdateHttp {

    override fun getText(url: String, headers: Map<String, String>): HttpText =
        request(url, headers) { connection, code ->
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val bytes = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    bytes.write(buffer, 0, read)
                    if (bytes.size() > MAX_TEXT_BYTES) throw IOException("The answer is too long")
                }
                bytes.toString(Charsets.UTF_8.name())
            }.orEmpty()
            val names = connection.headerFields.keys.filterNotNull()
            HttpText(code, body, names.associate { it.lowercase() to connection.getHeaderField(it).orEmpty() })
        }

    override fun download(
        url: String,
        headers: Map<String, String>,
        target: File,
        onBytes: (Long) -> Unit,
    ): Int = request(url, headers) { connection, code ->
        if (code == HttpURLConnection.HTTP_OK) {
            target.parentFile?.mkdirs()
            var total = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        onBytes(total)
                    }
                }
            }
        }
        code
    }

    private fun <T> request(
        url: String,
        headers: Map<String, String>,
        onAnswer: (HttpURLConnection, Int) -> T,
    ): T {
        var current = URL(url)
        var currentHeaders = headers

        for (hop in 0..MAX_REDIRECTS) {
            if (requireHttps && current.protocol != "https") {
                throw IOException("Refused an address that is not https: ${current.host}")
            }
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                requestMethod = "GET"
                currentHeaders.forEach { (name, value) -> setRequestProperty(name, value) }
            }
            try {
                val code = connection.responseCode
                if (code !in REDIRECT_CODES) return onAnswer(connection, code)

                val location = connection.getHeaderField("Location")
                    ?: throw IOException("A redirect with no address, HTTP $code")
                val next = URL(current, location)
                if (!sameOrigin(current, next)) {
                    currentHeaders = currentHeaders.filterKeys { !it.equals("Authorization", ignoreCase = true) }
                }
                current = next
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    private fun sameOrigin(a: URL, b: URL): Boolean =
        a.protocol == b.protocol && a.host.equals(b.host, ignoreCase = true) && portOf(a) == portOf(b)

    private fun portOf(url: URL): Int = if (url.port != -1) url.port else url.defaultPort

    private companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_TEXT_BYTES = 2 * 1024 * 1024
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
