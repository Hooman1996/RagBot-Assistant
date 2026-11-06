package org.ragbot.replica.bridge

import org.ragbot.replica.HomeDestinations
import org.ragbot.replica.ReplicaGraph

/**
 * The app as the graph describes it: tapping an edge's tags moves to its target. Every tap and
 * typed value is recorded, so a test can prove a control was never touched.
 */
internal class FakeControls(var screen: String = HomeDestinations.HOME) : Controls {
    val tapped = mutableListOf<String>()
    val typed = mutableMapOf<String, String>()
    var modal: String? = null
    var disabled = setOf<String>()
    var errors = setOf<String>()
    var rows = setOf<String>()
    var pressedBack = 0

    /** Tags that exist on this screen though no edge names them, such as a form's fields. */
    var extra = setOf<String>()

    private fun edges() = ReplicaGraph.edges.filter { it.from == screen }

    private fun shown(tag: String): Boolean {
        if (tag in extra || tag in rows || tag in typed) return true
        return edges().any { edge -> edge.action.split(" > ").any { it == tag } }
    }

    override fun state() = ScreenState(screen, "HOME", modal, false)

    override fun find(tag: String) =
        if (!shown(tag)) null
        else
            Control(
                tag,
                typed[tag].orEmpty(),
                tag !in disabled,
                clickable = true,
                editable = tag in extra,
                selected = false,
                password = false,
                error = tag in errors,
            )

    override fun withPrefix(prefix: String) = emptyList<Control>()

    override fun click(tag: String) {
        tapped += tag
        val edge = edges().firstOrNull { Policy.matches(it.action.split(" > ").last(), tag) } ?: return
        if (tag !in errors) screen = edge.to
    }

    override fun setText(tag: String, text: String) {
        typed[tag] = text
    }

    override fun back() {
        pressedBack += 1
        screen = HomeDestinations.HOME
    }
}
