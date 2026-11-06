package org.ragbot.replica.bridge

import org.ragbot.replica.Edge
import org.ragbot.replica.EdgeKind
import org.ragbot.replica.ReplicaGraph

/*
 * Finds a path over the exported edges of the profile.
 * Called by Navigator. User-only edges are not in this graph, so a route can never contain one.
 * It only plans: it never touches the screen.
 */
internal object Routes {
    val exported = ReplicaGraph.edges.filter { it.kind == EdgeKind.REAL || it.kind == EdgeKind.DEMO }

    /** A row-dependent edge: it needs a key from select_candidate. */
    fun needsCandidate(edge: Edge) = "*" in edge.action

    /** The shortest route, preferring one that needs no row choice. */
    fun find(from: String, to: String): List<Edge>? =
        search(from, to, exported.filterNot(::needsCandidate)) ?: search(from, to, exported)

    /** Breadth-first search, so the first route found is a shortest one. */
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

    /** Walks the search's records back from [to] to list the route in order. */
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
