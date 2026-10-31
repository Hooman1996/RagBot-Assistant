package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class LoanGroupRulesTest {
    @Test
    fun eligibilityStopsAtTheFirstApplicableGate() {
        val group = LoanGroupFixture.groups.first()
        assertNull(LoanGroupFixture.unavailableReason(group))
        assertEquals(
            "این گروه در دسترس نیست.",
            LoanGroupFixture.unavailableReason(
                group.copy(active = false, accountOpenElapsed = false)
            ),
        )
        assertEquals(
            "مدت لازم از زمان افتتاح حساب نگذشته است.",
            LoanGroupFixture.unavailableReason(
                group.copy(accountOpenElapsed = false, needsCredit = true)
            ),
        )
        assertTrue(
            LoanGroupFixture.unavailableReason(group.copy(needsCredit = true))!!.contains("استعلام")
        )
    }

    @Test
    fun warningRequiresAnActiveOptionWithALowerAvailablePeriod() {
        val option = LoanGroupFixture.options.first()
        assertNull(option.warningMonths)
        assertEquals(6, option.copy(availableMonths = 6).warningMonths)
        assertNull(option.copy(active = false, availableMonths = 6).warningMonths)
        assertNull(option.copy(availableMonths = 18).warningMonths)
        assertEquals(
            listOf(100_000_000L, 200_000_000L),
            LoanGroupFixture.options.groupBy { it.amountRial }.keys.toList(),
        )
    }
}
