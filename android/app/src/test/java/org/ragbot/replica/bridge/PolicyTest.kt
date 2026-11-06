package org.ragbot.replica.bridge

import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.ragbot.replica.EdgeKind
import org.ragbot.replica.HomeDestinations
import org.ragbot.replica.ReplicaGraph
import org.ragbot.replica.ReplicaProfile

/** Rule 4: no operation, route or argument reaches a user-only action or a secret field. */
class PolicyTest {
    private val secretFields =
        ReplicaProfile.forms.flatMap { form ->
            form.fields.filter { it.secret }.map { form.id to it.ref }
        }

    private fun concrete(pattern: String) = pattern.replace("*", "1")

    private fun executor(controls: FakeControls) =
        Executor(controls, "device-1", { 1_000 }, patienceMs = 0)

    private fun FakeControls.neverTouched(tag: String) {
        assertFalse("$tag was tapped", tag in tapped)
        assertFalse("$tag was typed", tag in typed)
    }

    @Test
    fun everyUserOnlyActionIsRefusedOnEveryScreen() {
        ReplicaProfile.userOnlyActions.map(::concrete).forEach { tag ->
            HomeDestinations.implemented.forEach { screen ->
                assertFalse("$tag on $screen", Policy.mayTap(tag, screen))
                assertFalse("$tag on $screen", Policy.mayType(tag, screen))
            }
        }
    }

    @Test
    fun tappingAUserOnlyActionNeverReachesTheControl() {
        ReplicaProfile.userOnlyActions.map(::concrete).forEach { tag ->
            val controls = FakeControls()
            controls.extra = setOf(tag)
            assertEquals(tag, "failed", Actions(controls, 0).tap(tag)?.outcome)
            controls.neverTouched(tag)
        }
    }

    @Test
    fun aControlNoEdgeOrFieldNamesIsRefused() {
        assertFalse(Policy.mayTap("anything-else", HomeDestinations.HOME))
        assertTrue(Policy.mayTap("menu-transfer", HomeDestinations.HOME))
    }

    @Test
    fun noRouteToAnyScreenCrossesAUserOnlyEdge() {
        val userOnly = ReplicaGraph.edges.filter { it.kind == EdgeKind.USER_ONLY }.toSet()
        HomeDestinations.implemented.forEach { to ->
            Routes.find(HomeDestinations.HOME, to).orEmpty().forEach { edge ->
                assertFalse("$edge", edge in userOnly)
                edge.action.split(" > ").forEach { assertFalse(it, Policy.isUserOnly(it)) }
            }
        }
    }

    @Test
    fun navigatingToAScreenBehindAUserStepNeverTapsThatStep() {
        val behind =
            ReplicaGraph.edges
                .filter { it.kind == EdgeKind.USER_ONLY }
                .map { it.to }
                .filter { Routes.find(HomeDestinations.HOME, it) == null }
                .distinct()
        assertTrue(behind.isNotEmpty())
        behind.forEach { to ->
            val controls = FakeControls()
            val executor = executor(controls)
            val version = executor.observer.observe().observation_version
            val result =
                executor.execute(command("navigate", destination = to, observation = version))
            assertEquals(to, "blocked", result.outcome)
            assertTrue(to, controls.tapped.isEmpty())
        }
    }

    @Test
    fun everySecretFieldIsRefusedBeforeTheScreenIsTouched() {
        assertTrue(secretFields.size > 10)
        secretFields.forEach { (form, ref) ->
            val controls = FakeControls(form)
            controls.extra = setOf(concrete(ref))
            val executor = executor(controls)
            val version = executor.observer.observe().observation_version
            val result =
                executor.execute(
                    command(
                        "fill_form",
                        form = form,
                        values = mapOf(concrete(ref) to FieldValue.Text("1234")),
                        observation = version,
                        draft = 0,
                    )
                )
            assertEquals(ref, "rejected_policy", result.outcome)
            assertEquals(ref, "field_not_fillable", result.error_code)
            controls.neverTouched(concrete(ref))
        }
    }

    @Test
    fun aFieldOutsideTheFormIsRefused() {
        val controls = FakeControls("S-174")
        val executor = executor(controls)
        val version = executor.observer.observe().observation_version
        val result =
            executor.execute(
                command(
                    "fill_form",
                    form = "S-174",
                    values = mapOf("otp-code" to FieldValue.Text("1234")),
                    observation = version,
                    draft = 0,
                )
            )
        assertEquals("rejected_policy", result.outcome)
    }

    @Test
    fun aChoiceValueCannotNameAConfirmControl() {
        // Each ref plus value would spell a user-only tag if the key were trusted.
        val controls = FakeControls("S-207")
        controls.extra = setOf("payment-submit", "bill-accept")
        assertEquals("failed", Actions(controls, 0).tap("bill-accept")?.outcome)
        assertFalse(Policy.isSafeKey("x > bill-accept"))
        assertFalse(Policy.isSafeKey("a b"))
        assertFalse(Policy.isSafeKey(""))
    }

    @Test
    fun aCandidateCannotReachAUserOnlyRow() {
        // S-159's rows are digital-*, and digital-confirm is user-only.
        val controls = FakeControls("S-159")
        controls.rows = setOf("digital-confirm")
        val executor = executor(controls)
        val version = executor.observer.observe().observation_version
        val result =
            executor.execute(
                command("select_candidate", ref = "confirm", observation = version)
            )
        assertEquals("failed", result.outcome)
        assertEquals("policy_refused", result.error_code)
        controls.neverTouched("digital-confirm")
    }

    @Test
    fun aCandidateWithStepsInItIsRefused() {
        val controls = FakeControls("S-283")
        val executor = executor(controls)
        val version = executor.observer.observe().observation_version
        val result =
            executor.execute(
                command("select_candidate", ref = "1 > payment-submit", observation = version)
            )
        assertEquals("rejected_policy", result.outcome)
    }

    @Test
    fun anUnknownOperationIsRefused() {
        listOf("submit", "confirm", "authorize", "pay").forEach { operation ->
            val controls = FakeControls("S-176")
            val executor = executor(controls)
            val version = executor.observer.observe().observation_version
            val result = executor.execute(command(operation, observation = version))
            assertEquals(operation, "rejected_policy", result.outcome)
            assertTrue(controls.tapped.isEmpty())
        }
    }

    @Test
    fun showReviewStopsOnEveryReviewPage() {
        Reviews.pages.forEach { page ->
            val controls = FakeControls(page)
            val executor = executor(controls)
            val version = executor.observer.observe().observation_version
            val result = executor.execute(command("show_review", observation = version))
            assertEquals(page, "succeeded", result.outcome)
            assertTrue(page, controls.tapped.isEmpty())
        }
    }

    @Test
    fun thisPageOfTheDocsListsEveryUserOnlyAction() {
        val page = File("../../docs/bridge/safety.md").readText()
        ReplicaProfile.userOnlyActions.forEach { assertTrue(it, "`$it`" in page) }
    }

    @Test
    fun aSecretValueIsNeverObserved() {
        val controls = FakeControls("S-176")
        controls.typed["card-transfer-cvv2"] = "123"
        val fields = Executor(controls, "device-1").observer.observe().fields
        assertFalse(fields.any { it.ref == "card-transfer-cvv2" })
    }
}
