package org.ragbot.replica.assistant

import org.junit.Assert.*
import org.junit.Test
import org.ragbot.replica.Json
import org.ragbot.replica.bridge.Observation

class AssistantClientTest {
    private val observation = Observation(3, 1, 1, "S-026", null, false, emptyList(), null)

    @Test
    fun aMessageCarriesTheWordsAndTheObservationAndNoSelection() {
        val body = Json.parse("body", AssistantClient.requestBody("c1", "به \"رضا\"", null, null, observation))
        assertEquals("c1", body.str("conversation_id"))
        assertEquals("replica-dev", body.str("session_id"))
        assertEquals("به \"رضا\"", body.str("message"))
        assertFalse(body.has("selection"))
        assertEquals("S-026", body["observation"].str("screen"))
    }

    @Test
    fun anOptionCarriesItsQuestionAndRefAndNoMessage() {
        val option = PanelOption("card-transfer-source=card-1", "****0018")
        val body = Json.parse("body", AssistantClient.requestBody("c1", null, option, "q1", observation))
        assertEquals("q1", body["selection"].str("question_id"))
        assertEquals("card-transfer-source=card-1", body["selection"].str("ref"))
        assertFalse(body.has("message"))
    }

    @Test
    fun aHistoryWithAQuestionGivesItsOptions() {
        val reply =
            """{"conversation_id":"c1","busy":false,"messages":[{"from":"user","text":"کارت به کارت"},""" +
                """{"from":"assistant","text":"از کدام کارت؟"}],"current":{"kind":"clarification",""" +
                """"question_id":"q1","text":"از کدام کارت؟","options":[{"ref":"cancel","label":"لغو"}]},""" +
                """"steps":[]}"""
        val history = AssistantClient.parseHistory(reply)
        assertEquals(listOf(true, false), history.lines.map { it.fromUser })
        assertEquals("q1", history.questionId)
        assertEquals(listOf(PanelOption("cancel", "لغو")), history.options)
        assertNull(history.taskState)
        assertEquals("a pending question waits for the user", "clarifying", history.shownState)
    }

    @Test
    fun aHistoryAtTheReviewGivesTheStateAndTheSteps() {
        val reply =
            """{"conversation_id":"c1","busy":false,"messages":[],"current":{"kind":"status",""" +
                """"task_state":"ready_for_user"},"steps":[{"operation":"navigate","screen":"S-174",""" +
                """"outcome":"succeeded"}]}"""
        val history = AssistantClient.parseHistory(reply)
        assertEquals("ready_for_user", history.taskState)
        assertEquals(listOf("navigate → S-174: succeeded"), history.steps)
        assertTrue(history.options.isEmpty())
    }

    @Test
    fun anEmptyHistoryShowsNothing() {
        val reply = """{"conversation_id":"c1","busy":false,"messages":[],"current":null,"steps":[]}"""
        assertEquals(PanelHistory(), AssistantClient.parseHistory(reply))
    }

    @Test
    fun everyTaskStateHasPersianWords() {
        val states = listOf("created", "clarifying", "resolving", "preparing", "verifying_review",
            "ready_for_user", "completed", "paused", "reconciling", "cancelling", "cancelled", "failed")
        assertEquals(states.toSet(), STATE_WORDS.keys)
        assertFalse("completed" in CANCELLABLE || "cancelled" in CANCELLABLE || "failed" in CANCELLABLE)
    }
}
