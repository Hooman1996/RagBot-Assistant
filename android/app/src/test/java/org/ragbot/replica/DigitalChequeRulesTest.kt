package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class DigitalChequeRulesTest {
    @Test
    fun onlyAnEmptySayadIdIsRejectedBeforeTheInquiry() {
        assertEquals(PaymentRules.MANDATORY, DigitalChequeRules.sayadError(""))
        assertNull(DigitalChequeRules.sayadError("0000000000000201"))
    }

    @Test
    fun cartableAndPickerSearchBySayadId() {
        assertEquals(2, DigitalChequeRules.search("").size)
        assertEquals(
            listOf("0000000000000202"),
            DigitalChequeRules.search("۲۰۲").map { it.sayadId },
        )
        assertTrue(DigitalChequeRules.search("999").isEmpty())
        assertEquals(listOf("0000000000000201"), DigitalChequeRules.assigned("201"))
        assertTrue(DigitalChequeRules.assigned("999").isEmpty())
    }

    @Test
    fun inquiryCarriesTheHoldersAndDigitalIban() {
        val cheque = checkNotNull(DigitalChequeRules.inquiry("0000000000000202"))
        assertEquals(listOf("کاربر نمایشی", "دارندهٔ نمایشی"), cheque.holders)
        assertEquals(2_500_000L, cheque.amount)
        assertNull(DigitalChequeRules.inquiry("0000000000000999"))
    }

    @Test
    fun menuOpensTheThreeRecoveredDigitalChequePages() {
        assertEquals(
            setOf("S-157", "S-151", "S-159"),
            ChequeFixture.menu
                .mapNotNull { it.destination }
                .filter { it in DigitalChequeDestinations.implemented }
                .toSet(),
        )
        assertFalse(DigitalTransferDraft().fixed)
    }
}
