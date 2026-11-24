package org.ragbot.replica.assistant

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.util.UUID
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal fun speechOrigin(value: String): URI? {
    val address = value.trim()
    val explicit = "://" in address
    val uri =
        try {
            URI(if (explicit) address else "http://$address")
        } catch (_: URISyntaxException) {
            return null
        }
    if (
        uri.scheme.lowercase() !in setOf("http", "https") ||
            uri.host == null ||
            uri.userInfo != null ||
            uri.rawQuery != null ||
            uri.rawFragment != null ||
            uri.rawPath !in setOf("", "/") ||
            uri.rawAuthority.endsWith(":") ||
            (uri.port != -1 && uri.port !in 1..65535) ||
            (!explicit && uri.port == -1)
    )
        return null
    return URI(uri.scheme.lowercase(), null, uri.host, uri.port, null, null, null)
}

internal sealed interface SpeechResult {
    data class Transcript(val text: String) : SpeechResult

    data class Busy(val retryAfterMillis: Long) : SpeechResult

    data object Empty : SpeechResult

    data object Timeout : SpeechResult

    data object Unavailable : SpeechResult
}

/** The speech provider sees only a health check or a new clip, on its own connection. */
internal class SpeechClient(
    private val origin: URI,
    // Allow the provider's five-minute queue plus processing time for one bounded recording.
    private val transcriptionTimeout: Int = 360_000,
) {
    suspend fun ready(): Boolean =
        withTimeoutOrNull(HEALTH_TIMEOUT.toLong()) {
            try {
                request("/api/health", HEALTH_TIMEOUT) { connection ->
                    if (connection.responseCode !in 200..299) return@request false
                    val reply = connection.json()
                    reply["status"] == JsonPrimitive("ok") &&
                        reply["model_loaded"] == JsonPrimitive(true)
                }
            } catch (_: IOException) {
                false
            } catch (_: JsonParseException) {
                false
            }
        } ?: false

    suspend fun transcribe(clip: File): SpeechResult =
        withTimeoutOrNull(transcriptionTimeout.toLong()) {
            try {
                request("/api/transcribe", transcriptionTimeout) { connection ->
                    val boundary = UUID.randomUUID().toString()
                    val header =
                        ("--$boundary\r\n" +
                                "Content-Disposition: form-data; name=\"file\"; filename=\"dictation.m4a\"\r\n" +
                                "Content-Type: audio/mp4\r\n\r\n")
                            .toByteArray()
                    val footer = "\r\n--$boundary--\r\n".toByteArray()
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=$boundary",
                    )
                    connection.setFixedLengthStreamingMode(
                        header.size + clip.length() + footer.size
                    )
                    connection.outputStream.use { output ->
                        output.write(header)
                        clip.inputStream().use { it.copyTo(output) }
                        output.write(footer)
                    }
                    when (connection.responseCode) {
                        429 -> SpeechResult.Busy(retryDelay(connection))
                        in 200..299 -> {
                            val text =
                                (connection.json()["text"] as? JsonPrimitive)
                                    ?.takeIf { it.isString }
                                    ?.asString
                                    ?.trim() ?: return@request SpeechResult.Unavailable
                            if (text.isEmpty()) SpeechResult.Empty
                            else SpeechResult.Transcript(text)
                        }
                        else -> SpeechResult.Unavailable
                    }
                }
            } catch (_: java.net.SocketTimeoutException) {
                SpeechResult.Timeout
            } catch (_: IOException) {
                SpeechResult.Unavailable
            } catch (_: JsonParseException) {
                SpeechResult.Unavailable
            }
        } ?: SpeechResult.Timeout

    private fun retryDelay(connection: HttpURLConnection): Long {
        val seconds = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
        if (seconds != null && seconds >= 0)
            return seconds.coerceAtMost((Long.MAX_VALUE / 2) / 1000) * 1000
        val date = connection.getHeaderFieldDate("Retry-After", -1)
        return if (date < 0) 0 else (date - System.currentTimeMillis()).coerceAtLeast(0)
    }

    private fun HttpURLConnection.json(): JsonObject =
        inputStream.bufferedReader(Charsets.UTF_8).use {
            jsonParser.fromJson(it, JsonObject::class.java)
                ?: throw JsonSyntaxException("Empty speech reply")
        }

    private suspend fun <T> request(path: String, timeout: Int, read: (HttpURLConnection) -> T): T =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val connection = origin.resolve(path).toURL().openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.connectTimeout = minOf(timeout, HEALTH_TIMEOUT)
                connection.readTimeout = timeout
                continuation.invokeOnCancellation {
                    // Disconnecting may block on the socket. Never do it on the UI thread.
                    Dispatchers.IO.dispatch(EmptyCoroutineContext) { connection.disconnect() }
                }
                try {
                    if (continuation.isActive) continuation.resume(read(connection))
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                } finally {
                    connection.disconnect()
                }
            }
        }

    private companion object {
        const val HEALTH_TIMEOUT = 3_000
        val jsonParser = GsonBuilder().setStrictness(Strictness.STRICT).create()
    }
}
