package org.ragbot.replica

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ReplicaProfileTest {
    private val exported =
        ReplicaGraph.edges.filter { it.kind == EdgeKind.REAL || it.kind == EdgeKind.DEMO }

    private fun quote(value: String) = "\"" + value.replace("\"", "\\\"") + "\""

    private fun strings(values: List<String>) = values.joinToString(", ", "[", "]") { quote(it) }

    private fun block(items: List<String>) = items.joinToString(",\n", "[\n", "\n  ]")

    private fun profileJson(): String {
        val screens = HomeDestinations.implemented.sorted().map { "    ${quote(it)}" }
        val edges =
            exported.map {
                "    {\"from\": ${quote(it.from)}, \"to\": ${quote(it.to)}, " +
                    "\"action\": ${quote(it.action)}}"
            }
        val forms =
            ReplicaProfile.forms.map { form ->
                val fields =
                    form.fields.joinToString(", ") {
                        val secret = if (it.secret) ", \"secret\": true" else ""
                        "{\"ref\": ${quote(it.ref)}, \"type\": ${quote(it.type)}$secret}"
                    }
                "    {\"id\": ${quote(form.id)}, \"fields\": [$fields]}"
            }
        val features =
            ReplicaProfile.features.map {
                "    {\"intent\": ${quote(it.intent)}, " +
                    "\"description\": ${quote(it.description)}, " +
                    "\"required_slots\": ${strings(it.requiredSlots)}, " +
                    "\"lookups\": ${strings(it.lookups)}, " +
                    "\"destination\": ${quote(it.destination)}, " +
                    "\"form\": ${it.form?.let(::quote) ?: "null"}}"
            }
        val userOnly = ReplicaProfile.userOnlyActions.map { "    ${quote(it)}" }
        return "{\n  \"profile_version\": 1,\n" +
            "  \"screens\": ${block(screens)},\n" +
            "  \"edges\": ${block(edges)},\n" +
            "  \"forms\": ${block(forms)},\n" +
            "  \"features\": ${block(features)},\n" +
            "  \"user_only_actions\": ${block(userOnly)}\n}\n"
    }

    private fun reachable(edges: List<Edge>): Set<String> {
        val seen = mutableSetOf(HomeDestinations.HOME)
        var size = 0
        while (seen.size != size) {
            size = seen.size
            edges.filter { it.from in seen }.forEach { seen += it.to }
        }
        return seen
    }

    private fun tags(action: String) = action.split(" > ")

    private fun matches(pattern: String, tag: String) =
        Regex(pattern.split("*").joinToString(".*") { Regex.escape(it) }).matches(tag)

    @Test
    fun committedProfileMatchesAFreshExport() {
        val fresh = profileJson()
        val build = File("build/replica-profile").apply { mkdirs() }
        File(build, "replica.profile.json").writeText(fresh)
        // Every edge with its kind, for the owner's validation sheet.
        File(build, "edges.tsv")
            .writeText(
                ReplicaGraph.edges.joinToString("") {
                    "${it.from}\t${it.to}\t${it.action}\t${it.kind}\n"
                }
            )
        val committed = File("../../app_profiles/replica.profile.json")
        if (System.getenv("UPDATE_REPLICA_PROFILE") != null) committed.writeText(fresh)
        assertEquals(committed.readText(), fresh)
    }

    @Test
    fun everyRegisteredScreenIsDeclaredAndReachableFromHome() {
        val declared = ReplicaGraph.edges.flatMap { listOf(it.from, it.to) }.toSet()
        assertEquals(HomeDestinations.implemented, declared)
        assertEquals(HomeDestinations.implemented, reachable(ReplicaGraph.edges))
    }

    @Test
    fun theExecutorReachesEveryFeatureWithoutAUserOnlyStep() {
        val seen = reachable(exported)
        ReplicaProfile.features.forEach {
            assertTrue(it.intent, it.destination in seen)
            it.form?.let { form -> assertTrue(form, form in seen) }
        }
        // These sit behind the user's OTP, card secrets or payment step.
        listOf("S-242", "S-205", "S-215", "S-228", "S-081").forEach { assertFalse(it, it in seen) }
    }

    @Test
    fun everyServiceHasAFeature() {
        val intents = ReplicaProfile.features.map { it.intent }
        ServiceCatalog.flows
            .filterNot { it.key.endsWith("-detail") }
            .forEach { assertTrue(it.key, it.key.replace('-', '_') in intents) }
    }

    @Test
    fun noExportedEdgeUsesAUserOnlyAction() {
        exported.forEach { edge ->
            tags(edge.action).forEach { tag ->
                ReplicaProfile.userOnlyActions.forEach {
                    assertFalse("$tag is $it", matches(it, tag))
                }
            }
        }
    }

    @Test
    fun demoEdgesAreLabelledAndUndeclaredMovesAreRefused() {
        ReplicaGraph.edges
            .filter { it.kind == EdgeKind.DEMO && it.from == HomeDestinations.HOME }
            .forEach { assertTrue(it.action, it.action.startsWith("demo-menu > demo-path-")) }
        assertFalse(ReplicaGraph.declared("S-026", "S-176"))
        assertFalse(ReplicaGraph.declared("S-026", "S-097"))
    }

    @Test
    fun ownerTracedScreensHaveARealRouteAndNoHomeDemoEdge() {
        val traced =
            tracedHomeTiles.values +
                listOf("S-187", "S-178", "S-180", "S-191", "S-192", "S-338", "S-307", "S-071") +
                listOf("S-463", "S-175", "S-213", "S-202", "S-203", "S-204", "S-066", "S-130") +
                listOf("S-076", "S-078", "S-079", "S-080", "S-082", "S-151", "S-254", "S-241")
        val real = reachable(ReplicaGraph.edges.filter { it.kind == EdgeKind.REAL })
        val demo =
            ReplicaGraph.edges
                .filter { it.kind == EdgeKind.DEMO && it.from == HomeDestinations.HOME }
                .map { it.to }
        traced.forEach {
            assertTrue(it, it in real)
            assertFalse(it, it in demo)
        }
    }

    @Test
    fun independentReceiptsAreNeverFeatures() {
        val receipts =
            setOf("S-092", "S-093", "S-094", "S-095", "S-096", "S-097", "S-098") +
                setOf("S-113", "S-133", "S-261", "S-405")
        ReplicaProfile.features.forEach { assertFalse(it.intent, it.destination in receipts) }
    }

    @Test
    fun everyTagInTheProfileExistsInTheAppSource() {
        val source =
            File("src/main/java")
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filterNot {
                    it.name.startsWith("ReplicaGraph") || it.name.startsWith("ReplicaProfile")
                }
                .joinToString("\n") { it.readText() }
        fun known(tag: String): Boolean {
            var prefix = tag.substringBefore('*')
            while (prefix.length > 3) {
                if ("\"$prefix" in source) return true
                prefix = prefix.dropLast(1).substringBeforeLast('-', "") + "-"
                if (prefix == "-") return false
            }
            return false
        }
        val all =
            ReplicaGraph.edges.flatMap { tags(it.action) } +
                ReplicaProfile.forms.flatMap { form -> form.fields.map { it.ref } } +
                ReplicaProfile.userOnlyActions
        all.forEach { assertTrue(it, known(it)) }
    }
}
