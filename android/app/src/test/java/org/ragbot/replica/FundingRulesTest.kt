package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class FundingRulesTest {
    @Test
    fun amountReadsTheFirstThirteenCharactersAndOtherwiseZero() {
        assertEquals(1_000_000L, FundingRules.amount("۱۰۰۰۰۰۰"))
        assertEquals(1_234_567_890_123L, FundingRules.amount("12345678901234567"))
        assertEquals(0L, FundingRules.amount(""))
        assertEquals(0L, FundingRules.amount("12a"))
    }

    @Test
    fun checksRunEmptyThenTransactionLimitsThenProposedRange() {
        assertEquals(PaymentRules.MANDATORY, FundingRules.error(0L))
        assertEquals("حداقل مبلغ افزایش موجودی ۱۰٬۰۰۰ ریال است.", FundingRules.error(9_999L))
        assertEquals(
            "حداکثر مبلغ افزایش موجودی ۵۰۰٬۰۰۰٬۰۰۰ ریال است.",
            FundingRules.error(500_000_001L),
        )
        // Inside the limits but outside the proposed range.
        assertEquals(
            "مقدار وارد شده از حداقل مبلغ مورد نیازکم تر است.",
            FundingRules.error(20_000L),
        )
        assertEquals("مقدار وارد شده از حداکثر مبلغ بیش تر است.", FundingRules.error(300_000_000L))
        assertNull(FundingRules.error(FundingFixture.DEFAULT_AMOUNT))
    }

    @Test
    fun stepperAddsWithoutCapAndSubtractsDownToZero() {
        assertEquals(1_100_000L, FundingRules.plus(1_000_000L))
        assertEquals(500_000_100_000L, FundingRules.plus(500_000_000_000L))
        assertEquals(900_000L, FundingRules.minus(1_000_000L))
        assertEquals(0L, FundingRules.minus(50_000L))
    }

    @Test
    fun fourGraphsShareOnlyTheExistingWalletReceipt() {
        assertEquals(15, FundingDestinations.implemented.size)
        assertFalse(WalletOverviewDestinations.RECEIPT in FundingDestinations.implemented)
        assertEquals(WalletOverviewDestinations.RECEIPT, FundingFlow.WALLET_STANDALONE.receipt)
        listOf("S-219", "S-218", "S-220", "S-229", "S-236").forEach {
            assertFalse(it in HomeDestinations.implemented)
        }
        FundingDestinations.implemented.forEach { assertTrue(it in FundingFlow.of(it).pages) }
    }
}
