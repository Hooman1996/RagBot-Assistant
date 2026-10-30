package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class IndividualTransferRulesTest {
    @Test
    fun cardPreviewRejectsMalformedEqualAndOverflowingInputs() {
        val valid = CardTransferFixture.receipt
        assertTrue(CardTransferRules.errors(valid).isEmpty())
        assertTrue(CardTransferRules.errors(valid.copy(source = "123")).containsKey("source"))
        assertTrue(
            CardTransferRules.errors(valid.copy(destination = valid.source))
                .containsKey("destination")
        )
        assertTrue(
            CardTransferRules.errors(valid.copy(destination = "000000000000000X"))
                .containsKey("destination")
        )
        assertTrue(CardTransferRules.errors(valid.copy(amount = "0")).containsKey("amount"))
        assertTrue(
            CardTransferRules.errors(valid.copy(amount = "100000000000")).containsKey("amount")
        )
        assertTrue(CardTransferRules.errors(valid.copy(amount = "۲۵۰۰۰۰")).isEmpty())
    }

    @Test
    fun accountReviewRequiresSourceRecipientAmountAndSyntheticReason() {
        val valid =
            AccountTransferDraft(
                destination = "0000000000002",
                amount = "250000",
                reason = AccountTransferFixture.reasons.first(),
            )
        assertNull(AccountTransferFixture.validate(valid))
        listOf(
                valid.copy(sourceId = "missing"),
                valid.copy(destination = "123"),
                valid.copy(destination = "000000000000X"),
                valid.copy(amount = "-1"),
                valid.copy(amount = "9223372036854775808"),
                valid.copy(reason = ""),
            )
            .forEach { assertNotNull(AccountTransferFixture.validate(it)) }
    }

    @Test
    fun ibanChecksumRejectsMalformedInputWithoutClaimingBankEligibility() {
        val valid =
            AccountTransferDraft(
                mode = AccountTransferDestinations.INTERBANK,
                destination = "IR220000000000000000000001",
                amount = "250000",
                reason = AccountTransferFixture.reasons.first(),
            )
        assertNull(AccountTransferFixture.validate(valid))
        assertNull(
            AccountTransferFixture.validate(valid.copy(destination = "۲۲۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۱"))
        )
        listOf(
                "IR000000000000000000000001",
                "IR220000000000000000000001X",
                "garbage220000000000000000000001",
                "220000000000000000000001IR",
                "000000000000000000000000",
            )
            .forEach {
                assertNotNull(it, AccountTransferFixture.validate(valid.copy(destination = it)))
            }
    }
}
