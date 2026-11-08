package org.ragbot.replica.bridge

import org.ragbot.replica.latinDigits

/*
 * The one place the executor touches the screen.
 * Navigator and FormFiller call it for every tap and every typed value. Each call asks Policy
 * first, then checks that the control is shown and enabled, so a disabled control is never fired.
 * It must never call Controls for a tag Policy refuses.
 */
internal class Actions(
    private val controls: Controls,
    private val patienceMs: Long = 3_000,
    private val pauseMs: Long = 40,
) {
    /** Where the app is now. */
    fun state() = controls.state()

    /** The control with this tag, or null. Reading is always allowed. */
    fun find(tag: String) = controls.find(tag)

    /** Waits until [done] holds. The UI updates on its own frame, after the handler returns. */
    fun waitUntil(done: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + patienceMs
        while (true) {
            if (done()) return true
            if (System.currentTimeMillis() > deadline) return false
            Thread.sleep(pauseMs)
        }
    }

    /** Taps [tag] through its own handler, or says why it could not. Null means tapped. */
    fun tap(tag: String): Ending? {
        val screen = controls.state().screen
        if (!Policy.mayTap(tag, screen)) return failed("policy_refused")
        if (!waitUntil { controls.find(tag) != null }) return blocked("control_not_shown:$tag")
        val control = checkNotNull(controls.find(tag))
        if (!control.enabled) return blocked("control_disabled:$tag")
        if (!control.clickable) return failed("control_not_clickable")
        controls.click(tag)
        return null
    }

    /** Types through the field's own handler and reads it back. Null means set. */
    fun type(ref: String, text: String): Ending? {
        val screen = controls.state().screen
        if (!Policy.mayType(ref, screen)) return failed("policy_refused")
        val control = controls.find(ref) ?: return blocked("control_not_shown:$ref")
        if (control.password) return failed("policy_refused")
        if (!control.enabled) return blocked("control_disabled:$ref")
        controls.setText(ref, text)
        // A money field shows separators, so only its digits are compared.
        val wanted = comparable(text)
        val set = waitUntil { controls.find(ref)?.let { comparable(it.text) } == wanted }
        return if (set) null else failed("field_not_set")
    }

    /** Presses Back, which only ever leaves a page. */
    fun back() = controls.back()

    /** The refs of fields on [screen] now showing an error. */
    fun errorsAmong(refs: List<String>) = refs.filter { controls.find(it)?.error == true }

    /** The form of a value to compare: only its digits when it is a number. */
    private fun comparable(text: String): String {
        val digits = latinDigits(text).filter { it.isDigit() }
        return if (digits.isNotEmpty() && text.all { !it.isLetter() }) digits else text
    }
}
