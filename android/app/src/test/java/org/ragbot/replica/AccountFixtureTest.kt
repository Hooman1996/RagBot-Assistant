package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class AccountFixtureTest {
    @Test
    fun exactSignedMoneyAndUnknownBalancesRemainDistinct() {
        assertEquals("−۱۲۳", persianNumber(-123L))
        assertEquals("−۹٬۲۲۳٬۳۷۲٬۰۳۶٬۸۵۴٬۷۷۵٬۸۰۸", persianNumber(Long.MIN_VALUE))
        assertEquals("۹٬۲۲۳٬۳۷۲٬۰۳۶٬۸۵۴٬۷۷۵٬۸۰۷", persianNumber(Long.MAX_VALUE))
        assertEquals(12_500_000L, AccountFixture.accounts.first().balance)
        assertNull(AccountFixture.accounts.last().balance)
        assertTrue(AccountFixture.accounts.all { it.unit == "IRR" })
        assertEquals(3, AccountFixture.accounts.map { it.key }.toSet().size)
    }

    @Test
    fun searchAcceptsPersianArabicAndLatinDigitsWithoutLosingPrecision() {
        val second = AccountFixture.accounts[1]
        assertTrue(accountMatches(second, "9818460579"))
        assertTrue(accountMatches(second, "٩٨١٨٤٦٠٥٧٩"))
        assertTrue(accountMatches(second, "کاظمی"))
        assertFalse(accountMatches(second, "7234143231"))
        val transaction = HomeFixture.transactions.first()
        assertTrue(historyMatches(transaction, "۱٬۲۵۰٬۰۰۰"))
        assertTrue(historyMatches(transaction, "۱۲۵۰۰۰۰"))
        assertTrue(historyMatches(transaction, "کارت ملت"))
        assertFalse(historyMatches(transaction, "1.25"))
    }
}
