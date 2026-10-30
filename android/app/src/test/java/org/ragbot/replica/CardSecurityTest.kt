package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class CardSecurityTest {
    @Test
    fun pinRulesFollowTheRecoveredLengthsAndRetypeCheck() {
        assertEquals(
            emptyMap<String, String>(),
            pinErrors(PinOperation.ISSUE_FIRST, "", "1234", "1234"),
        )
        assertEquals(setOf("new"), pinErrors(PinOperation.ISSUE_FIRST, "", "12345", "12345").keys)
        assertEquals(
            setOf("retype"),
            pinErrors(PinOperation.ISSUE_SECOND, "", "12345", "12346").keys,
        )
        assertEquals(
            setOf("new", "retype"),
            pinErrors(PinOperation.ISSUE_SECOND, "", "1234", "").keys,
        )
        assertEquals(
            emptyMap<String, String>(),
            pinErrors(PinOperation.CHANGE_SECOND, "11111", "123456789012", "123456789012"),
        )
        assertEquals(
            setOf("new"),
            pinErrors(PinOperation.CHANGE_SECOND, "1", "1234567890123", "1234567890123").keys,
        )
        assertEquals(
            setOf("current"),
            pinErrors(PinOperation.CHANGE_FIRST, "", "۱۲۳۴", "۱۲۳۴").keys,
        )
    }

    @Test
    fun tabRowsOpenOnlyImplementedSecurityPages() {
        val rows =
            CardFixture.actions.values.flatten().filter {
                it.key in setOf("block", "pin", "activation")
            }
        assertEquals(5, rows.size)
        rows.forEach { assertTrue(it.destination in CardSecurityDestinations.implemented) }
        assertTrue(HomeDestinations.implemented.containsAll(CardSecurityDestinations.implemented))
        assertEquals("12345", PREVIEW_OTP)
    }
}
