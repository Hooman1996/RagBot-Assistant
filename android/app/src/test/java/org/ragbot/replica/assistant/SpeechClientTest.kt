package org.ragbot.replica.assistant

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SpeechClientTest {
    @get:Rule val files = TemporaryFolder()

    @Test
    fun acceptsOnlyHttpOrigins() {
        assertEquals("http://example.test:9081", speechOrigin(" example.test:9081 ")?.toString())
        assertEquals("https://example.test", speechOrigin("https://example.test/")?.toString())
        assertEquals("http://[::1]:9081", speechOrigin("[::1]:9081")?.toString())
        for (invalid in
            listOf(
                "",
                "example.test",
                "file:///speech",
                "http://a:0",
                "http://a:65536",
                "http://a:",
                "http://person:secret@a",
                "https://a/path",
                "https://a/?query",
                "https://a/#fragment",
            )) {
            assertNull(invalid, speechOrigin(invalid))
        }
    }

    @Test
    fun readinessRequiresHttpSuccessAndStrictJson() = runBlocking {
        val replies = LinkedBlockingQueue<Pair<Int, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/health") { exchange ->
            val (code, body) = replies.remove()
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val client =
                SpeechClient(checkNotNull(speechOrigin("127.0.0.1:${server.address.port}")))
            val healthy = """{"status":"ok","model_loaded":true,"uptime_sec":12.3}"""
            replies.add(503 to healthy)
            assertFalse(client.ready())
            replies.add(200 to healthy)
            assertTrue(client.ready())
            replies.add(200 to """{"status":"ok","model_loaded":"true"}""")
            assertFalse(client.ready())
            replies.add(200 to """{"status":"loading","model_loaded":true}""")
            assertFalse(client.ready())
            replies.add(200 to healthy)
            assertTrue(client.ready())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun sendsOnlyTheClipOnceAndHandlesProviderReplies() = runBlocking {
        val requests = LinkedBlockingQueue<Pair<String?, String>>()
        val replies = LinkedBlockingQueue<Pair<Int, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/transcribe") { exchange ->
            val body =
                exchange.requestBody.use { it.readBytes().toString(StandardCharsets.ISO_8859_1) }
            requests.add(exchange.requestHeaders.getFirst("X-Assistant-Session") to body)
            val (code, response) = replies.remove()
            if (code == 429) exchange.responseHeaders.set("Retry-After", "4")
            val bytes = response.toByteArray()
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val clip = files.newFile("test.m4a").apply { writeText("synthetic-audio") }
            val client =
                SpeechClient(checkNotNull(speechOrigin("127.0.0.1:${server.address.port}")))
            replies.add(200 to """{"text":"  سلام  ","segments":[{"start":0.1}],"total_sec":1.5}""")
            assertEquals(SpeechResult.Transcript("سلام"), client.transcribe(clip))
            val (session, multipart) = requests.remove()
            assertNull(session)
            assertTrue(multipart.contains("name=\"file\"; filename=\"dictation.m4a\""))
            assertTrue(multipart.contains("Content-Type: audio/mp4\r\n\r\nsynthetic-audio"))
            assertFalse(multipart.contains("keep_files"))
            replies.add(429 to "{}")
            assertEquals(SpeechResult.Busy(4000), client.transcribe(clip))
            replies.add(200 to """{"text":" "}""")
            assertEquals(SpeechResult.Empty, client.transcribe(clip))
            replies.add(200 to """{"text":42}""")
            assertEquals(SpeechResult.Unavailable, client.transcribe(clip))
            replies.add(500 to "{}")
            assertEquals(SpeechResult.Unavailable, client.transcribe(clip))
            assertEquals(4, requests.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun aStalledUploadTimesOutWithoutRetrying() = runBlocking {
        val uploads = LinkedBlockingQueue<Unit>()
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/transcribe") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            uploads.add(Unit)
            release.await(5, TimeUnit.SECONDS)
            exchange.close()
        }
        server.start()
        try {
            val clip = files.newFile("test.m4a").apply { writeText("synthetic-audio") }
            val client =
                SpeechClient(checkNotNull(speechOrigin("127.0.0.1:${server.address.port}")), 500)
            assertEquals(SpeechResult.Timeout, client.transcribe(clip))
            assertEquals(1, uploads.size)
        } finally {
            release.countDown()
            server.stop(0)
        }
    }

    @Test
    fun appendsToTheCurrentDraftWithSpacing() {
        assertEquals("رضا", appendTranscript("", "رضا"))
        assertEquals("برای رضا", appendTranscript("برای", "رضا"))
        assertEquals("برای رضا", appendTranscript("برای ", "رضا"))
    }
}
