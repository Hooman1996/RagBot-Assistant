package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class ChequeRulesTest {
    @Test
    fun chequeNumberIsOnlyRequiredAndDropsATrailingInvalidCharacter() {
        assertEquals(PaymentRules.MANDATORY, ChequeRules.chequeNoError(""))
        assertNull(ChequeRules.chequeNoError("534001"))
        assertEquals("534001/22", ChequeRules.chequeNoInput("534001/22"))
        assertEquals("53400", ChequeRules.chequeNoInput("53400a"))
        assertEquals("12", ChequeRules.chequeNoInput("۱۲"))
    }

    @Test
    fun trackingFindsOnlyTheSelectedAccountsCheques() {
        assertEquals("برگشتی", ChequeRules.track("account-1", "534002")?.statusDesc)
        assertNull(ChequeRules.track("account-2", "534002"))
        assertEquals("ثبت شده", ChequeRules.track("account-2", "۲۲۹۹۰۳")?.statusDesc)
    }

    @Test
    fun searchesMatchNumberOrAmount() {
        val book = ChequeFixture.books.first()
        assertEquals(
            listOf("534003"),
            ChequeRules.search(ChequeFixture.cheques(book), "534003").map { it.no },
        )
        assertEquals(4, ChequeRules.search(ChequeFixture.cheques(book), "").size)
        assertEquals(
            listOf("880002"),
            ChequeRules.searchReport(ChequeFixture.report, "9500000").map { it.no },
        )
    }

    @Test
    fun issueNeedsAnAccountAndPageCountAndSharedAccountsGoToTheCardBoard() {
        assertEquals(setOf("account", "page"), ChequeRules.issueErrors(ChequeIssueDraft()).keys)
        assertTrue(
            ChequeRules.issueErrors(ChequeIssueDraft(ChequeMedia.DIGITAL, "account-1", "20"))
                .isEmpty()
        )
        assertEquals(listOf("10", "20"), ChequeFixture.pageCounts.getValue(ChequeMedia.DIGITAL))
        assertTrue(ChequeRules.cardBoard("account-3"))
        assertFalse(ChequeRules.cardBoard("account-1"))
    }

    @Test
    fun menuOpensOnlyBuiltPagesAndPaperChequeTasksOpenTheirOwnDetail() {
        val targets = ChequeFixture.menu.mapNotNull { it.destination }.toSet()
        assertTrue(HomeDestinations.implemented.containsAll(targets))
        assertTrue("S-148" in HomeDestinations.implemented)
        assertEquals(listOf(false, true, false), ChequeFixture.tasks.map { it.pichak })
        assertEquals(setOf("WAITING", "TRACK", "DONE"), ChequeFixture.tabs.map { it.first }.toSet())
    }

    @Test
    fun reportFilterNeedsAStatusAndDatesOnlyForOtherBanksOutsideReceiving() {
        assertEquals(
            mapOf("status" to PaymentRules.MANDATORY),
            ChequeRules.reportErrors(ChequeReportFilter("OTHER")),
        )
        assertTrue(ChequeRules.reportErrors(ChequeReportFilter("INTERNAL", "PASSED")).isEmpty())
        assertTrue(ChequeRules.reportErrors(ChequeReportFilter("OTHER", "RECEIVING")).isEmpty())
        assertTrue(ChequeReportFilter("OTHER", "RETURNED").dated)
        assertFalse(ChequeReportFilter("INTERNAL", "RETURNED").dated)
        assertEquals(
            listOf("PASSED", "RETURNED", "RECEIVING"),
            chequeReportSubtypes.map { it.first },
        )
        assertEquals(
            setOf("INTERNAL", "OTHER"),
            ChequeFixture.menu
                .filter { it.destination == ChequeDestinations.REPORT_FILTER }
                .map { if ("KARAFARIN" in it.key) "INTERNAL" else "OTHER" }
                .toSet(),
        )
    }
}
