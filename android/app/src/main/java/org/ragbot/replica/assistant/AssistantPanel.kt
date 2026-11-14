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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * The assistant panel: a floating «دستیار» bubble the user can drag anywhere opens and closes a small
 * chat box (so does «دستیار» in the support menu). The box steps aside while the agent drives the
 * app and comes back when it is done; cancel is a separate control, and it closes the box.
 * It lives inside the app's own window, never in a dialog, so the executor still sees and taps the
 * screen behind it. It catches no Back press (the executor uses Back) and shows no spinner (an
 * indeterminate one reads as loading). It keeps the conversation only in memory, and it has no way
 * to confirm, submit or pay: only the user confirms, on the app's own review.
 */
/** The chat box when open, the bubble always, and the polling while the agent works. */
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
            stepAside(state)
        }
        stepAside(state) // the turn ended: a box that stepped aside comes back
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (state.open) Panel(state, scope)
        Bubble(state)
    }
}

@Composable
private fun Panel(state: AssistantPanelState, scope: CoroutineScope) {
    val history = state.history
    val focus = LocalFocusManager.current
    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.45f).testTag("assistant-panel"),
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
                            {
                                state.open = false // out of the way: the app stays where it is
                                act(state, scope) { AssistantClient.cancel() }
                            },
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

/** Sends the user's words, or an option of the pending question, noting where the app was. */
private fun send(state: AssistantPanelState, text: String?, option: PanelOption?) {
    state.startScreen = AssistantClient.lastObservation()?.screen
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
