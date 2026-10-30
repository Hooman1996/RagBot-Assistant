package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class NotificationsSupportRulesTest {
    @Test
    fun unreadTotalsExcludeInitiallyReadAndLocallyReadOrDeletedRecords() {
        assertEquals(5, NotificationFixture.unreadCount(emptyList(), emptyList()))
        assertEquals(4, NotificationFixture.unreadCount(listOf("notice-1"), emptyList()))
        assertEquals(
            3,
            NotificationFixture.unreadCount(
                listOf("notice-1", "notice-1"),
                listOf("notice-2", "notice-3"),
            ),
        )
        assertEquals(
            listOf("notice-1", "notice-5"),
            NotificationFixture.visible(listOf("notice-3"), "info").map { it.id },
        )
        assertEquals(
            0,
            NotificationFixture.unreadCount(emptyList(), NotificationFixture.records.map { it.id }),
        )
    }

    @Test
    fun messageAndQuestionLengthsUseTheirSeparateRecoveredBoundaries() {
        assertFalse(SupportRules.message("متن"))
        assertFalse(SupportRules.message("a".repeat(4)))
        assertTrue(SupportRules.message("a".repeat(5)))
        assertTrue(SupportRules.message("a".repeat(250)))
        assertFalse(SupportRules.message("a".repeat(251)))
        assertFalse(SupportRules.question("a".repeat(3)))
        assertTrue(SupportRules.question("پرسش"))
        assertTrue(SupportRules.question("a".repeat(250)))
        assertFalse(SupportRules.question("a".repeat(251)))
        // The recovered frontend counts raw input; server whitespace rules remain unknown.
        assertTrue(SupportRules.message("     "))
        assertTrue(SupportRules.question("    "))
    }

    @Test
    fun suggestionRequiresDescriptionWithoutInventingAMessageLengthLimit() {
        assertFalse(SupportRules.suggestion(""))
        assertTrue(SupportRules.suggestion("ن"))
        assertTrue(SupportRules.suggestion("ن".repeat(251)))
    }

    @Test
    fun everySyntheticThreadBelongsToExactlyOneTicketAndStartsWithItsQuestion() {
        val keys = SupportFixture.tickets.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(SupportFixture.firstTicket in keys)
        assertTrue(SupportFixture.messages.all { it.ticketKey in keys })
        SupportFixture.tickets.forEach { ticket ->
            val thread = SupportFixture.messages.filter { it.ticketKey == ticket.key }
            assertEquals(ticket.question, thread.first().text)
            assertFalse(thread.first().fromSupport)
            if (ticket.status == "ANSWERED") assertTrue(thread.any { it.fromSupport })
        }
    }
}
