package org.ragbot.replica

/*
 * The shape of the profile, and the object the rest of the app reads it through.
 * Generated into app_profiles/replica.profile.json by the JVM test ReplicaProfileTest.
 * The forms, features and user-only list each live in their own Profile*.kt file.
 */

/**
 * One field of a form; [ref] is its test tag. [requires] means its screen will not continue
 * without it. Secret fields are listed so they can be refused, and are never filled or asked for.
 */
internal data class FormField(
    val ref: String,
    val type: String,
    val secret: Boolean = false,
    val requires: Boolean = false,
    val ask: String? = null,
    val description: String? = null,
    val lookup: String? = null,
)

internal data class ProfileForm(val id: String, val fields: List<FormField>)

/** [form] is the feature's first form; a route to [destination] passes through it. */
internal data class ProfileFeature(
    val intent: String,
    val description: String,
    val requiredSlots: List<String>,
    val lookups: List<String>,
    val destination: String,
    val form: String?,
)

/** What the replica exports. Generated into app_profiles/replica.profile.json. */
internal object ReplicaProfile {
    val forms = ProfileForms.all
    val features = ProfileFeatures.all
    val userOnlyActions = ProfileUserOnly.actions
}
