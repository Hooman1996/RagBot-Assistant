package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class CardFixtureTest {
    @Test
    fun cardAccountAndCreditAmountsStayDistinctExactIntegers() {
        val byKey = CardFixture.cards.associateBy { it.key }
        assertEquals(0L, byKey.getValue("card-2").cardBalance)
        assertEquals(3_750_000L, byKey.getValue("card-2").accountBalance)
        assertNull(byKey.getValue("card-3").cardBalance)
        assertTrue(CardFixture.cards.all { it.unit == "IRR" })
        val credit = CardFixture.credit.getValue("card-4")
        assertEquals("IRR", credit.unit)
        assertEquals(500_000_000L, credit.creditLimit)
        assertEquals(320_000_000L, credit.remainingCredit)
        assertEquals(180_000_000L, credit.totalPayable)
        assertEquals(0L, CardFixture.credit.getValue("card-5").totalPayable)
        assertEquals(CardFixture.cards.size, CardFixture.cards.map { it.key }.toSet().size)
    }

    @Test
    fun numbersAreGroupedWithoutClientSideMasking() {
        assertEquals("۰۰۰۰ ۰۰۰۰ ۰۰۰۰ ۰۰۰۱", cardNumberGroups("0000000000000001"))
        assertEquals("۰۰۰۰ ۰۰** **** ۰۰۰۳", cardNumberGroups("000000******0003"))
        val second = CardFixture.cards[1]
        assertTrue(cardMatches(second, "۰۰۰۲"))
        assertTrue(cardMatches(second, "کاربر"))
        assertFalse(cardMatches(second, "0001"))
    }

    @Test
    fun secureBalanceFormAppliesRecoveredChecks() {
        val empty = secureFormErrors("", "", "", "")
        assertEquals(setOf("number", "cvv2", "expiry", "password"), empty.keys)
        assertTrue(empty.getValue("cvv2").contains("الزامی"))
        assertEquals(setOf("cvv2", "password"), secureFormErrors(null, "123", "۰۵/۰۸", "1234").keys)
        assertEquals(
            emptyMap<String, String>(),
            secureFormErrors("0000000000000001", "1234", "05/08", "12345"),
        )
        assertTrue("expiry" in secureFormErrors(null, "1234", "13/08", "12345"))
    }

    @Test
    fun everyActionEitherOpensAnImplementedPageOrIsExplicitlyUnavailable() {
        val implemented = HomeDestinations.implemented
        CardFixture.actions.values.flatten().forEach {
            assertTrue(it.destination == null || it.destination in implemented)
        }
        assertEquals(CardKind.entries.toSet(), CardFixture.actions.keys)
    }
}
