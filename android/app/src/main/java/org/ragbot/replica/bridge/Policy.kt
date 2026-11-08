package org.ragbot.replica.bridge

import org.ragbot.replica.EdgeKind
import org.ragbot.replica.FormField
import org.ragbot.replica.ReplicaGraph
import org.ragbot.replica.ReplicaProfile

/*
 * Rule 4 in code: decides what the executor may touch.
 * Called by Actions before every tap and every typed value, and by Executor before a command runs.
 * This is an allow-list. A control is touched only if an exported edge or a non-secret form field
 * names it, and anything on the user-only list is refused even if a list names it by mistake.
 * It must never gain an exception for confirm, submit, pay, PIN, OTP, password or CVV2.
 */
internal object Policy {
    private val exportedTags =
        ReplicaGraph.edges
            .filter { it.kind == EdgeKind.REAL || it.kind == EdgeKind.DEMO }
            .flatMap { it.action.split(" > ") }
            .toSet()

    private val forms = ReplicaProfile.forms.associateBy { it.id }

    /** A row or choice key: plain characters, so it cannot name another step or tag. */
    private val safeKey = Regex("[\\p{L}\\p{N}_-]+")

    /** True when [tag] fits [pattern], where * stands for a row key. */
    fun matches(pattern: String, tag: String) =
        pattern.split("*").joinToString(".*") { Regex.escape(it) }.toRegex().matches(tag)

    /** True when [tag] is on the profile's list of actions only the user may perform. */
    fun isUserOnly(tag: String) = ReplicaProfile.userOnlyActions.any { matches(it, tag) }

    /** True when [key] is one plain word, so it can only ever name a row or an option. */
    fun isSafeKey(key: String) = safeKey.matches(key)

    /** The fillable field [ref] of [form], or null when it is unknown or only the user's. */
    fun fillable(form: String, ref: String): FormField? {
        val field = forms[form]?.fields?.firstOrNull { matches(it.ref, ref) } ?: return null
        return field.takeUnless { it.secret || isUserOnly(ref) }
    }

    /** May the executor tap [tag] on [screen]? Only named controls, and never a user-only one. */
    fun mayTap(tag: String, screen: String): Boolean {
        if (isUserOnly(tag)) return false
        return exportedTags.any { matches(it, tag) } || opensField(tag, screen)
    }

    /** True for a field's own control: its picker, or one of its options. */
    private fun opensField(tag: String, screen: String): Boolean {
        val fields = forms[screen]?.fields.orEmpty().filterNot { it.secret }
        return fields.any { field ->
            tag == field.ref ||
                tag == "${field.ref}-picker" ||
                matches("${field.ref}-*", tag)
        }
    }

    /** May the executor type into [ref] on [screen]? */
    fun mayType(ref: String, screen: String) = !isUserOnly(ref) && fillable(screen, ref) != null
}
