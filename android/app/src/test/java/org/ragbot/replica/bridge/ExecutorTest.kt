package org.ragbot.replica.bridge

import org.junit.Assert.*
import org.junit.Test

class ExecutorTest {
    private val controls = FakeControls()
    private val executor = Executor(controls, "device-1", { 1_000 }, patienceMs = 0)

    private fun version() = executor.observer.observe().observation_version

    private fun run(
        operation: String,
        id: String = "cmd-1",
        destination: String? = null,
        form: String? = null,
        values: Map<String, FieldValue> = emptyMap(),
        arguments: Map<String, String> = emptyMap(),
        draft: Long? = null,
    ) =
        executor.execute(
            command(operation, id, destination, form, values, arguments, version(), draft)
        )

    @Test
    fun navigateFollowsTheEdgesAndReportsEachStep() {
        val result = run("navigate", destination = "S-192")
        assertEquals("succeeded", result.outcome)
        assertEquals("S-192", result.observation.screen)
        assertEquals(listOf("S-026", "S-193"), result.trace.map { it.from })
        assertEquals("menu-transfer", result.trace.first().action)
    }

    @Test
    fun navigateGoesBackWhenNoForwardRouteExists() {
        controls.screen = "S-207"
        val result = run("navigate", destination = "S-192")
        assertEquals("succeeded", result.outcome)
        assertEquals("back", result.trace.first().action)
        assertEquals(1, controls.pressedBack)
    }

    @Test
    fun navigateStopsWhereARowKeyIsMissing() {
        val result = run("navigate", destination = "S-284")
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("missing_argument"), result.needs)
        assertEquals("S-283", result.observation.screen)
    }

    @Test
    fun navigateAppliesItsArgumentAtTheRowStep() {
        controls.rows = setOf("charity-1")
        val result = run("navigate", destination = "S-284", arguments = mapOf("charity" to "1"))
        assertEquals("succeeded", result.outcome)
        assertEquals("S-284", result.observation.screen)
        assertEquals(listOf("charity-1"), controls.tapped.takeLast(1))
    }

    @Test
    fun navigateReportsAnUnexpectedScreenAndDecidesNothing() {
        controls.screen = "S-174"
        controls.landOn = "S-093"
        val result = run("navigate", destination = "S-176")
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("unexpected_screen"), result.needs)
        assertEquals("S-093", result.observation.screen)
    }

    @Test
    fun navigateIsBlockedWhenOnlyTheUserCanReachTheScreen() {
        val result = run("navigate", destination = "S-093")
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("no_executor_route"), result.needs)
        assertTrue(controls.tapped.isEmpty())
    }

    @Test
    fun anOpenDialogStopsNavigation() {
        controls.modal = "notice"
        val result = run("navigate", destination = "S-192")
        assertEquals(listOf("close_dialog"), result.needs)
        assertTrue(controls.tapped.isEmpty())
    }

    @Test
    fun aDisabledControlIsNeverFired() {
        controls.disabled = setOf("menu-transfer")
        val result = run("navigate", destination = "S-192")
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("control_disabled:menu-transfer"), result.needs)
        assertTrue(controls.tapped.isEmpty())
    }

    @Test
    fun fillFormTypesThroughTheFieldAndRaisesTheDraftVersion() {
        controls.screen = "S-174"
        controls.extra = setOf("card-transfer-amount")
        val result =
            run(
                "fill_form",
                form = "S-174",
                values = mapOf("card-transfer-amount" to FieldValue.Money(250_000)),
                draft = 0,
            )
        assertEquals("succeeded", result.outcome)
        assertEquals("250000", controls.typed["card-transfer-amount"])
        assertEquals(1L, result.draft_version)
        assertEquals(1L, result.observation.draft?.draft_version)
    }

    @Test
    fun fillFormNeedsTheFormOnScreen() {
        val result =
            run(
                "fill_form",
                form = "S-174",
                values = mapOf("card-transfer-amount" to FieldValue.Money(1)),
                draft = 0,
            )
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("navigate"), result.needs)
    }

    @Test
    fun showReviewOpensTheReviewAndStops() {
        controls.screen = "S-174"
        val result = run("show_review", draft = 0)
        assertEquals("succeeded", result.outcome)
        assertEquals("S-176", result.observation.screen)
        assertEquals(listOf("card-transfer-review"), controls.tapped)
        // A second show_review on the review page taps nothing, least of all the confirm.
        assertEquals("succeeded", run("show_review", "cmd-2", draft = 0).outcome)
        assertEquals(listOf("card-transfer-review"), controls.tapped)
    }

    @Test
    fun aValidationErrorStopsOnTheFormAndNamesTheField() {
        controls.screen = "S-174"
        controls.extra = setOf("card-transfer-amount")
        controls.typed["card-transfer-amount"] = ""
        controls.errors = setOf("card-transfer-review", "card-transfer-amount")
        val result = run("show_review", draft = 0)
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("card-transfer-amount"), result.needs)
        assertEquals("S-174", result.observation.screen)
    }

    @Test
    fun aStaleObservationIsRejectedWithoutTouchingTheScreen() {
        val stale = version() + 5
        val result =
            executor.execute(
                command("navigate", destination = "S-192", observation = stale)
            )
        assertEquals("rejected_stale", result.outcome)
        assertEquals("stale_observation", result.error_code)
        assertTrue(controls.tapped.isEmpty())
    }

    @Test
    fun aStaleDraftIsRejected() {
        controls.screen = "S-174"
        val result = run("show_review", draft = 7)
        assertEquals("rejected_stale", result.outcome)
        assertEquals("stale_draft", result.error_code)
    }

    @Test
    fun anExpiredCommandIsRejected() {
        val result =
            executor.execute(
                command(
                    "navigate",
                    destination = "S-192",
                    observation = version(),
                    expires = "1970-01-01T00:00:00Z",
                )
            )
        assertEquals("rejected_expired", result.outcome)
        assertTrue(controls.tapped.isEmpty())
    }

    @Test
    fun aRepeatedCommandReturnsItsFirstResultAndRunsOnce() {
        val command = command("navigate", destination = "S-193", observation = version())
        val first = executor.execute(command)
        val tapped = controls.tapped.size
        assertSame(first, executor.execute(command))
        assertEquals(tapped, controls.tapped.size)
    }

    @Test
    fun aCommandForAnotherDeviceIsRefused() {
        val result =
            executor.execute(
                command(
                    "navigate",
                    destination = "S-192",
                    observation = version(),
                    device = "other",
                )
            )
        assertEquals("rejected_policy", result.outcome)
        assertEquals("wrong_device", result.error_code)
    }

    @Test
    fun cardAndIbanNumbersAreMasked() {
        assertEquals("****0007", Observer.mask("6037 0000 0000 0007"))
        assertEquals("250000", Observer.mask("250000"))
    }
}
