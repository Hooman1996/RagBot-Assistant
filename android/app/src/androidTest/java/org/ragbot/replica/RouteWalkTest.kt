package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Walks every plain route through Home and the menu pages and checks the screen it opens. */
@RunWith(AndroidJUnit4::class)
class RouteWalkTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val menus =
        setOf(HomeDestinations.HOME, "D-001", "D-002") +
            setOf("S-193", "S-180", "S-324", "S-201", "S-111", "S-110", "S-147", "S-253") +
            setOf("S-025", "S-174", "S-211", "S-403")

    // These need a filled form, so a plain walk stops before them.
    private val formSteps = listOf("-review", "-continue", "-calculate", "-submit")

    private fun routes(): Map<String, List<Edge>> {
        val found = mutableMapOf(HomeDestinations.HOME to emptyList<Edge>())
        var queue = listOf(HomeDestinations.HOME)
        while (queue.isNotEmpty()) {
            val next = mutableListOf<String>()
            for (from in queue.filter { it in menus }) {
                ReplicaGraph.edges
                    .filter { it.from == from && it.kind == EdgeKind.REAL && "*" !in it.action }
                    .filter { edge -> formSteps.none { it in edge.action } }
                    .filter { it.to !in found }
                    .forEach {
                        found[it.to] = found.getValue(from) + it
                        next += it.to
                    }
            }
            queue = next
        }
        return found - HomeDestinations.HOME
    }

    private fun tap(tag: String) {
        compose.waitForIdle()
        val node = compose.onNodeWithTag(tag)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
            // Not inside a scrolling parent, as in a dialog.
        }
        node.performClick()
    }

    @Test
    fun everyPlainRouteOpensAWorkingScreen() {
        val all = routes()
        check(all.size > 50) { "Only ${all.size} routes" }
        all.forEach { (screen, path) ->
            ActivityScenario.launch(MainActivity::class.java).use {
                tap("demo-entry")
                path.flatMap { it.action.split(" > ") }.forEach(::tap)
                compose.waitForIdle()
                compose.onNodeWithTag(screen).assertExists("$screen by ${path.map { it.action }}")
                compose.onNodeWithTag("page-back").assertExists()
            }
        }
    }
}
