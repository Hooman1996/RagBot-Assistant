package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class DigitalChequeRulesTest {
    @Test
    fun onlyAnEmptySayadIdIsRejectedBeforeTheInquiry() {
        assertEquals(PaymentRules.MANDATORY, DigitalChequeRules.sayadError(""))
        assertNull(DigitalChequeRules.sayadError("5041124072452997"))
    }

    @Test
    fun cartableAndPickerSearchBySayadId() {
        assertEquals(2, DigitalChequeRules.search("").size)
        assertEquals(
            listOf("1404713921090897"),
            DigitalChequeRules.search("۰۸۹۷").map { it.sayadId },
        )
        assertTrue(DigitalChequeRules.search("999").isEmpty())
        assertEquals(listOf("5041124072452997"), DigitalChequeRules.assigned("2997"))
        assertTrue(DigitalChequeRules.assigned("999").isEmpty())
    }

    @Test
    fun inquiryCarriesTheHoldersAndDigitalIban() {
        val cheque = checkNotNull(DigitalChequeRules.inquiry("1404713921090897"))
        assertEquals(listOf("امیرحسین کاظمی", "محمد جعفری"), cheque.holders)
        assertEquals(2_500_000L, cheque.amount)
        assertNull(DigitalChequeRules.inquiry("6104332658420740"))
    }

    @Test
    fun menuOpensTheThreeRecoveredDigitalChequePages() {
        assertEquals(
            setOf("S-157", "S-151", "S-159"),
            ChequeFixture.chakadMenu
                .mapNotNull { it.destination }
                .filter { it in DigitalChequeDestinations.implemented }
                .toSet(),
        )
        assertFalse(DigitalTransferDraft().fixed)
    }
}
