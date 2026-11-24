package org.ragbot.replica.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ragbot.replica.BuildConfig

/*
 * The assistant panel: a floating «دستیار» bubble the user can drag anywhere opens and closes a small
 * chat box (so does «دستیار» in the support menu). The box steps aside while the agent drives the
 * app and comes back when it is done; cancel is a separate control, and it closes the box.
 * It lives inside the app's own window, never in a dialog, so the executor still sees and taps the
 * screen behind it. It catches no Back press (the executor uses Back) and shows no spinner (an
 * indeterminate one reads as loading). Under the messages, «مراحل دستیار» opens the agent's own steps
 * (a thought, an action and its result). It keeps the conversation only in memory, and it has no way
 * to confirm, submit or pay: only the user confirms, on the app's own review.
 */
/** The chat box when open, the bubble always, and the polling while the agent works. */
@Composable
internal fun AssistantOverlay(
    speech: SpeechClient? = remember { speechOrigin(BuildConfig.ASR_URL)?.let { SpeechClient(it) } }
) {
    val state = AssistantPanelState
    if (!state.available) return
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.open) { if (state.open) refresh(state) }
    LaunchedEffect(state.history.busy, state.sending) {
        while (state.history.busy || state.sending) {
            delay(POLL_MS)
            refresh(state)
            stepAside(state)
        }
        stepAside(state) // the turn ended: a box that stepped aside comes back
    }
    val view = LocalView.current
    LaunchedEffect(state.history.questionId, state.open) {
        val fields = state.history.fields
        if (state.open && fields.isNotEmpty()) {
            delay(SETTLE_MS) // the page first moves above the box
            bringIntoView(view, fields)
        }
    }
    val context = LocalContext.current.applicationContext
    val voice = remember(speech) { Dictation(context, speech, scope) }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        val room = (maxHeight - 54.dp).coerceAtLeast(0.dp)
        val panelHeight = (maxHeight * PANEL_SHARE).coerceIn(minOf(245.dp, room), room)
        if (state.open) Panel(state, scope, voice, panelHeight)
        Bubble(state, if (state.open) panelHeight else 0.dp)
    }
}

@Composable
private fun Panel(state: AssistantPanelState, scope: CoroutineScope, voice: Dictation, height: Dp) {
    val history = state.history
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val list = rememberLazyListState()
    val followNewMessages = !list.canScrollForward
    LaunchedEffect(history.lines.size, history.options.size) {
        if (followNewMessages && history.lines.isNotEmpty())
            list.scrollToItem(history.lines.lastIndex)
    }

    val task = Triple(state.conversationId, history.questionId, history.taskState)
    // A permission reply belongs to the request that launched it, even if the panel has closed.
    var permissionPending by remember { mutableStateOf<(() -> Boolean)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val draftIsCurrent = permissionPending
            permissionPending = null
            if (draftIsCurrent != null)
                voice.permissionResult(granted, draftIsCurrent) {
                    state.draft = appendTranscript(state.draft, it)
                }
        }
    DisposableEffect(lifecycle, task, history.busy) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) voice.discard()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            voice.discard()
        }
    }
    LaunchedEffect(lifecycle, voice) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { voice.watchReadiness() }
    }

    val canCompose = !history.busy && !state.sending
    val submit = {
        val draft = state.draft
        val text = draft.trim()
        if (!state.history.busy && !state.sending && text.isNotEmpty()) {
            voice.discard()
            focus.clearFocus()
            act(
                state,
                scope,
                call = { send(state, text, null) },
                onSuccess = { if (state.draft == draft) state.draft = "" },
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height)
                .onGloballyPositioned {
                    state.covered =
                        it.findRootCoordinates().size.height - it.boundsInRoot().top.toInt()
                }
                .testTag("assistant-panel"),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFFFAFCFA),
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "دستیار",
                            color = PanelGreen,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val status =
                            STATE_WORDS[history.shownState]
                                ?: if (history.busy || state.sending) "در حال انجام…" else ""
                        Text(
                            status,
                            Modifier.testTag("assistant-state"),
                            color = PanelMuted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (history.busy || history.shownState in CANCELLABLE) {
                        TextButton(
                            {
                                if (!state.sending || history.busy) {
                                    voice.discard()
                                    state.open = false
                                    act(state, scope, call = AssistantClient::cancel)
                                }
                            },
                            Modifier.testTag("assistant-cancel"),
                            colors = ButtonDefaults.textButtonColors(contentColor = PanelError),
                        ) {
                            Text("لغو کار")
                        }
                    }
                    IconButton(
                        {
                            voice.discard()
                            state.open = false
                        },
                        Modifier.testTag("assistant-minimize").semantics {
                            contentDescription = "کوچک کردن گفتگو"
                        },
                    ) {
                        Canvas(Modifier.size(20.dp)) {
                            drawLine(
                                PanelMuted,
                                Offset(3.dp.toPx(), center.y),
                                Offset(size.width - 3.dp.toPx(), center.y),
                                2.dp.toPx(),
                                StrokeCap.Round,
                            )
                        }
                    }
                }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().testTag("assistant-messages"),
                    list,
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(history.lines) { Line(it) }
                    itemsIndexed(history.options) { index, option ->
                        OutlinedButton(
                            {
                                if (!state.history.busy && !state.sending) {
                                    voice.discard()
                                    act(state, scope, call = { send(state, null, option) })
                                }
                            },
                            Modifier.fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .testTag("assistant-option-$index"),
                            enabled = canCompose,
                        ) {
                            Text(leftToRight(option.label))
                        }
                    }
                    if (history.agentSteps.isNotEmpty())
                        item {
                            TextButton(
                                { state.stepsOpen = !state.stepsOpen },
                                Modifier.testTag("assistant-agent-steps"),
                            ) {
                                Text(
                                    "مراحل دستیار (${history.agentSteps.size}) ${if (state.stepsOpen) "▴" else "▾"}"
                                )
                            }
                        }
                    if (state.stepsOpen)
                        itemsIndexed(history.agentSteps) { index, step -> AgentStep(step, index) }
                    items(history.steps) {
                        Text(it, color = PanelMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.error?.let {
                    Text(
                        it,
                        Modifier.testTag("assistant-error"),
                        color = PanelError,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
                VoiceStatus(voice)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (voice.phase == null) {
                        Microphone(voice, enabled = canCompose && permissionPending == null) {
                            if (state.sending || state.history.busy || !voice.askPermission())
                                return@Microphone
                            val draftIsCurrent = {
                                state.open &&
                                    lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                                    !state.sending &&
                                    !state.history.busy &&
                                    task ==
                                        Triple(
                                            state.conversationId,
                                            state.history.questionId,
                                            state.history.taskState,
                                        )
                            }
                            if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                voice.permissionResult(true, draftIsCurrent) {
                                    state.draft = appendTranscript(state.draft, it)
                                }
                            } else {
                                permissionPending = draftIsCurrent
                                launcher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    } else Box(Modifier.size(48.dp))
                    OutlinedTextField(
                        state.draft,
                        { state.draft = it },
                        Modifier.weight(1f).testTag("assistant-text"),
                        placeholder = { Text("درخواست خود را بنویسید", maxLines = 1) },
                        enabled = canCompose,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PanelGreen,
                            cursorColor = PanelGreen,
                        ),
                    )
                    Button(
                        submit,
                        Modifier.testTag("assistant-send"),
                        enabled = canCompose && state.draft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PanelGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("ارسال")
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatus(voice: Dictation) {
    val phase = voice.phase
    val message =
        when (phase) {
            VoicePhase.Permission -> "در انتظار دسترسی به میکروفون"
            VoicePhase.Starting -> "در حال آماده‌کردن میکروفون"
            VoicePhase.Recording -> "در حال ضبط صدا"
            VoicePhase.Transcribing -> "در حال تبدیل صدا به متن"
            null -> voice.status
        } ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            message,
            Modifier.weight(1f).testTag("assistant-voice-status").semantics {
                liveRegion = LiveRegionMode.Polite
            },
            color = if (phase == VoicePhase.Recording) PanelError else PanelMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        if (phase == VoicePhase.Recording) {
            TextButton(
                voice::stopRecording,
                Modifier.testTag("assistant-voice-stop"),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("توقف ضبط")
            }
        }
        if (phase != null) {
            TextButton(
                { voice.discard("صدا حذف شد.") },
                Modifier.testTag("assistant-voice-discard"),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("حذف صدا")
            }
        }
    }
}

@Composable
private fun Microphone(voice: Dictation, enabled: Boolean, onClick: () -> Unit) {
    val active = enabled && voice.ready && !voice.coolingDown
    val label =
        when {
            voice.coolingDown -> "میکروفون؛ سرویس گفتار شلوغ است"
            !voice.ready -> "میکروفون؛ سرویس گفتار در دسترس نیست"
            else -> "ضبط صدا"
        }
    IconButton(
        onClick,
        Modifier.testTag("assistant-mic").semantics { contentDescription = label },
        enabled = active,
    ) {
        val color = if (active) PanelGreen else Color(0xFF6D7772)
        Canvas(Modifier.size(24.dp)) {
            val unit = size.width / 24
            fun point(x: Int, y: Int) = Offset(x * unit, y * unit)
            drawRoundRect(color, point(9, 2), Size(6 * unit, 12 * unit), CornerRadius(3 * unit))
            drawArc(
                color,
                0f,
                180f,
                false,
                point(5, 5),
                Size(14 * unit, 13 * unit),
                style = Stroke(2 * unit),
            )
            drawLine(color, point(12, 18), point(12, 22), 2 * unit, StrokeCap.Round)
            drawLine(color, point(9, 22), point(15, 22), 2 * unit, StrokeCap.Round)
        }
    }
}

private val PanelGreen = Color(0xFF145D32)
private val PanelMuted = Color(0xFF4C5D54)
private val PanelError = Color(0xFFB3261E)

/** One message: the user's on one side, the assistant's on the other. */
@Composable
private fun Line(line: PanelLine) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            leftToRight(line.text),
            Modifier.align(if (line.fromUser) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(
                    start = if (line.fromUser) 0.dp else 28.dp,
                    end = if (line.fromUser) 28.dp else 0.dp,
                )
                .widthIn(max = 480.dp)
                .background(
                    if (line.fromUser) Color(0xFFDDEFE4) else Color(0xFFEDF1EF),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color(0xFF172B20),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** One of the agent's steps: its thought, then the action and its result, small and grey. */
@Composable
private fun AgentStep(step: PanelStep, index: Int) {
    Column(
        Modifier.fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFFF4F5F4), RoundedCornerShape(6.dp))
            .padding(6.dp)
            .testTag("assistant-agent-step-$index")
    ) {
        Text(leftToRight(step.thought), style = MaterialTheme.typography.bodySmall)
        Text(
            // The action and its JSON result read left to right, with any Persian inside kept in
            // place.
            "\u2066${step.action} → ${step.result.ifEmpty { "…" }}\u2069",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The text with each masked number, such as 610433******3086, kept left to right in Persian. */
private fun leftToRight(text: String) = MASKED_NUMBER.replace(text) { "\u2066${it.value}\u2069" }

private val MASKED_NUMBER = Regex("[0-9۰-۹]*\\*+[0-9۰-۹*]*")

/** Sends the user's words, or an option of the pending question, noting where the app was. */
private fun send(state: AssistantPanelState, text: String?, option: PanelOption?) {
    state.startScreen = AssistantClient.lastObservation()?.screen
    AssistantClient.send(state.conversationId, text, option, state.history.questionId)
}

/**
 * Runs one call off the main thread, then reloads the conversation; a failure is shown, not thrown.
 */
private fun act(
    state: AssistantPanelState,
    scope: CoroutineScope,
    call: () -> Unit,
    onSuccess: () -> Unit = {},
) {
    state.pendingCalls++
    state.error = null
    scope.launch {
        try {
            withContext(Dispatchers.IO) { call() }
            onSuccess()
        } catch (error: IOException) {
            state.error = "انجام نشد: ${error.message}"
        } catch (error: IllegalStateException) {
            state.error = "اتصال برنامه به دستیار هنوز برقرار نشده است."
        } finally {
            try {
                refresh(state)
            } finally {
                state.pendingCalls--
            }
        }
    }
}

/** Reloads the conversation from the backend; a failure leaves what is shown. */
private suspend fun refresh(state: AssistantPanelState) {
    try {
        state.history =
            withContext(Dispatchers.IO) { AssistantClient.history(state.conversationId) }
    } catch (error: IOException) {
        state.error = "دستیار در دسترس نیست."
    }
}

/**
 * Scrolls the page behind the box so the first asked field it shows sits at the top of the part
 * above the box. The fields come with the question; a page without one of them is left as it is.
 */
private fun bringIntoView(view: View, fields: List<String>) {
    val root = (view as? RootForTest)?.semanticsOwner?.rootSemanticsNode ?: return
    val nodes =
        generateSequence(listOf(root)) { level -> level.flatMap { it.children }.ifEmpty { null } }
            .flatten()
    val field =
        fields.firstNotNullOfOrNull { ref ->
            nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == ref }
        } ?: return
    val page =
        generateSequence(field.parent) { it.parent }
            .firstOrNull { SemanticsActions.ScrollBy in it.config }
    // The field's own place, not its shown part: a field out of view shows nothing.
    val by =
        field.positionInRoot.y -
            (page ?: return).boundsInRoot.top -
            MARGIN_DP * view.resources.displayMetrics.density
    page.config[SemanticsActions.ScrollBy].action?.invoke(0f, by)
}

private const val POLL_MS = 700L
private const val SETTLE_MS = 300L
private const val MARGIN_DP = 16

/** The share of the screen the open box takes, from the bottom. */
internal const val PANEL_SHARE = .45f
