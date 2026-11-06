package org.ragbot.replica.bridge

/*
 * The executor's only view of the screen. ComposeControls implements it on the real app, and the
 * JVM tests use a fake. An app team porting the executor re-implements this one interface.
 * It never decides what is allowed: Policy and Actions do that before calling it.
 */

/** One control on screen, found by its test tag. */
internal data class Control(
    val tag: String,
    val text: String,
    val enabled: Boolean,
    val clickable: Boolean,
    val editable: Boolean,
    val selected: Boolean,
    val password: Boolean,
    val error: Boolean,
)

/** Where the app is. [screen] is a profile ID, and [modal] names an open dialog, if any. */
internal data class ScreenState(
    val screen: String,
    val homeTab: String,
    val modal: String?,
    val loading: Boolean,
)

internal interface Controls {
    fun state(): ScreenState

    /** The control with this tag in the top window, or null. */
    fun find(tag: String): Control?

    /** Every control of the top window whose tag starts with [prefix]. */
    fun withPrefix(prefix: String): List<Control>

    /** Runs the control's own click handler, as a tap would. */
    fun click(tag: String)

    /** Replaces the text through the field's own change handler, as typing would. */
    fun setText(tag: String, text: String)

    /** Presses Android Back. */
    fun back()
}
