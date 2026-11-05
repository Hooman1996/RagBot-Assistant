package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SayadChequeRulesTest {
    @Before
    fun freshSession() {
        MockBank.clock = { "۱۴۰۵/۰۸/۱۴ - ۱۰:۰۰" }
        MockBank.reset()
    }

    @Test
    fun aRegisteredIdCannotBeUsedTwice() {
        val id = "0000000000000900"
        assertTrue(MockBank.sayadRegister(id, 1_000L, "۱۴۰۵/۱۰/۰۱", "رضا") is BankResult.Done)
        assertTrue(MockBank.sayadRegister(id, 1_000L, "۱۴۰۵/۱۰/۰۱", "رضا") is BankResult.Failed)
        assertTrue(MockBank.sayadInquiry(id) is BankResult.Done)
    }

    @Test
    fun onlyAPendingChequeIsAnsweredAndOnlyAHeldChequeMoves() {
        val id = "0000000000000311"
        assertTrue(MockBank.sayadTransfer(id, "رضا") is BankResult.Failed)
        assertTrue(MockBank.sayadAnswer(id, true) is BankResult.Done)
        assertTrue(MockBank.sayadAnswer(id, false) is BankResult.Failed)
        assertTrue(MockBank.sayadTransfer(id, "رضا") is BankResult.Done)
        assertFalse(id in MockBank.sayadHeld.map { it.sayadId })
    }

    @Test
    fun everyReceiptPageOfAServiceIsDeclaredAfterTheUsersConfirm() {
        ServiceCatalog.flows.forEach { flow ->
            flow.result?.let {
                val edge =
                    ReplicaGraph.edges.single { e ->
                        e.from == flow.steps.last().page && e.to == it
                    }
                assertEquals(EdgeKind.USER_ONLY, edge.kind)
            }
        }
    }
}
