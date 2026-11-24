package org.ragbot.replica.assistant

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal enum class VoicePhase {
    Permission,
    Starting,
    Recording,
    Transcribing,
}

internal class Dictation(
    private val context: Context,
    private val speech: SpeechClient?,
    private val scope: CoroutineScope,
) {
    var ready by mutableStateOf(false)
        private set

    var phase by mutableStateOf<VoicePhase?>(null)
        private set

    var status by mutableStateOf<String?>(null)
        private set

    var coolingDown by mutableStateOf(false)
        private set

    private var operation: Job? = null
    private var stop: CompletableDeferred<Unit>? = null
    @OptIn(ExperimentalCoroutinesApi::class)
    private val recorderThread = Dispatchers.IO.limitedParallelism(1)

    suspend fun watchReadiness() {
        if (speech == null) return
        try {
            while (true) {
                ready = speech.ready()
                delay(30_000)
            }
        } finally {
            ready = false
        }
    }

    fun askPermission(): Boolean {
        if (!ready || coolingDown || phase != null) return false
        status = null
        phase = VoicePhase.Permission
        return true
    }

    fun permissionResult(
        granted: Boolean,
        draftIsCurrent: () -> Boolean,
        append: (String) -> Unit,
    ) {
        if (phase != VoicePhase.Permission) return
        if (!granted) {
            discard("برای ضبط، دسترسی به میکروفون را در تنظیمات برنامه فعال کنید.")
            return
        }
        if (!draftIsCurrent() || !ready) {
            discard()
            return
        }
        val provider = checkNotNull(speech)
        val finish = CompletableDeferred<Unit>()
        stop = finish
        phase = VoicePhase.Starting
        operation =
            scope.launch {
                var clip: File? = null
                var recorder: MediaRecorder? = null
                try {
                    withContext(recorderThread) {
                        val file = File.createTempFile("dictation-", ".m4a", context.cacheDir)
                        clip = file
                        val capture = newRecorder()
                        recorder = capture
                        capture.apply {
                            setOnErrorListener { _, _, _ ->
                                scope.launch {
                                    if (stop === finish)
                                        discard("ضبط صدا انجام نشد. دوباره تلاش کنید.")
                                }
                            }
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setAudioChannels(1)
                            setAudioSamplingRate(16_000)
                            setAudioEncodingBitRate(48_000)
                            setOutputFile(file.absolutePath)
                            prepare()
                            start()
                        }
                    }
                    phase = VoicePhase.Recording
                    withTimeoutOrNull(45_000) { finish.await() }
                    phase = VoicePhase.Transcribing
                    withContext(recorderThread) {
                        recorder?.stop()
                        recorder?.release()
                        recorder = null
                    }
                    val file = checkNotNull(clip)
                    val result = provider.transcribe(file)
                    if (stop !== finish || !draftIsCurrent()) return@launch
                    status =
                        when (result) {
                            is SpeechResult.Transcript -> {
                                append(result.text)
                                "متن صدا به پیش‌نویس اضافه شد."
                            }
                            is SpeechResult.Busy -> {
                                if (result.retryAfterMillis > 0) {
                                    coolingDown = true
                                    scope.launch {
                                        delay(result.retryAfterMillis)
                                        coolingDown = false
                                    }
                                }
                                "سرویس گفتار شلوغ است. کمی بعد دوباره ضبط کنید."
                            }
                            SpeechResult.Empty -> "گفتاری پیدا نشد. دوباره ضبط کنید."
                            SpeechResult.Timeout -> "زمان تبدیل صدا تمام شد. دوباره ضبط کنید."
                            SpeechResult.Unavailable ->
                                "تبدیل صدا انجام نشد. اتصال را بررسی کنید و دوباره ضبط کنید."
                        }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IOException) {
                    if (stop === finish) status = "ضبط صدا انجام نشد. دوباره تلاش کنید."
                } catch (_: RuntimeException) {
                    if (stop === finish) status = "ضبط صدا انجام نشد. دوباره تلاش کنید."
                } finally {
                    if (stop === finish) {
                        phase = null
                        stop = null
                        operation = null
                    }
                    withContext(NonCancellable + recorderThread) {
                        try {
                            recorder?.release()
                        } finally {
                            clip?.delete()
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        if (phase != VoicePhase.Recording) return
        phase = VoicePhase.Transcribing
        stop?.complete(Unit)
    }

    fun discard(message: String? = null) {
        stop = null
        phase = null
        status = message
        operation?.cancel()
        operation = null
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
}

internal fun appendTranscript(draft: String, transcript: String): String =
    draft + (if (draft.lastOrNull()?.isWhitespace() == false) " " else "") + transcript
