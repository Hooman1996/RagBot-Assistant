package org.ragbot.replica.bridge

import org.junit.Assert.assertEquals

/**
 * The fake in-process command source for device tests. It builds each command from a fresh
 * observation, as the backend does, and hands it to the real Executor on the real screens.
 */
internal class BridgeRig(patienceMs: Long = 3_000) {
    val controls = ComposeControls()
    val executor = Executor(controls, BridgeConfig.DEVICE_ID, patienceMs = patienceMs)
    private var count = 0

    fun run(
        operation: String,
        destination: String? = null,
        form: String? = null,
        values: Map<String, FieldValue> = emptyMap(),
        arguments: Map<String, String> = emptyMap(),
    ): CommandResult {
        val seen = executor.observer.observe()
        count += 1
        val draft = if (operation in DRAFT_OPERATIONS) executor.observer.draftVersion else null
        return executor.execute(
            Command(
                1,
                1,
                "task-1",
                1,
                "cmd-$count",
                BridgeConfig.DEVICE_ID,
                BridgeConfig.DEVICE_ID,
                operation,
                seen.observation_version,
                draft,
                "2100-01-01T00:00:00Z",
                destination,
                form,
                values,
                arguments,
            )
        )
    }

    fun navigate(destination: String, vararg arguments: Pair<String, String>) =
        run("navigate", destination = destination, arguments = arguments.toMap())

    fun review() = run("show_review")

    /** Moves to [screen], choosing rows by [arguments], and checks the observation agrees. */
    fun reach(screen: String, vararg arguments: Pair<String, String>) =
        expect(navigate(screen, *arguments), screen)

    /** Fills [form], which is on screen, and checks the fill. */
    fun fill(form: String, vararg values: Pair<String, FieldValue>) =
        expect(run("fill_form", form = form, values = values.toMap()), form)

    fun expect(result: CommandResult, screen: String): CommandResult {
        assertEquals(result.toString(), "succeeded", result.outcome)
        assertEquals(result.toString(), screen, result.observation.screen)
        return result
    }

    /** Plays the user: a tap the executor itself may never make. */
    fun userTaps(tag: String) = controls.click(tag)

    private companion object {
        val DRAFT_OPERATIONS = setOf("fill_form", "show_review")
    }
}

internal fun text(value: String) = FieldValue.Text(value)

internal fun rials(value: Long) = FieldValue.Money(value)
