package org.ragbot.replica.bridge

import org.ragbot.replica.EdgeKind
import org.ragbot.replica.ReplicaGraph
import org.ragbot.replica.ServiceCatalog

/*
 * The fill_form and show_review operations.
 * Called by Executor. Text goes through the field's own change handler, and pickers through their
 * own option taps, so the screen validates exactly as it does for a user.
 * show_review opens the review and stops. It never taps the confirm on that page.
 */
internal class FormFiller(private val actions: Actions, private val navigator: Navigator) {
    /** Sets [values] on [form], which must be the screen in front of the user. */
    fun fill(form: String, values: Map<String, FieldValue>): Ending {
        if (actions.state().screen != form) return blocked("navigate")
        for ((ref, value) in values) {
            val stop =
                when (value) {
                    is FieldValue.Money -> actions.type(ref, value.value.toString())
                    is FieldValue.Text -> setField(ref, value.text)
                }
            if (stop != null) return stop
        }
        return succeeded("form_filled_$form")
    }

    /** A typed field takes the text. Any other field is a choice, and the text is its key. */
    private fun setField(ref: String, value: String): Ending? {
        if (actions.find(ref)?.editable == true) return actions.type(ref, value)
        if (!Policy.isSafeKey(value)) return failed("invalid_value")
        val option = "$ref-$value"
        if (actions.find(option) == null) {
            val opener = if (actions.find("$ref-picker") != null) "$ref-picker" else ref
            actions.tap(opener)?.let {
                return it
            }
        }
        actions.tap(option)?.let {
            return it
        }
        // The screen updates on a later frame: a chip turns selected, or a picker's window closes.
        actions.waitUntil { actions.find(option)?.selected != false }
        return if (actions.waitUntil { actions.state().modal == null }) null
        else blocked("close_dialog")
    }

    /** Opens the review page that follows this screen. On a review page, it does nothing. */
    fun showReview(): Ending {
        val screen = actions.state().screen
        if (screen in Reviews.pages) return succeeded("review_shown_$screen")
        val edge =
            Routes.exported.firstOrNull { it.from == screen && it.to in Reviews.pages }
                ?: return blocked("no_review_here")
        val steps = mutableListOf<TraceStep>()
        navigator.walk(edge, steps)?.let {
            return it
        }
        return succeeded("review_shown_${actions.state().screen}")
    }
}

/** The pages where the user's confirm waits. The executor stops on these. */
internal object Reviews {
    /** Confirmation pages whose confirm has no result page in the graph. */
    private val withoutResultPage = setOf("S-128", "S-139", "S-144", "S-152", "S-331")

    private val serviceReviews =
        ServiceCatalog.flows.filter { it.submit != null }.map { it.steps.last().page }

    val pages =
        ReplicaGraph.edges.filter { it.kind == EdgeKind.USER_ONLY }.map { it.from }.toSet() +
            withoutResultPage +
            serviceReviews
}
