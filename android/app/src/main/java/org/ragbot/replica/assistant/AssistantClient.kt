package org.ragbot.replica.assistant

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.ragbot.replica.BuildConfig
import org.ragbot.replica.Json
import org.ragbot.replica.bridge.BridgeConfig
import org.ragbot.replica.bridge.ContractJson
import org.ragbot.replica.bridge.Observation

/*
 * The assistant panel's three calls of contracts/openapi/assistant_mobile.json: a message (or an
 * option), the conversation's history, and cancel. Called by AssistantPanel, off the main thread.
 * The address is the build's development address. It carries no credentials, and nothing it sends
 * can confirm, submit or pay: the backend only prepares, and the user confirms on the app's review.
 */
internal data class PanelLine(val fromUser: Boolean, val text: String)

internal data class PanelOption(val ref: String, val label: String)

/** What the panel shows: the messages, the pending question or the task, and the run's progress. */
internal data class PanelHistory(
    val busy: Boolean = false,
    val lines: List<PanelLine> = emptyList(),
    val questionId: String? = null,
    val options: List<PanelOption> = emptyList(),
    val taskState: String? = null,
    val steps: List<String> = emptyList(),
) {
    /** The state to show: a pending question means the task waits for the user. */
    val shownState
        get() = if (questionId != null) "clarifying" else taskState
}

internal object AssistantClient {
    /** POST /assistant/v1/messages with the user's words, or the option the user picked. */
    fun send(conversationId: String, message: String?, option: PanelOption?, questionId: String?) {
        val seen = checkNotNull(lastObservation()) { "The bridge loop has not observed the app yet" }
        call("POST", "/assistant/v1/messages", requestBody(conversationId, message, option, questionId, seen))
    }

    /** GET /assistant/v1/history for the conversation. */
    fun history(conversationId: String): PanelHistory =
        parseHistory(call("GET", "/assistant/v1/history?conversation_id=$conversationId", null))

    /** POST /assistant/v1/sessions/{session_id}/cancel. */
    fun cancel() {
        call("POST", "/assistant/v1/sessions/${BridgeConfig.DEVICE_ID}/cancel", null)
    }

    /** The loop's latest observation, or null before the loop has reported. */
    fun lastObservation(): Observation? = org.ragbot.replica.bridge.CommandPoller.lastObservation

    /** A conversation request as the contract writes it: exactly one of message or selection. */
    fun requestBody(
        conversationId: String,
        message: String?,
        option: PanelOption?,
        questionId: String?,
        observation: Observation,
    ): String {
        val q = ContractJson::quote
        val said =
            if (option != null) "\"selection\":{\"question_id\":${q(questionId.orEmpty())},\"ref\":${q(option.ref)}}"
            else "\"message\":${q(checkNotNull(message))}"
        return listOf(
                "\"request_id\":${q(UUID.randomUUID().toString())}",
                "\"conversation_id\":${q(conversationId)}",
                "\"session_id\":${q(BridgeConfig.DEVICE_ID)}",
                "\"device_id\":${q(BridgeConfig.DEVICE_ID)}",
                "\"user_context\":{\"user_ref\":\"synthetic-user\",\"locale\":\"fa-IR\"}",
                said,
                "\"observation\":${ContractJson.write(observation)}",
            )
            .joinToString(",", "{", "}")
    }

    /** Reads the history reply: messages, the current question or task state, and the steps. */
    fun parseHistory(reply: String): PanelHistory {
        val root = Json.parse("history", reply)
        val current = root["current"]
        val question = !current.isNull && current.str("kind") == "clarification"
        return PanelHistory(
            busy = root.bool("busy"),
            lines = root.list("messages") { PanelLine(it.str("from") == "user", it.str("text")) },
            questionId = if (question) current.str("question_id") else null,
            options =
                if (question) current.list("options") { PanelOption(it.str("ref"), it.str("label")) }
                else emptyList(),
            taskState = if (!current.isNull && current.has("task_state")) current.str("task_state") else null,
            steps = root.list("steps") { "${it.str("operation")} → ${it.str("screen")}: ${it.str("outcome")}" },
        )
    }

    /** One HTTP call; any answer but 200 is an IOException naming the backend's reason. */
    private fun call(method: String, path: String, body: String?): String {
        val connection = URL(BuildConfig.BRIDGE_BACKEND + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            if (connection.responseCode != 200) {
                val reason = connection.errorStream?.use { it.readBytes().decodeToString() }.orEmpty()
                throw IOException("HTTP ${connection.responseCode} $reason")
            }
            return connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }
    }

    private const val TIMEOUT_MS = 10_000
}
