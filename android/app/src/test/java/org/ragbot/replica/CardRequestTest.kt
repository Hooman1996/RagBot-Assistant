package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class CardRequestTest {
    @Test
    fun inPersonDeliverySkipsTheAddressPage() {
        val request = CardRequest(CardOperation.ISSUE, "account-1")
        assertEquals(CardRequestDestinations.COLOR, nextAfterFirstPage(request))
        assertEquals(
            CardRequestDestinations.ADDRESS,
            nextAfterFirstPage(request.copy(inPerson = false)),
        )
    }

    @Test
    fun feesAreExactIntegerRialsAndRowsOpenImplementedRequestPages() {
        CardOperation.entries.forEach { assertTrue(it.fee > 0L) }
        val rows = CardFixture.actions.getValue(CardKind.DEBIT).associateBy { it.key }
        assertEquals(CardRequestDestinations.ISSUE, rows.getValue("issue").destination)
        listOf("extend", "replicate", "recolor").forEach {
            assertEquals(CardRequestDestinations.MODIFY, rows.getValue(it).destination)
            assertNotNull(CardOperation.valueOf(it.uppercase()))
        }
        assertTrue(HomeDestinations.implemented.containsAll(CardRequestDestinations.implemented))
    }
}
