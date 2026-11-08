package org.ragbot.replica.bridge

import org.junit.Assert.*
import org.junit.Test

class ContractJsonTest {
    private val base =
        """"protocol_version":1,"profile_version":1,"task_id":"t","task_revision":1,""" +
            """"command_id":"c","device_id":"d","session_id":"d",""" +
            """"expected_observation_version":3,"expires_at":"2026-11-06T12:00:30Z""""

    private val observation =
        Observation(3, 1, 1, "S-174", null, false, listOf(ObservedField("a", "ر\"ض")), null)

    @Test
    fun aReplyWithoutACommandHasNone() {
        assertNull(ContractJson.commandIn("""{"kind":"status","task_state":"paused"}"""))
    }

    @Test
    fun readsAFillFormWithMoney() {
        val reply =
            """{"kind":"command","command":{$base,"operation":"fill_form",""" +
                """"expected_draft_version":0,"arguments":{"form":"S-174","values":""" +
                """{"to":"رضا","amount":{"value":250000,"unit":"IRR"}}}}}"""
        val command = checkNotNull(ContractJson.commandIn(reply))
        assertEquals("S-174", command.form)
        assertEquals(0L, command.expected_draft_version)
        assertEquals(FieldValue.Text("رضا"), command.values["to"])
        assertEquals(FieldValue.Money(250_000), command.values["amount"])
    }

    @Test
    fun aFractionalAmountIsRefused() {
        val reply =
            """{"kind":"command","command":{$base,"operation":"fill_form",""" +
                """"expected_draft_version":0,"arguments":{"form":"S-174","values":""" +
                """{"amount":{"value":1.5,"unit":"IRR"}}}}}"""
        assertThrows(Exception::class.java) { ContractJson.commandIn(reply) }
    }

    @Test
    fun writesAResultWithOnlyTheFieldsItHas() {
        val text =
            ContractJson.write(
                CommandResult(
                    "c",
                    "blocked",
                    null,
                    observation,
                    0,
                    needs = listOf("missing_argument"),
                    trace = listOf(TraceStep("S-026", "S-283", "menu-charity")),
                )
            )
        assertTrue(text, text.startsWith("""{"command_id":"c","outcome":"blocked","""))
        assertTrue(text, """"postcondition":null""" in text)
        assertTrue(text, """"value":"ر\"ض"""" in text)
        assertTrue(text, """"needs":["missing_argument"]""" in text)
        assertTrue(text, """"action":"menu-charity"""" in text)
        assertFalse(text, "error_code" in text)
    }
}
