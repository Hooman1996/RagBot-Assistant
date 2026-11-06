package org.ragbot.replica.bridge

import org.ragbot.replica.BuildConfig
import org.ragbot.replica.HomeDestinations
import org.ragbot.replica.ReplicaProfile
import org.ragbot.replica.latinDigits

/*
 * Builds the observation of contracts/schemas/observation.schema.json.
 * Called by Executor for every result, and by CommandPoller to report the app's state.
 * The version rises on every visible change. A secret field is never read, not even masked.
 */
internal class Observer(private val controls: Controls) {
    private var version = 0L
    private var lastSeen: Observation? = null

    /** The draft the executor is working on. Null when there is none. */
    private var draft: ObservedDraft? = null
    private var draftFields: List<ObservedField> = emptyList()
    private var draftCount = 0

    /** The current draft's version. 0 means no draft, as in the contract. */
    val draftVersion
        get() = draft?.draft_version ?: 0L

    /** Records a fill: the first one starts a draft, and each one raises its version. */
    fun filled() {
        val current = draft
        draft =
            if (current == null) {
                draftCount += 1
                ObservedDraft("draft-$draftCount", 1, 0)
            } else current.copy(draft_version = current.draft_version + 1)
        draftFields = fields(controls.state().screen)
    }

    /** Records that the review now shows the draft's current version. */
    fun reviewed() {
        draft = draft?.let { it.copy(review_version = it.draft_version) }
    }

    /** A fresh observation. Its version rises only when something visible changed. */
    fun observe(): Observation {
        val state = controls.state()
        val fields = fields(state.screen)
        trackUserChanges(state.screen, fields)
        val fresh =
            Observation(
                version,
                BuildConfig.VERSION_CODE.toLong(),
                PROFILE_VERSION,
                state.screen,
                state.modal,
                state.loading,
                fields,
                draft,
            )
        val same = lastSeen?.copy(observation_version = version) == fresh
        if (!same) version += 1
        val seen = fresh.copy(observation_version = version)
        lastSeen = seen
        return seen
    }

    /** A user's edit changes the draft, so a command built on the old values must go stale. */
    private fun trackUserChanges(screen: String, fields: List<ObservedField>) {
        val current = draft ?: return
        if (screen == HomeDestinations.HOME || screen == HomeDestinations.FRONT) {
            draft = null
            draftFields = emptyList()
            return
        }
        val sameForm = fields.map { it.ref } == draftFields.map { it.ref }
        if (fields.isNotEmpty() && sameForm && fields != draftFields) {
            draft = current.copy(draft_version = current.draft_version + 1)
            draftFields = fields
        }
    }

    /** The current form's non-secret fields, masked. */
    private fun fields(screen: String): List<ObservedField> {
        val form = ReplicaProfile.forms.firstOrNull { it.id == screen } ?: return emptyList()
        return form.fields
            .filterNot { it.secret || "*" in it.ref }
            .mapNotNull { field -> shown(field.ref)?.let { ObservedField(field.ref, mask(it)) } }
    }

    /** A field's text, its picker's text, or the key of its selected option. */
    private fun shown(ref: String): String? {
        val control = controls.find(ref) ?: controls.find("$ref-picker")
        if (control != null) return if (control.password) null else control.text
        return controls.withPrefix("$ref-").firstOrNull { it.selected }?.tag?.removePrefix("$ref-")
    }

    companion object {
        const val PROFILE_VERSION = 1L

        /** Keeps only the last four digits of a card, account or IBAN number. */
        fun mask(value: String): String {
            val digits = latinDigits(value).filter { it.isDigit() }
            return if (digits.length >= 12) "****" + digits.takeLast(4) else value
        }
    }
}
