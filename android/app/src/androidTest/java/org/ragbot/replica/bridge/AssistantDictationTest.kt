package org.ragbot.replica.bridge

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ragbot.replica.MainActivity
import org.ragbot.replica.assistant.AssistantOverlay
import org.ragbot.replica.assistant.AssistantPanelState
import org.ragbot.replica.assistant.PanelHistory
import org.ragbot.replica.assistant.PanelLine
import org.ragbot.replica.assistant.SpeechClient
import org.ragbot.replica.assistant.speechOrigin

@RunWith(AndroidJUnit4::class)
class AssistantDictationTest {
    @get:Rule val ui = createEmptyComposeRule()

    @Test
    fun readinessRecoversWhileTheDraftKeepsFocus() = withPanel { stub, _ ->
        val text = ui.onNodeWithTag("assistant-text")
        ui.onNodeWithTag("assistant-mic").assertIsNotEnabled()
        text.performClick().performTextReplacement("پیش‌نویس")
        stub.healthy = true
        waitForMic(true, 35_000)
        text.assertIsFocused().assertTextContains("پیش‌نویس")
        ui.runOnIdle {
            AssistantPanelState.history = PanelHistory(
                lines = List(40) { PanelLine(it % 2 == 0, "پیام آزمایشی $it") },
            )
        }
        ui.onNodeWithTag("assistant-messages").performScrollToIndex(2)
        val olderMessage = ui.onNodeWithText("پیام آزمایشی 2")
        stub.healthy = false
        waitForMic(false, 35_000)
        text.assertIsFocused().assertTextContains("پیش‌نویس")
        olderMessage.assertIsDisplayed()
        stub.healthy = true
        waitForMic(true, 35_000)
        text.assertIsFocused().assertTextContains("پیش‌نویس")
        olderMessage.assertIsDisplayed()
    }

    @Test
    fun recordingAppendsToTheLiveDraftAndClosingDiscardsALateResult() = withPanel { stub, app ->
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.RECORD_AUDIO,
        )
        stub.healthy = true
        // Restart visibility so the first real health request sees the ready provider.
        app.moveToState(Lifecycle.State.CREATED).moveToState(Lifecycle.State.RESUMED)
        waitForMic(true)
        val text = ui.onNodeWithTag("assistant-text")
        ui.runOnIdle { AssistantPanelState.history = PanelHistory(taskState = "ready_for_user") }
        ui.onNodeWithTag("assistant-cancel").assertIsDisplayed()
        text.performClick().performTextReplacement("برای")
        startAndStop()
        ui.waitUntil(5_000) { stub.uploads.get() == 1 }
        ui.onNodeWithTag("assistant-cancel").assertDoesNotExist()
        text.performTextReplacement("لطفاً برای")
        stub.releaseReplies = true
        ui.waitUntil(5_000) { AssistantPanelState.draft == "لطفاً برای رضا" }
        text.assertIsFocused().assertTextContains("لطفاً برای رضا")
        ui.onNodeWithTag("assistant-cancel").assertIsDisplayed()
        assertTrue("the provider received an M4A file field", stub.validUpload)
        assertFalse("the assistant was not asked to send", AssistantPanelState.sending)

        stub.releaseReplies = false
        startAndStop()
        ui.waitUntil(5_000) { stub.uploads.get() == 2 }
        ui.onNodeWithTag("assistant-minimize").performClick()
        stub.releaseReplies = true
        ui.waitUntil(5_000) { stub.replies.get() == 2 }
        ui.onNodeWithTag("assistant-bubble").performClick()
        ui.onNodeWithTag("assistant-text").assertTextContains("لطفاً برای رضا")
        assertEquals(2, stub.uploads.get())
        ui.waitUntil(5_000) {
            instrumentation.targetContext.cacheDir.listFiles().orEmpty().none {
                it.name.startsWith("dictation-")
            }
        }
    }

    private fun startAndStop() {
        ui.onNodeWithTag("assistant-mic").performClick()
        ui.waitUntil(5_000) {
            ui.onAllNodes(hasTestTag("assistant-voice-stop")).fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithTag("assistant-cancel").assertDoesNotExist()
        Thread.sleep(1_000) // Let the emulator's microphone deliver AAC samples before stopping.
        ui.onNodeWithTag("assistant-voice-stop").performClick()
    }

    private fun waitForMic(enabled: Boolean, timeout: Long = 5_000) =
        ui.waitUntil(timeout) {
            ui.onAllNodes(hasTestTag("assistant-mic") and isEnabled())
                .fetchSemanticsNodes()
                .isNotEmpty() == enabled
        }

    private fun withPanel(check: (SpeechStub, ActivityScenario<MainActivity>) -> Unit) {
        SpeechStub().use { stub ->
            ActivityScenario.launch(MainActivity::class.java).use { app ->
                val speech = SpeechClient(checkNotNull(speechOrigin("127.0.0.1:${stub.port}")))
                app.onActivity { activity ->
                    AssistantPanelState.available = true
                    AssistantPanelState.open = true
                    AssistantPanelState.history = PanelHistory()
                    AssistantPanelState.draft = ""
                    activity.setContent {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Rtl
                        ) {
                            AssistantOverlay(speech)
                        }
                    }
                }
                try {
                    check(stub, app)
                } finally {
                    app.onActivity {
                        AssistantPanelState.open = false
                        AssistantPanelState.draft = ""
                    }
                }
            }
        }
    }
}

/** Device-local provider. No audio leaves the emulator, and replies ignore the recorded signal. */
private class SpeechStub : Closeable {
    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    val port = server.localPort
    val uploads = AtomicInteger()
    val replies = AtomicInteger()
    @Volatile var healthy = false
    @Volatile var releaseReplies = false
    @Volatile var validUpload = false

    private val worker =
        thread(name = "speech-test-stub") {
            while (!server.isClosed) {
                try {
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val input = socket.getInputStream().buffered()
                        val route = input.line()
                        val headers =
                            generateSequence { input.line().takeIf(String::isNotEmpty) }.toList()
                        val upload = route.startsWith("POST /api/transcribe ")
                        if (upload) {
                            val size =
                                headers
                                    .single { it.startsWith("Content-Length:", ignoreCase = true) }
                                    .substringAfter(':')
                                    .trim()
                                    .toInt()
                            val bytes = ByteArray(size)
                            java.io.DataInputStream(input).readFully(bytes)
                            val multipart = bytes.toString(Charsets.ISO_8859_1)
                            validUpload =
                                "name=\"file\"; filename=\"dictation.m4a\"" in multipart &&
                                    "Content-Type: audio/mp4" in multipart &&
                                    "ftyp" in multipart &&
                                    "keep_files" !in multipart &&
                                    headers.none { it.startsWith("X-Assistant-", true) }
                            uploads.incrementAndGet()
                            while (!releaseReplies && !server.isClosed) Thread.sleep(20)
                            replies.incrementAndGet()
                        }
                        val reply =
                            if (upload) """{"text":"رضا"}"""
                            else """{"status":"ok","model_loaded":$healthy}"""
                        val bytes = reply.toByteArray()
                        socket.getOutputStream().apply {
                            write(
                                "HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: ${bytes.size}\r\n\r\n"
                                    .toByteArray()
                            )
                            write(bytes)
                            flush()
                        }
                    }
                } catch (_: IOException) {
                    // Discarding a clip can close the client's socket before the response.
                }
            }
        }

    private fun BufferedInputStream.line(): String = buildString {
        while (true) {
            val next = read()
            if (next == -1 || next == 10) break
            if (next != 13) append(next.toChar())
        }
    }

    override fun close() {
        server.close()
        worker.join(6_000)
    }
}
