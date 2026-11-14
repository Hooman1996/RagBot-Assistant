package org.ragbot.replica.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * The assistant panel: open it from «دستیار» in the support menu, talk to the agent, minimize it,
 * reopen it from its bubble, and cancel the task with a separate control.
 * It lives inside the app's own window, never in a dialog, so the executor still sees and taps the
 * screen behind it. It catches no Back press (the executor uses Back) and shows no spinner (an
 * indeterminate one reads as loading). It keeps the conversation only in memory, and it has no way
 * to confirm, submit or pay: only the user confirms, on the app's own review.
 */
internal object AssistantPanelState {
    var available by mutableStateOf(false) // the bridge loop is on: a debug build with the flag file
    var open by mutableStateOf(false)
    var conversationId by mutableStateOf(UUID.randomUUID().toString())
    var history by mutableStateOf(PanelHistory())
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

/** The task states in Persian, as the panel and its bubble show them. */
internal val STATE_WORDS =
    mapOf(
        "created" to "شروع",
        "clarifying" to "منتظر پاسخ شما",
        "resolving" to "در حال بررسی",
        "preparing" to "در حال آماده‌سازی در برنامه",
        "verifying_review" to "بررسی صفحه بازبینی",
        "ready_for_user" to "آماده بازبینی شما",
        "completed" to "انجام شد",
        "paused" to "نگه داشته شده",
        "reconciling" to "بررسی دوباره",
        "cancelling" to "در حال لغو",
        "cancelled" to "لغو شد",
        "failed" to "انجام نشد",
    )

/** States in which the task can still be cancelled. */
internal val CANCELLABLE =
    setOf(
        "created",
        "clarifying",
        "resolving",
        "preparing",
        "verifying_review",
        "ready_for_user",
        "paused",
        "reconciling",
    )

/** The panel when open, its bubble when minimized with a conversation, and the polling while busy. */
@Composable
internal fun AssistantOverlay() {
    val state = AssistantPanelState
    if (!state.available) return
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.open) { if (state.open) refresh(state) }
    LaunchedEffect(state.history.busy, state.sending) {
        while (state.history.busy || state.sending) {
            delay(POLL_MS)
            refresh(state)
        }
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (state.open) Panel(state, scope)
        else if (state.history.lines.isNotEmpty())
            ExtendedFloatingActionButton(
                { state.open = true },
                Modifier.align(Alignment.BottomStart).padding(16.dp).testTag("assistant-bubble"),
            ) {
                Text("دستیار · ${STATE_WORDS[state.history.shownState] ?: "گفتگو"}")
            }
    }
}

@Composable
private fun Panel(state: AssistantPanelState, scope: CoroutineScope) {
    val history = state.history
    val focus = LocalFocusManager.current
    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.6f).testTag("assistant-panel"),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("دستیار", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "  ${STATE_WORDS[history.shownState].orEmpty()}",
                        Modifier.weight(1f).testTag("assistant-state"),
                        color = Color.Gray,
                    )
                    TextButton({ state.open = false }, Modifier.testTag("assistant-minimize")) {
                        Text("کوچک کردن")
                    }
                    if (history.busy || history.shownState in CANCELLABLE)
                        OutlinedButton(
                            { act(state, scope) { AssistantClient.cancel() } },
                            Modifier.testTag("assistant-cancel"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E)),
                        ) {
                            Text("لغو کار")
                        }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("assistant-messages")) {
                    items(history.lines) { Line(it) }
                    if (history.busy || state.sending) item { Text("در حال انجام…", color = Color.Gray) }
                    items(history.steps) { Text(it, color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
                }
                history.options.forEachIndexed { index, option ->
                    OutlinedButton(
                        { act(state, scope) { send(state, null, option) } },
                        Modifier.fillMaxWidth().testTag("assistant-option-$index"),
                        enabled = !history.busy && !state.sending,
                    ) {
                        Text(option.label)
                    }
                }
                state.error?.let { Text(it, Modifier.testTag("assistant-error"), color = Color(0xFFB3261E)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        state.draft,
                        { state.draft = it },
                        Modifier.weight(1f).testTag("assistant-text"),
                        placeholder = { Text("درخواست خود را بنویسید") },
                        enabled = !history.busy && !state.sending,
                    )
                    Button(
                        {
                            focus.clearFocus()
                            val text = state.draft.trim()
                            if (text.isNotEmpty()) {
                                state.draft = ""
                                act(state, scope) { send(state, text, null) }
                            }
                        },
                        Modifier.padding(start = 8.dp).testTag("assistant-send"),
                        enabled = !history.busy && !state.sending,
                    ) {
                        Text("ارسال")
                    }
                }
            }
        }
    }
}

/** One message: the user's on one side, the assistant's on the other. */
@Composable
private fun Line(line: PanelLine) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            line.text,
            Modifier.align(if (line.fromUser) Alignment.CenterStart else Alignment.CenterEnd)
                .background(
                    if (line.fromUser) Color(0xFFE7F0FB) else Color(0xFFEEF6F0),
                    RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
        )
    }
}

/** Sends the user's words, or an option of the pending question. */
private fun send(state: AssistantPanelState, text: String?, option: PanelOption?) {
    AssistantClient.send(state.conversationId, text, option, state.history.questionId)
}

/** Runs one call off the main thread, then reloads the conversation; a failure is shown, not thrown. */
private fun act(state: AssistantPanelState, scope: CoroutineScope, call: () -> Unit) {
    scope.launch {
        state.sending = true
        state.error = null
        try {
            withContext(Dispatchers.IO) { call() }
        } catch (error: IOException) {
            state.error = "انجام نشد: ${error.message}"
        } catch (error: IllegalStateException) {
            state.error = "اتصال برنامه به دستیار هنوز برقرار نشده است."
        } finally {
            refresh(state)
            state.sending = false
        }
    }
}

/** Reloads the conversation from the backend; a failure leaves what is shown. */
private suspend fun refresh(state: AssistantPanelState) {
    try {
        state.history = withContext(Dispatchers.IO) { AssistantClient.history(state.conversationId) }
    } catch (error: IOException) {
        state.error = "دستیار در دسترس نیست."
    }
}

private const val POLL_MS = 700L
