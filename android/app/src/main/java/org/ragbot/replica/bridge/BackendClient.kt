package org.ragbot.replica.bridge

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.ragbot.replica.BuildConfig

/*
 * The two calls of contracts/openapi/assistant_mobile.json that the loop uses.
 * Called by CommandPoller. The address is the build's development address, nothing else.
 * It carries no credentials and never calls a bank or a mock bank.
 * It logs one line per command and per result (tag RagBotBridge), never a field's value.
 */
internal interface Backend {
    /** Reports the app's state. The reply may hold the next command. */
    fun resume(observation: Observation): Command?

    /** Posts a command's result. The reply may hold the next command. */
    fun postResult(result: CommandResult): Command?
}

internal class BackendClient : Backend {
    /** POST /assistant/v1/sessions/{session_id}/resume. */
    override fun resume(observation: Observation) =
        logged(
            post(
                "/assistant/v1/sessions/${BridgeConfig.DEVICE_ID}/resume",
                ContractJson.write(observation),
            )
        )

    /** POST /assistant/v1/results. */
    override fun postResult(result: CommandResult): Command? {
        val trace = result.trace.joinToString(" ") { "${it.from}→${it.to}" }
        val needs = result.needs.joinToString(",")
        Log.i(
            TAG,
            "result ${result.command_id.take(8)}: ${result.outcome} on ${result.observation.screen}" +
                (if (trace.isEmpty()) "" else " trace $trace") +
                (if (needs.isEmpty()) "" else " needs $needs") +
                (result.error_code?.let { " error_code $it" } ?: ""),
        )
        return logged(post("/assistant/v1/results", ContractJson.write(result)))
    }

    /** Logs a command the backend handed over: its ID, operation and target, never its values. */
    private fun logged(command: Command?): Command? {
        if (command != null) {
            Log.i(
                TAG,
                "command ${command.command_id.take(8)}: ${command.operation} " +
                    (command.destination ?: command.form ?: ""),
            )
        }
        return command
    }

    /** Posts JSON and reads the command in the reply. Any non-200 is an IOException. */
    private fun post(path: String, body: String): Command? {
        val connection = URL(BuildConfig.BRIDGE_BACKEND + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
            val reply = connection.inputStream.use { it.readBytes().decodeToString() }
            return ContractJson.commandIn(reply)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000
        const val TAG = "RagBotBridge"
    }
}
