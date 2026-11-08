package org.ragbot.replica.bridge

import org.ragbot.replica.Edge
import org.ragbot.replica.EdgeKind
import org.ragbot.replica.ProfileSelections
import org.ragbot.replica.ReplicaGraph

/*
 * How the replica's navigate gets to a screen: it walks the declared edges, as a user would tap.
 * Called by Navigator. This is the app's own business, and an app team may open a screen directly
 * instead. The assistant's plan comes from assistant/navigator.py: nothing here decides what is
 * required. User-only edges are not in this graph, so a walk can never contain one.
 */
internal object Routes {
    val exported = ReplicaGraph.edges.filter { it.kind == EdgeKind.REAL || it.kind == EdgeKind.DEMO }

    /** The argument names navigate accepts: the selections the edges declare. */
    val selections = exported.mapNotNull { ProfileSelections.of(it.action)?.ref }.toSet()

    /** The edge with its row key in place, or null when [arguments] has no key for it. */
    fun applied(edge: Edge, arguments: Map<String, String>): Edge? {
        val selection = ProfileSelections.of(edge.action) ?: return edge
        val key = arguments[selection.ref] ?: return null
        return edge.copy(action = edge.action.replace("*", key))
    }

    /** The shortest walk, preferring one that [arguments] can complete. */
    fun find(from: String, to: String, arguments: Map<String, String> = emptyMap()): List<Edge>? =
        search(from, to, exported.filter { applied(it, arguments) != null })
            ?: search(from, to, exported)

    /** Breadth-first search, so the first walk found is a shortest one. */
    private fun search(from: String, to: String, edges: List<Edge>): List<Edge>? {
        if (from == to) return emptyList()
        val cameBy = mutableMapOf<String, Edge>()
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val screen = queue.removeFirst()
            for (edge in edges.filter { it.from == screen }) {
                if (edge.to == from || edge.to in cameBy) continue
                cameBy[edge.to] = edge
                if (edge.to == to) return pathTo(to, from, cameBy)
                queue.addLast(edge.to)
            }
        }
        return null
    }

    /** Walks the search's records back from [to] to list the steps in order. */
    private fun pathTo(to: String, from: String, cameBy: Map<String, Edge>): List<Edge> {
        val path = mutableListOf<Edge>()
        var screen = to
        while (screen != from) {
            val edge = cameBy.getValue(screen)
            path.add(0, edge)
            screen = edge.from
        }
        return path
    }
}
