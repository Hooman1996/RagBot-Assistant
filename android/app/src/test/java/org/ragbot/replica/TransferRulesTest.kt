package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class TransferRulesTest {
    @Test
    fun rialAmountsNormalizeDigitsWithoutFloatingPointOrImplicitUnitConversion() {
        assertEquals(1_250_000L, TransferRules.amountRial("۱۲۵۰۰۰۰"))
        assertEquals(125L, TransferRules.amountRial("١٢٥"))
        assertEquals(Long.MAX_VALUE, TransferRules.amountRial(Long.MAX_VALUE.toString()))
        assertNull(TransferRules.amountRial("9223372036854775808"))
        listOf("", "0", "-1", "+1", "1.5", "1,000", "۱٬۰۰۰", " 100", "NaN").forEach {
            assertNull(it, TransferRules.amountRial(it))
        }
    }

    @Test
    fun callerFieldLengthIsSeparateFromUnknownBankLimits() {
        assertEquals(99_999_999_999L, TransferRules.amountRial("99999999999", 11))
        assertNull(TransferRules.amountRial("100000000000", 11))
        assertEquals(100_000_000_000L, TransferRules.amountRial("100000000000"))
    }
}
