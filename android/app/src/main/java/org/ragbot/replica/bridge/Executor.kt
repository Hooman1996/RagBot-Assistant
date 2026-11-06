package org.ragbot.replica.bridge

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.ragbot.replica.HomeDestinations

/*
 * Runs one command from start to end, following contracts/transitions/command.json.
 * Called by CommandPoller, and by tests with a fake Controls.
 * It rejects stale, expired, duplicate and refused commands before touching the screen, and keeps
 * a small journal so a repeated command returns its first result instead of running twice.
 * It has no operation that submits, confirms or pays, and must never gain one.
 */
internal class Executor(
    private val controls: Controls,
    private val device_id: String,
    private val now: () -> Long = System::currentTimeMillis,
    patienceMs: Long = 3_000,
) {
    private val actions = Actions(controls, patienceMs)
    private val navigator = Navigator(actions)
    private val filler = FormFiller(actions, navigator)
    val observer = Observer(controls)
    private val journal = LinkedHashMap<String, CommandResult>()

    /** Runs [command] once and returns its result, or the result of its first run. */
    fun execute(command: Command): CommandResult {
        journal[command.command_id]?.let {
            return it
        }
        val result = report(command, rejection(command) ?: run(command))
        journal[command.command_id] = result
        if (journal.size > JOURNAL_SIZE) journal.remove(journal.keys.first())
        return result
    }

    /** The reason to refuse [command] without running it, or null to run it. */
    private fun rejection(command: Command): Ending? {
        policyProblem(command)?.let {
            return Ending("rejected_policy", error_code = it)
        }
        if (expired(command.expires_at)) return Ending("rejected_expired", error_code = "expired")
        val seen = observer.observe()
        if (command.expected_observation_version != seen.observation_version)
            return Ending("rejected_stale", error_code = "stale_observation")
        val draft = command.expected_draft_version
        if (draft != null && draft != observer.draftVersion)
            return Ending("rejected_stale", error_code = "stale_draft")
        return null
    }

    /** Why [command] may never run, whatever the screen shows, or null. */
    private fun policyProblem(command: Command): String? {
        if (command.device_id != device_id) return "wrong_device"
        if (command.protocol_version != PROTOCOL_VERSION) return "wrong_protocol"
        if (command.profile_version != Observer.PROFILE_VERSION) return "wrong_profile"
        return when (command.operation) {
            "observe",
            "show_review" -> null
            "navigate" ->
                if (command.destination in screens) null else "unknown_screen"
            "fill_form" -> fillProblem(command)
            "select_candidate" ->
                if (Policy.isSafeKey(command.ref.orEmpty())) null else "invalid_candidate"
            else -> "unknown_operation"
        }
    }

    /** Every field must belong to the form and be one the executor may fill. */
    private fun fillProblem(command: Command): String? {
        val form = command.form.orEmpty()
        val allowed = command.values.keys.all { Policy.fillable(form, it) != null }
        return if (allowed && command.values.isNotEmpty()) null else "field_not_fillable"
    }

    /** Performs the operation. Only here can a command touch the screen. */
    private fun run(command: Command): Ending =
        try {
            when (command.operation) {
                "navigate" -> navigator.navigate(checkNotNull(command.destination))
                "select_candidate" -> navigator.selectCandidate(checkNotNull(command.ref))
                "fill_form" -> fill(command)
                "show_review" -> review()
                else -> succeeded("observed")
            }
        } catch (_: IllegalStateException) {
            // The app may or may not have moved: only a fresh observation can tell.
            Ending("uncertain", error_code = "executor_error")
        }

    /** Fills the form and records the new draft version. */
    private fun fill(command: Command): Ending {
        val ending = filler.fill(checkNotNull(command.form), command.values)
        if (ending.outcome == "succeeded") observer.filled()
        return ending
    }

    /** Opens the review and records which draft version it shows. */
    private fun review(): Ending {
        val ending = filler.showReview()
        if (ending.outcome == "succeeded") observer.reviewed()
        return ending
    }

    /** Adds the fresh observation that every result must carry. */
    private fun report(command: Command, ending: Ending): CommandResult {
        val moved = command.operation == "navigate" || command.operation == "select_candidate"
        val observation = observer.observe()
        return CommandResult(
            command.command_id,
            ending.outcome,
            ending.postcondition,
            observation,
            observer.draftVersion,
            ending.needs,
            ending.error_code,
            if (moved) navigator.trace else emptyList(),
        )
    }

    /** True at or after the command's UTC expiry. An unreadable time counts as expired. */
    private fun expired(expires_at: String): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            checkNotNull(format.parse(expires_at)).time <= now()
        } catch (_: ParseException) {
            true
        }
    }

    private companion object {
        const val PROTOCOL_VERSION = 1L
        const val JOURNAL_SIZE = 50
        val screens = HomeDestinations.implemented
    }
}
