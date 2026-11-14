package org.ragbot.replica.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/*
 * What the assistant panel remembers while the app runs: whether the box is open, the conversation,
 * the bubble's place, and the task states in Persian. Memory only: nothing is stored on the phone.
 */
internal object AssistantPanelState {
    var available by mutableStateOf(false) // the bridge loop is on: a debug build with the flag file
    var open by mutableStateOf(false)
    var conversationId by mutableStateOf(UUID.randomUUID().toString())
    var history by mutableStateOf(PanelHistory())
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var bubbleX by mutableFloatStateOf(-1f) // where the user dragged the bubble, in pixels; -1: not yet
    var bubbleY by mutableFloatStateOf(-1f)
    var startScreen by mutableStateOf<String?>(null) // the app's screen when the last message went out
    var autoClosed by mutableStateOf(false) // the box closed itself while the agent drove the app
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
