package org.ragbot.replica.bridge

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.ragbot.replica.MainActivity
import org.ragbot.replica.assistant.AssistantPanelState
import org.ragbot.replica.assistant.PanelHistory
import org.ragbot.replica.assistant.PanelLine

/**
 * The assistant panel never stands between the executor and the screen, opens from the support
 * menu beside the chatbot or from its floating bubble, and minimizes back to the bubble. Like the bridge journeys, the
 * app runs on its own frame clock, and every tap goes through the executor's own controls.
 */
@RunWith(AndroidJUnit4::class)
class AssistantPanelTest {
    private lateinit var app: ActivityScenario<MainActivity>
    private lateinit var rig: BridgeRig

    @Before
    fun open() {
        app = ActivityScenario.launch(MainActivity::class.java)
        rig = BridgeRig()
        onMain {
            // After the launch, which sets it from the bridge's flag file.
            AssistantPanelState.available = true
            AssistantPanelState.open = false
            AssistantPanelState.history = PanelHistory()
        }
    }

    @After
    fun close() {
        onMain { AssistantPanelState.open = false }
        app.close()
    }

    private fun onMain(work: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(work)

    private fun shown(tag: String) = rig.controls.find(tag) != null

    private fun waitFor(tag: String, present: Boolean = true) =
        assertTrue(tag, Actions(rig.controls).waitUntil { shown(tag) == present })

    @Test
    fun withThePanelOpenTheExecutorStillReachesTheReview() {
        onMain { AssistantPanelState.open = true }
        waitFor("assistant-panel")
        val seen = rig.executor.observer.observe()
        assertNull("the panel is no dialog", seen.modal)
        assertFalse("the panel shows no spinner", seen.loading)
        rig.reach("S-174")
        rig.fill(
            "S-174",
            "card-transfer-source" to FieldValue.Text("card-1"),
            "card-transfer-destination" to FieldValue.Text("0000000000000091"),
            "card-transfer-amount" to FieldValue.Money(250_000),
        )
        rig.expect(rig.review(), "S-176")
        assertTrue("the panel stayed open", shown("assistant-panel"))
    }

    /** A control's shown top and bottom, in pixels (clipped by the page), and its whole height. */
    private fun place(tag: String): Triple<Float, Float, Int> {
        var found = Triple(Float.NaN, Float.NaN, 0)
        app.onActivity { activity ->
            val root = composeRoot(activity.window.decorView) ?: return@onActivity
            generateSequence(listOf(root.semanticsOwner.rootSemanticsNode)) { level ->
                    level.flatMap { it.children }.ifEmpty { null }
                }
                .flatten()
                .firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
                ?.let { found = Triple(it.boundsInRoot.top, it.boundsInRoot.bottom, it.size.height) }
        }
        return found
    }

    /** True when the whole control shows, above the open box. */
    private fun shownAboveTheBox(tag: String): Boolean {
        val (top, bottom, height) = place(tag)
        return height > 0 && bottom - top >= height - 1 && bottom <= place("assistant-panel").first
    }

    private fun composeRoot(view: View): RootForTest? =
        view as? RootForTest
            ?: (view as? ViewGroup)?.let { group ->
                (0 until group.childCount).firstNotNullOfOrNull { composeRoot(group.getChildAt(it)) }
            }

    @Test
    fun aQuestionBringsTheFieldItAsksAboveTheBox() {
        rig.reach("S-174")
        onMain { AssistantPanelState.open = true }
        waitFor("assistant-panel")
        Thread.sleep(1_000) // opening reloads the conversation; the question comes after that
        assertFalse("the amount starts hidden by the box", shownAboveTheBox("card-transfer-amount"))
        onMain { AssistantPanelState.history = PanelHistory(questionId = "q1", fields = listOf("card-transfer-amount")) }
        val shown = Actions(rig.controls).waitUntil { shownAboveTheBox("card-transfer-amount") }
        assertTrue("the whole amount field shows above the box", shown)
    }

    @Test
    fun theSupportMenuEntryOpensThePanelBesideTheChatbot() {
        rig.reach("D-001")
        waitFor("support-menu-chatbot")
        rig.controls.click("support-menu-assistant")
        waitFor("assistant-panel")
    }

    @Test
    fun theBubbleIsAlwaysThereAndOpensAndClosesTheBox() {
        waitFor("assistant-bubble")
        rig.controls.click("assistant-bubble")
        waitFor("assistant-panel")
        rig.controls.click("assistant-bubble")
        waitFor("assistant-panel", present = false)
    }

    @Test
    fun minimizeHidesTheBoxAndKeepsTheBubble() {
        onMain { AssistantPanelState.history = PanelHistory(lines = listOf(PanelLine(true, "کارت به کارت"))) }
        waitFor("assistant-bubble")
        rig.controls.click("assistant-bubble")
        waitFor("assistant-panel")
        rig.controls.click("assistant-minimize")
        waitFor("assistant-panel", present = false)
        assertTrue("the bubble stays", shown("assistant-bubble"))
    }
}
