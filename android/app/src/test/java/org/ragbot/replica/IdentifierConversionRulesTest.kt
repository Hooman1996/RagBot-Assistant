package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class IdentifierConversionRulesTest {
    @Test
    fun checksumValidUnknownBankCannotBecomeAnOwnershipResult() {
        val identifier = "740020000000000000000001"
        assertTrue(AccountTransferFixture.validIban(identifier))
        for (mode in listOf(ConversionMode.IBAN_TO_ACCOUNT, ConversionMode.IBAN_TO_CARD)) {
            val draft = ConversionDraft(mode, identifier)
            assertNotNull(ConversionFixture.error(draft))
            assertNull(ConversionFixture.result(draft))
            assertFalse(ConversionFixture.paidQuery(draft))
        }
    }

    @Test
    fun accountNeedsBankAndRespectsInputLimitWithoutInventingAnAlgorithm() {
        val draft = ConversionDraft(identifier = ConversionFixture.account, bankKey = "internal")
        assertNull(ConversionFixture.error(draft))
        assertNotNull(ConversionFixture.error(draft.copy(identifier = "")))
        assertNotNull(ConversionFixture.error(draft.copy(bankKey = "")))
        assertNotNull(ConversionFixture.error(draft.copy(bankKey = "unknown")))
        assertNull(ConversionFixture.error(draft.copy(identifier = "0".repeat(30))))
        assertNotNull(ConversionFixture.error(draft.copy(identifier = "0".repeat(31))))
        assertEquals(
            "000.000.000001.1",
            ConversionFixture.normalizeIdentifier(draft.mode, " ۰۰۰.۰۰۰.۰۰۰۰۰۱.۱ "),
        )
        assertNull(ConversionFixture.result(draft.copy(identifier = "000.000.000001.1")))
    }

    @Test
    fun cardInputNeedsSixteenDigitsAndKnownSyntheticBankWithoutLuhn() {
        for (mode in listOf(ConversionMode.CARD_TO_IBAN, ConversionMode.CARD_TO_ACCOUNT)) {
            val draft = ConversionDraft(mode, ConversionFixture.card)
            assertNull(ConversionFixture.error(draft))
            assertNull(ConversionFixture.error(draft.copy(identifier = "۰۰۰۰ ۰۰۰۰ ۰۰۰۰ ۰۰۰۱")))
            for (identifier in
                listOf(
                    "",
                    "0".repeat(15),
                    "0".repeat(17),
                    "000000000000000X",
                    "1111110000000001",
                )) {
                assertNotNull(
                    identifier,
                    ConversionFixture.error(draft.copy(identifier = identifier)),
                )
            }
            assertFalse(ConversionFixture.paidQuery(draft))
            val paid = draft.copy(identifier = "0000010000000001")
            assertNull(ConversionFixture.error(paid))
            assertTrue(ConversionFixture.paidQuery(paid))
            assertNull(ConversionFixture.result(paid))
        }
    }

    @Test
    fun ibanUsesRecoveredChecksumAndDigitNormalization() {
        val draft = ConversionDraft(ConversionMode.IBAN_TO_ACCOUNT, ConversionFixture.iban)
        for (identifier in
            listOf(
                ConversionFixture.iban,
                "۲۲۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۰۱",
                "ir22 0000 0000 0000 0000 0000 01",
                "220000000000000000000001IR",
            )) {
            assertNull(identifier, ConversionFixture.error(draft.copy(identifier = identifier)))
        }
        for (identifier in
            listOf(
                "",
                "IR000000000000000000000001",
                "0".repeat(24),
                "2".repeat(24),
                "22000000000000000000000",
            )) {
            assertNotNull(identifier, ConversionFixture.error(draft.copy(identifier = identifier)))
        }
    }

    @Test
    fun checksumValidityDoesNotEstablishBankEligibilityOrOwnership() {
        val otherBank = "480010000000000000000001"
        assertTrue(AccountTransferFixture.validIban(otherBank))
        for (mode in listOf(ConversionMode.IBAN_TO_ACCOUNT, ConversionMode.IBAN_TO_CARD)) {
            val other = ConversionDraft(mode, otherBank)
            assertNotNull(ConversionFixture.error(other))
            assertNull(ConversionFixture.result(other))
            assertFalse(ConversionFixture.paidQuery(other))
            val unknownRecord = ConversionDraft(mode, "IR920000000000000000000002")
            assertNull(ConversionFixture.error(unknownRecord))
            assertNull(ConversionFixture.result(unknownRecord))
        }
    }

    @Test
    fun allFiveKnownConversionsReturnOnlyExplicitSyntheticOwnership() {
        for (mode in ConversionMode.entries) {
            val identifier =
                when {
                    mode.isCardInput -> ConversionFixture.card
                    mode.isIbanInput -> ConversionFixture.iban
                    else -> ConversionFixture.account
                }
            val draft = ConversionDraft(mode, identifier, "internal")
            val result = ConversionFixture.result(draft)!!
            assertEquals("دارندهٔ ساختگی", result.owner)
            assertEquals(ConversionFixture.account, result.account)
            assertEquals(ConversionFixture.iban, result.iban)
            assertEquals(ConversionFixture.cards, result.cards)
            assertEquals(2, result.cards.size)
            assertFalse(ConversionFixture.paidQuery(draft))
        }
    }

    @Test
    fun otherBankAccountStopsAtPaidGateAndUnknownLocalRecordHasNoResult() {
        val account = ConversionDraft(identifier = ConversionFixture.account, bankKey = "external")
        assertNull(ConversionFixture.error(account))
        assertTrue(ConversionFixture.paidQuery(account))
        assertNull(ConversionFixture.result(account))
        val unknown = account.copy(identifier = "0000000000999", bankKey = "internal")
        assertNull(ConversionFixture.error(unknown))
        assertNull(ConversionFixture.result(unknown))
        val unknownCard = ConversionDraft(ConversionMode.CARD_TO_IBAN, "0000000000000999")
        assertNull(ConversionFixture.error(unknownCard))
        assertNull(ConversionFixture.result(unknownCard))
    }
}
