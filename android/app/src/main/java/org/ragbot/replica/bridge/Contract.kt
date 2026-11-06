package org.ragbot.replica.bridge

/*
 * The command, observation and result of contracts/schemas, as Kotlin data.
 * Used by every bridge file. Property names match the contract's JSON names on purpose, so this
 * code reads against contracts/. It holds no logic and can never carry a secret field's value.
 */

/** The only operations the executor accepts. None of them submits or authorizes. */
internal val SUPPORTED_OPERATIONS =
    listOf("observe", "navigate", "fill_form", "select_candidate", "show_review")

/** A value in fill_form: plain text, or a whole number of rials. */
internal sealed interface FieldValue {
    data class Text(val text: String) : FieldValue

    data class Money(val value: Long) : FieldValue
}

/** One bounded operation from the backend. Only the fields of [operation] are set. */
internal data class Command(
    val protocol_version: Long,
    val profile_version: Long,
    val task_id: String,
    val task_revision: Long,
    val command_id: String,
    val device_id: String,
    val session_id: String,
    val operation: String,
    val expected_observation_version: Long,
    val expected_draft_version: Long?,
    val expires_at: String,
    val destination: String? = null,
    val form: String? = null,
    val values: Map<String, FieldValue> = emptyMap(),
    val ref: String? = null,
)

internal data class ObservedField(val ref: String, val value: String)

internal data class ObservedDraft(
    val draft_id: String,
    val draft_version: Long,
    val review_version: Long,
)

/** What the app shows right now. Field values are masked and never secret. */
internal data class Observation(
    val observation_version: Long,
    val app_version: Long,
    val profile_version: Long,
    val screen: String,
    val modal: String?,
    val loading: Boolean,
    val fields: List<ObservedField>,
    val draft: ObservedDraft?,
    val supported_operations: List<String> = SUPPORTED_OPERATIONS,
)

internal data class TraceStep(val from: String, val to: String, val action: String)

/** What the executor verified after a command, with a fresh observation. */
internal data class CommandResult(
    val command_id: String,
    val outcome: String,
    val postcondition: String?,
    val observation: Observation,
    val draft_version: Long,
    val needs: List<String> = emptyList(),
    val error_code: String? = null,
    val trace: List<TraceStep> = emptyList(),
)

/** How an operation ended, before the executor adds the fresh observation. */
internal data class Ending(
    val outcome: String,
    val postcondition: String? = null,
    val needs: List<String> = emptyList(),
    val error_code: String? = null,
)

internal fun succeeded(postcondition: String) = Ending("succeeded", postcondition)

internal fun blocked(vararg needs: String) = Ending("blocked", needs = needs.toList())

internal fun failed(error_code: String) = Ending("failed", error_code = error_code)
