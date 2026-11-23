package org.ragbot.replica.bridge

import org.ragbot.replica.EdgeKind
import org.ragbot.replica.ProfileForms
import org.ragbot.replica.ProfileSelections
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
        val texts = values.mapValues { (_, value) -> text(value) }.entries.toList()
        for ((index, entry) in texts.withIndex()) {
            val next = texts.getOrNull(index + 1)?.let { "${it.key}-${it.value}" }
            setField(entry.key, entry.value, next)?.let {
                return it
            }
        }
        return succeeded("form_filled_$form")
    }

    private fun text(value: FieldValue) =
        when (value) {
            is FieldValue.Money -> value.value.toString()
            is FieldValue.Text -> value.text
        }

    /**
     * A typed field takes the text. Any other field is a choice, and the text is its key. A choice
     * may open a dialog that asks the next field ([next], its option's tag), such as a weekly
     * transfer's day: then the dialog is left for that field.
     */
    private fun setField(ref: String, value: String, next: String?): Ending? {
        if (actions.find(ref)?.editable == true) return actions.type(ref, value)
        if (!Policy.isSafeKey(value)) return failed("invalid_value")
        val option = "$ref-$value"
        // A choice already made is left as it is: a second tap would untick a checkbox, such as a
        // loan's installment when the form is filled again before the review.
        if (actions.find(option)?.selected == true) return null
        if (actions.find(option) == null) {
            val opener = if (actions.find("$ref-picker") != null) "$ref-picker" else ref
            actions.tap(opener)?.let {
                return it
            }
        }
        actions.tap(option)?.let {
            // The option isn't there or is disabled: close the picker it opened, as a user would,
            // so the next command doesn't find a dialog in the way.
            if (actions.state().modal != null) {
                actions.back()
                actions.waitUntil { actions.state().modal == null }
            }
            return it
        }
        // The screen updates on a later frame: a chip turns selected, or a picker's window closes.
        actions.waitUntil { actions.find(option)?.selected != false }
        val settled = actions.waitUntil {
            actions.state().modal == null || (next != null && actions.find(next) != null)
        }
        return if (settled) null else blocked("close_dialog")
    }

    /** Opens the review page that follows this screen. On a review page, it does nothing. */
    fun showReview(): Ending {
        val screen = actions.state().screen
        if (screen in Reviews.pages) return succeeded("review_shown_$screen")
        val edge = Reviews.edgeFrom(screen) ?: return blocked("no_review_here")
        val steps = mutableListOf<TraceStep>()
        navigator.walk(edge, steps)?.let {
            return it
        }
        return succeeded("review_shown_${actions.state().screen}")
    }
}

/** The pages where the user's confirm or consent waits. The executor stops on these. */
internal object Reviews {
    /** Confirmation pages whose confirm has no result page in the graph. */
    private val withoutResultPage = setOf("S-128", "S-139", "S-144", "S-152", "S-331")

    private val serviceReviews =
        ServiceCatalog.flows.filter { it.submit != null }.map { it.steps.last().page }

    private val consentPages =
        ServiceCatalog.flows.flatMap { it.steps }.filter(ProfileForms::needsConsent).map { it.page }

    val pages =
        ReplicaGraph.edges.filter { it.kind == EdgeKind.USER_ONLY }.map { it.from }.toSet() +
            withoutResultPage +
            serviceReviews +
            consentPages

    /** The edge show_review takes from [screen] to the review after it, or null when none does. */
    fun edgeFrom(screen: String) =
        Routes.exported.firstOrNull {
            it.from == screen && it.to in pages && ProfileSelections.of(it.action) == null
        }

    /** Where show_review stops when it starts on [screen]: a review, or [screen] when none follows. */
    fun stop(screen: String) = if (screen in pages) screen else edgeFrom(screen)?.to ?: screen
}
