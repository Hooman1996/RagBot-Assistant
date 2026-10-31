package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class PaymentRulesTest {
    private val bill = BillDraft("account-1", "9000000000001", "1250001")

    @Test
    fun billAndPaymentIdsAreRequiredDigitsOfAtLeastSix() {
        assertEquals(PaymentRules.MANDATORY, PaymentRules.idError(""))
        assertEquals(PaymentRules.INVALID, PaymentRules.idError("12345"))
        assertEquals(PaymentRules.INVALID, PaymentRules.idError("12a456"))
        assertNull(PaymentRules.idError("۱۲۳۴۵۶"))
        assertNull(PaymentRules.idError("1234567891234"))
    }

    @Test
    fun eachSourceModeChecksItsOwnSource() {
        assertTrue(PaymentRules.billErrors(BillPaymentDestinations.ACCOUNT, bill).isEmpty())
        assertEquals(
            setOf("source", "bill", "pay"),
            PaymentRules.billErrors(BillPaymentDestinations.CARD, BillDraft()).keys,
        )
        val typed = bill.copy(source = "000000000000001")
        assertEquals(
            PaymentRules.INVALID,
            PaymentRules.billErrors(BillPaymentDestinations.WITHOUT_SOURCE, typed)["source"],
        )
        assertTrue(
            PaymentRules.billErrors(
                    BillPaymentDestinations.WITHOUT_SOURCE,
                    typed.copy(source = "0000000000000001"),
                )
                .isEmpty()
        )
    }

    @Test
    fun inquiryReturnsOnlyUnpaidFixtureBills() {
        val (found, error) = PaymentRules.inquiry(bill)
        assertNull(error)
        assertEquals(1_250_000L, found?.amount)
        assertNotNull(
            PaymentRules.inquiry(bill.copy(billId = "9000000000002", payId = "430002")).second
        )
        assertNotNull(PaymentRules.inquiry(bill.copy(payId = "1250002")).second)
    }

    @Test
    fun onlyDueOrPostponedInstallmentsArePayable() {
        val (open, closed) = PaymentFixture.policies
        assertEquals(
            listOf(false, false, true, true),
            open.installments.map(PaymentRules::selectable),
        )
        assertTrue(PaymentRules.payable(open))
        assertFalse(PaymentRules.payable(closed))
        assertEquals(PaymentFixture.statusTitles.keys, open.installments.map { it.status }.toSet())
    }

    @Test
    fun repaymentIsRequiredPositiveAndAtMostTheDebt() {
        assertEquals(PaymentRules.MANDATORY, PaymentRules.repaymentError("", 1_000L))
        assertEquals(PaymentRules.INVALID, PaymentRules.repaymentError("1,000", 1_000L))
        assertEquals(PaymentRules.OUT_OF_RANGE, PaymentRules.repaymentError("0", 1_000L))
        assertEquals(PaymentRules.OVER_DEBT, PaymentRules.repaymentError("1001", 1_000L))
        assertEquals(
            PaymentRules.OVER_DEBT,
            PaymentRules.repaymentError("99999999999999999999", 1_000L),
        )
        assertNull(PaymentRules.repaymentError("۱", 1_000L))
        assertNull(PaymentRules.repaymentError("1000", 1_000L))
    }
}
