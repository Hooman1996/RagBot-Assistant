package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class LoanRulesTest {
    private val normal = LoanFixture.loans.first()
    private val overdue = LoanFixture.loans[1]

    @Test
    fun paidInstallmentsCannotBeChosen() {
        assertEquals(LoanFixture.PAID_ERROR, LoanFixture.selectionError(normal, emptyList(), 0))
        val paid = LoanFixture.loans.last()
        assertEquals(LoanFixture.PAID_ERROR, LoanFixture.selectionError(paid, emptyList(), 0))
    }

    @Test
    fun selectionStartsAtFirstUnpaidAndCannotSkipAhead() {
        assertEquals(LoanFixture.ORDER_ERROR, LoanFixture.selectionError(normal, emptyList(), 2))
        assertNull(LoanFixture.selectionError(normal, emptyList(), 1))
        assertNull(LoanFixture.selectionError(normal, listOf(1), 2))
    }

    @Test
    fun deselectionMustRemoveTheLastSelectedInstallmentFirst() {
        assertEquals(LoanFixture.ORDER_ERROR, LoanFixture.selectionError(normal, listOf(1, 2), 1))
        assertNull(LoanFixture.selectionError(normal, listOf(1, 2), 2))
        assertNull(LoanFixture.selectionError(normal, listOf(1), 1))
    }

    @Test
    fun trackableDebtMustBeSelectedBeforeOtherStatuses() {
        assertEquals(LoanFixture.OVERDUE_ERROR, LoanFixture.selectionError(overdue, emptyList(), 2))
        assertEquals(LoanFixture.ORDER_ERROR, LoanFixture.selectionError(overdue, emptyList(), 1))
        assertNull(LoanFixture.selectionError(overdue, emptyList(), 0))
        assertNull(LoanFixture.selectionError(overdue, listOf(0), 1))
        assertEquals(
            LoanFixture.OVERDUE_ERROR,
            LoanFixture.selectionError(overdue, listOf(0, 1), 2),
        )
    }

    @Test
    fun totalsUseRemainingAmountWithoutAddingPenaltyAgain() {
        assertEquals(8_150_000L, LoanFixture.selectedSumIrr(overdue, listOf(0, 1)))
        assertEquals(8_150_000L, LoanFixture.payableIrr(overdue, listOf(0, 1)))
        val partial =
            normal.copy(
                installments =
                    listOf(
                        normal.installments[1].copy(paidIrr = 2_000_000L, remainingIrr = 3_000_000L)
                    )
            )
        assertEquals(3_000_000L, LoanFixture.selectedSumIrr(partial, listOf(0)))
    }

    @Test
    fun fullDebtSelectionReceivesTheRecoveredEarlyRepaymentDiscount() {
        assertEquals(10_000_000L, LoanFixture.selectedSumIrr(normal, listOf(1, 2)))
        assertEquals(9_750_000L, LoanFixture.payableIrr(normal, listOf(1, 2)))
        assertEquals(5_000_000L, LoanFixture.payableIrr(normal, listOf(1)))
        assertEquals(
            10_000_000L,
            LoanFixture.payableIrr(normal.copy(discountIrr = 0L), listOf(1, 2)),
        )
    }

    @Test
    fun reviewRequiresAValidSequenceAndAnExplicitSource() {
        assertEquals(LoanFixture.SELECT_ERROR, LoanFixture.reviewError(normal, emptyList(), ""))
        assertEquals(LoanFixture.SOURCE_ERROR, LoanFixture.reviewError(normal, listOf(1), ""))
        assertEquals(
            LoanFixture.ORDER_ERROR,
            LoanFixture.reviewError(normal, listOf(2), "account-1"),
        )
        assertEquals(
            LoanFixture.ORDER_ERROR,
            LoanFixture.reviewError(normal, listOf(1, 1), "account-1"),
        )
        assertNull(LoanFixture.reviewError(normal, listOf(1, 2), "account-1"))
    }

    @Test
    fun emptySchedulesAndOutOfRangeSelectionsCannotReachReview() {
        val empty = normal.copy(installments = emptyList())
        assertEquals(LoanFixture.SELECT_ERROR, LoanFixture.selectionError(empty, emptyList(), 0))
        assertEquals(
            LoanFixture.SELECT_ERROR,
            LoanFixture.reviewError(normal, listOf(99), "account-1"),
        )
    }

    @Test
    fun amountsAboveFloatPrecisionRemainExactIntegers() {
        val exact = 9_007_199_254_740_993L
        val loan =
            normal.copy(
                totalDebtIrr = exact,
                discountIrr = 1L,
                installments =
                    listOf(normal.installments[1].copy(amountIrr = exact, remainingIrr = exact)),
            )
        assertEquals(exact, LoanFixture.selectedSumIrr(loan, listOf(0)))
        assertEquals(exact - 1L, LoanFixture.payableIrr(loan, listOf(0)))
    }
}
