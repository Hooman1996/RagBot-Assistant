package org.ragbot.replica.bridge

import org.ragbot.replica.Edge
import org.ragbot.replica.HomeDestinations
import org.ragbot.replica.ReplicaProfile

/*
 * The navigate operation.
 * Called by Executor. It moves one exported edge at a time, through the tags a user taps, and
 * checks the new screen after each step. navigate's arguments fill in the row keys. It walks again
 * from wherever it lands, because some pages choose their own next page, and it reports where it
 * stopped. It never opens a screen directly and never crosses a user-only step.
 */
internal class Navigator(private val actions: Actions) {
    /** The steps taken by the last operation, for the result's trace. */
    var trace = emptyList<TraceStep>()
        private set

    /** Moves to [destination], or stops and says what stopped it. */
    fun navigate(destination: String, arguments: Map<String, String>): Ending {
        val steps = mutableListOf<TraceStep>()
        trace = steps
        val start = actions.state().screen
        val reachable =
            Routes.find(start, destination) != null ||
                Routes.find(HomeDestinations.HOME, destination) != null
        if (!reachable) return blocked("no_executor_route")
        var movedForward = false
        repeat(MAX_STEPS) {
            val now = actions.state()
            if (now.modal != null) return blocked("close_dialog")
            if (now.screen == destination) return succeeded("screen_is_$destination")
            val edge = Routes.find(now.screen, destination, arguments)?.firstOrNull()
            if (edge == null && movedForward) return blocked("unexpected_screen")
            if (edge == null || needsHomeTab(edge, now)) {
                stepBack(steps, now.screen)
            } else {
                val step = Routes.applied(edge, arguments) ?: return blocked("missing_argument")
                walk(step, steps)?.let {
                    return it
                }
                movedForward = true
            }
        }
        return failed("route_too_long")
    }

    /**
     * Takes one edge to the page in front of the user. Null means it moved. A page may choose
     * another target, such as a form that skips a step, so any new page counts after the wait.
     */
    fun walk(edge: Edge, steps: MutableList<TraceStep>): Ending? {
        for (tag in edge.action.split(" > ")) {
            actions.tap(tag)?.let {
                return it
            }
        }
        actions.waitUntil { actions.state().screen == edge.to }
        if (actions.state().screen == edge.from) return stoppedOn(edge)
        steps += TraceStep(edge.from, actions.state().screen, edge.action)
        return null
    }

    /** The page stayed: say whether a dialog or the page's own validation stopped the move. */
    private fun stoppedOn(edge: Edge): Ending {
        if (actions.state().modal != null) return blocked("close_dialog")
        val refs =
            ReplicaProfile.forms.firstOrNull { it.id == edge.from }?.fields.orEmpty().map { it.ref }
        val wrong = actions.errorsAmong(refs)
        return if (wrong.isEmpty()) blocked("valid_form") else blocked(*wrong.toTypedArray())
    }

    /** Home's edges start from its home tab, unless they choose a tab themselves. */
    private fun needsHomeTab(edge: Edge, now: ScreenState) =
        edge.from == HomeDestinations.HOME &&
            now.homeTab != "HOME" &&
            !edge.action.startsWith("control-")

    /** Presses Back once and records the move. Back only ever leaves a page. */
    private fun stepBack(steps: MutableList<TraceStep>, from: String) {
        actions.back()
        actions.waitUntil { actions.state().screen != from }
        steps += TraceStep(from, actions.state().screen, "back")
    }

    private companion object {
        const val MAX_STEPS = 40
    }
}
