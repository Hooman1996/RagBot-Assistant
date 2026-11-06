package org.ragbot.replica.bridge

import org.ragbot.replica.Json

/*
 * Reads commands and writes observations and results, as contracts/schemas define them.
 * Called by BackendClient. Reading uses the app's small JSON reader, which accepts only whole
 * numbers, so a fractional amount cannot get in. Writing quotes every string.
 */
internal object ContractJson {
    /** The command in a backend reply, or null when the reply carries none. */
    fun commandIn(reply: String): Command? {
        val root = Json.parse("reply", reply)
        return if (root.str("kind") == "command") command(root["command"]) else null
    }

    /** Reads the command object, with its operation's arguments. */
    private fun command(json: Json): Command {
        val arguments = json["arguments"]
        return Command(
            json.long("protocol_version"),
            json.long("profile_version"),
            json.str("task_id"),
            json.long("task_revision"),
            json.str("command_id"),
            json.str("device_id"),
            json.str("session_id"),
            json.str("operation"),
            json.long("expected_observation_version"),
            if (json.has("expected_draft_version")) json.long("expected_draft_version") else null,
            json.str("expires_at"),
            if (arguments.has("destination")) arguments.str("destination") else null,
            if (arguments.has("form")) arguments.str("form") else null,
            if (arguments.has("values")) arguments["values"].map(::fieldValue) else emptyMap(),
            if (arguments.has("ref")) arguments.str("ref") else null,
        )
    }

    /** A fill value: plain text, or money in whole rials. */
    private fun fieldValue(json: Json): FieldValue {
        if (!json.has("value")) return FieldValue.Text(json.string())
        check(json.str("unit") == "IRR") { "Money must be in IRR" }
        return FieldValue.Money(json.long("value"))
    }

    /** The observation as contract JSON. */
    fun write(observation: Observation): String {
        val parts =
            listOf(
                "\"observation_version\":${observation.observation_version}",
                "\"app_version\":${observation.app_version}",
                "\"profile_version\":${observation.profile_version}",
                "\"screen\":${quote(observation.screen)}",
                "\"modal\":${observation.modal?.let(::quote) ?: "null"}",
                "\"loading\":${observation.loading}",
                "\"fields\":${array(observation.fields.map(::write))}",
                "\"draft\":${observation.draft?.let(::write) ?: "null"}",
                "\"supported_operations\":${array(observation.supported_operations.map(::quote))}",
            )
        return parts.joinToString(",", "{", "}")
    }

    /** The result as contract JSON. Optional fields appear only when they have a value. */
    fun write(result: CommandResult): String {
        val parts =
            mutableListOf(
                "\"command_id\":${quote(result.command_id)}",
                "\"outcome\":${quote(result.outcome)}",
                "\"postcondition\":${result.postcondition?.let(::quote) ?: "null"}",
                "\"observation\":${write(result.observation)}",
                "\"draft_version\":${result.draft_version}",
            )
        if (result.needs.isNotEmpty()) parts += "\"needs\":${array(result.needs.map(::quote))}"
        result.error_code?.let { parts += "\"error_code\":${quote(it)}" }
        if (result.trace.isNotEmpty()) parts += "\"trace\":${array(result.trace.map(::write))}"
        return parts.joinToString(",", "{", "}")
    }

    /** One observed field as JSON. */
    private fun write(field: ObservedField) =
        "{\"ref\":${quote(field.ref)},\"value\":${quote(field.value)}}"

    /** The draft as JSON. */
    private fun write(draft: ObservedDraft) =
        "{\"draft_id\":${quote(draft.draft_id)},\"draft_version\":${draft.draft_version}," +
            "\"review_version\":${draft.review_version}}"

    /** One trace step as JSON. */
    private fun write(step: TraceStep) =
        "{\"from\":${quote(step.from)},\"to\":${quote(step.to)},\"action\":${quote(step.action)}}"

    /** Items that are already JSON, as a JSON array. */
    private fun array(items: List<String>) = items.joinToString(",", "[", "]")

    /** [text] as a JSON string. */
    private fun quote(text: String): String {
        val body = StringBuilder()
        for (ch in text) {
            when {
                ch == '"' -> body.append("\\\"")
                ch == '\\' -> body.append("\\\\")
                ch < ' ' -> body.append("\\u%04x".format(ch.code))
                else -> body.append(ch)
            }
        }
        return "\"$body\""
    }
}
