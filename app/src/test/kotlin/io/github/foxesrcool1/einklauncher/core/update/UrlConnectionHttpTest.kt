package io.github.foxesrcool1.einklauncher.core.update

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress

/**
 * The real network code, against two small servers on this machine. One
 * plays GitHub and one plays the storage server GitHub redirects to.
 */
class UrlConnectionHttpTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val servers = mutableListOf<HttpServer>()
    private val http = UrlConnectionHttp(requireHttps = false)

    private fun server(routes: Map<String, (HttpExchange) -> Unit>): String {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        routes.forEach { (path, handler) -> server.createContext(path) { exchange -> exchange.use(handler) } }
        server.start()
        servers += server
        return "http://127.0.0.1:${server.address.port}"
    }

    private fun HttpExchange.answer(code: Int, body: ByteArray = ByteArray(0), headers: Map<String, String> = emptyMap()) {
        headers.forEach { (name, value) -> responseHeaders.add(name, value) }
        sendResponseHeaders(code, if (body.isEmpty()) -1 else body.size.toLong())
        if (body.isNotEmpty()) responseBody.write(body)
    }

    @After
    fun stop() = servers.forEach { it.stop(0) }

    @Test
    fun `text comes back with its code and its headers in lower case`() {
        val base = server(mapOf("/list" to { it.answer(200, "[1]".toByteArray(), mapOf("X-RateLimit-Remaining" to "59")) }))
        val answer = http.getText("$base/list", emptyMap())
        assertEquals(200, answer.code)
        assertEquals("[1]", answer.body)
        assertEquals("59", answer.headers["x-ratelimit-remaining"])
    }

    @Test
    fun `an error answer keeps its body, so the reason can be read`() {
        val base = server(mapOf("/list" to { it.answer(404, """{"message":"Not Found"}""".toByteArray()) }))
        val answer = http.getText("$base/list", emptyMap())
        assertEquals(404, answer.code)
        assertTrue(answer.body.contains("Not Found"))
    }

    @Test
    fun `the token is not sent on to the storage server`() {
        var tokenAtStorage: String? = "not asked"
        var acceptAtStorage: String? = null
        val payload = ByteArray(200_000) { (it % 7).toByte() }
        val storage = server(
            mapOf(
                "/signed" to {
                    tokenAtStorage = it.requestHeaders.getFirst("Authorization")
                    acceptAtStorage = it.requestHeaders.getFirst("Accept")
                    it.answer(200, payload)
                },
            ),
        )
        var tokenAtGithub: String? = null
        val github = server(
            mapOf(
                "/asset" to {
                    tokenAtGithub = it.requestHeaders.getFirst("Authorization")
                    it.answer(302, headers = mapOf("Location" to "$storage/signed?sig=abc"))
                },
            ),
        )

        val target = File(temporary.root, "out.apk")
        var last = 0L
        val code = http.download(
            "$github/asset",
            mapOf("Authorization" to "Bearer secret", "Accept" to "application/octet-stream"),
            target,
        ) { last = it }

        assertEquals(200, code)
        assertEquals("Bearer secret", tokenAtGithub)
        assertNull("the token leaked to the second server", tokenAtStorage)
        assertEquals("application/octet-stream", acceptAtStorage)
        assertArrayEquals(payload, target.readBytes())
        assertEquals(payload.size.toLong(), last)
    }

    @Test
    fun `the token stays on a redirect inside the same server`() {
        var tokenAfter: String? = null
        lateinit var base: String
        base = server(
            mapOf(
                "/old" to { it.answer(301, headers = mapOf("Location" to "/new")) },
                "/new" to {
                    tokenAfter = it.requestHeaders.getFirst("Authorization")
                    it.answer(200, "ok".toByteArray())
                },
            ),
        )
        assertEquals("ok", http.getText("$base/old", mapOf("Authorization" to "Bearer secret")).body)
        assertEquals("Bearer secret", tokenAfter)
    }

    @Test
    fun `a redirect loop ends with an error`() {
        val base = server(mapOf("/loop" to { it.answer(302, headers = mapOf("Location" to "/loop")) }))
        assertThrows(IOException::class.java) { http.getText("$base/loop", emptyMap()) }
    }

    @Test
    fun `nothing is written when the answer is not 200`() {
        val base = server(mapOf("/gone" to { it.answer(404, "no".toByteArray()) }))
        val target = File(temporary.root, "out.apk")
        assertEquals(404, http.download("$base/gone", emptyMap(), target) {})
        assertFalse(target.exists())
    }

    @Test
    fun `plain http is refused unless a test asks for it`() {
        val base = server(mapOf("/list" to { it.answer(200, "[]".toByteArray()) }))
        val error = assertThrows(IOException::class.java) { UrlConnectionHttp().getText("$base/list", emptyMap()) }
        assertTrue(error.message.orEmpty().contains("https"))
    }
}
