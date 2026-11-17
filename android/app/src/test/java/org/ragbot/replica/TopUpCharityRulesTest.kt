package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class TopUpCharityRulesTest {
    private val irancell = checkNotNull(TopUpRules.config("IRANCELL"))
    private val valid = TopUpDraft("account-1", "09358346703", "IRANCELL", "100000")

    @Test
    fun prefixesSelectTheOperatorFromFourDigits() {
        assertEquals("", TopUpRules.detect("090", "IRANCELL"))
        assertEquals("IRANCELL", TopUpRules.detect("0935", ""))
        assertEquals("RIGHTEL", TopUpRules.detect("۰۹۲۱۱", "IRANCELL"))
        assertEquals("HAMRAH_AVAL", TopUpRules.detect("0999", "HAMRAH_AVAL"))
    }

    @Test
    fun phoneMustBeElevenDigitsStartingWithZeroNine() {
        assertEquals(PaymentRules.MANDATORY, TopUpRules.phoneError(""))
        assertEquals(PaymentRules.INVALID, TopUpRules.phoneError("0935834670"))
        assertEquals(PaymentRules.INVALID, TopUpRules.phoneError("08013206651"))
        assertNull(TopUpRules.phoneError("۰۹۳۵۸۳۴۶۷۰۳"))
    }

    @Test
    fun amountChecksMinimumThenMaximumThenMultiplier() {
        assertTrue(TopUpRules.amountError(irancell, "40000")!!.startsWith("حداقل"))
        assertTrue(TopUpRules.amountError(irancell, "5050000")!!.startsWith("حداکثر"))
        assertTrue(TopUpRules.amountError(irancell, "99999999999999999999")!!.startsWith("حداکثر"))
        assertTrue(TopUpRules.amountError(irancell, "75000")!!.contains("ضریبی"))
        assertNull(TopUpRules.amountError(irancell, "5000000"))
        assertNotNull(TopUpRules.amountError(checkNotNull(TopUpRules.config("RIGHTEL")), "150000"))
    }

    @Test
    fun eachSourceModeAndMissingOperatorAreReported() {
        assertTrue(TopUpRules.errors(TopUpDestinations.ACCOUNT, valid).isEmpty())
        assertEquals(
            setOf("source", "phone", "operator", "amount"),
            TopUpRules.errors(TopUpDestinations.CARD, TopUpDraft()).keys,
        )
        assertEquals(
            PaymentRules.INVALID,
            TopUpRules.errors(TopUpDestinations.WITHOUT_SOURCE, valid)["source"],
        )
        assertEquals(
            TopUpRules.CHOOSE_OPERATOR,
            TopUpRules.errors(TopUpDestinations.ACCOUNT, valid.copy(operator = ""))["operator"],
        )
    }

    @Test
    fun charityAmountIsRequiredAndWithinTheRange() {
        assertEquals(PaymentRules.MANDATORY, CharityRules.amountError(""))
        assertEquals(PaymentRules.OUT_OF_RANGE, CharityRules.amountError("9999"))
        assertEquals(PaymentRules.OUT_OF_RANGE, CharityRules.amountError("100000001"))
        assertNull(CharityRules.amountError("۱۰۰۰۰"))
    }
}
