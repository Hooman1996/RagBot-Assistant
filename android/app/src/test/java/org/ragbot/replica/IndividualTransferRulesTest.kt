package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class IndividualTransferRulesTest {
    @Test
    fun cardPreviewRejectsMalformedEqualAndOverflowingInputs() {
        val valid = CardTransferDraft("6104336383233566", "6104334806064544", "250000")
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
                destination = "0212355097104",
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
                destination = "IR320120470240363429009957",
                amount = "250000",
                reason = AccountTransferFixture.reasons.first(),
            )
        assertNull(AccountTransferFixture.validate(valid))
        assertNull(
            AccountTransferFixture.validate(valid.copy(destination = "۳۲۰۱۲۰۴۷۰۲۴۰۳۶۳۴۲۹۰۰۹۹۵۷"))
        )
        listOf(
                "IR100120470240363429009957",
                "IR320120470240363429009957X",
                "garbage320120470240363429009957",
                "320120470240363429009957IR",
                "000000000000000000000000",
            )
            .forEach {
                assertNotNull(it, AccountTransferFixture.validate(valid.copy(destination = it)))
            }
    }
}
