package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class RecurringTransferRulesTest {
    private val day = 86_400_000L
    private val valid =
        ScheduleDraft("اجاره", "1405/09/01", "1405/12/01", RecurringTransferRules.MONTHLY, 15)

    @Test
    fun datesMustBeOrderedAndWithinOneYear() {
        assertNull(RecurringTransferRules.datesError(0, 0))
        assertNull(RecurringTransferRules.datesError(0, 365 * day))
        assertEquals(
            "تاریخ ابتدایی باید قبل از تاریخ انتهایی بازه زمانی باشد.",
            RecurringTransferRules.datesError(day, 0),
        )
        assertEquals(
            "فاصله تاریخ شروع و پایان باید کمتر از یک سال باشد.",
            RecurringTransferRules.datesError(0, 366 * day),
        )
        assertNotNull(RecurringTransferRules.datesError(null, 0))
    }

    @Test
    fun requestNeedsTitleFrequencyAndItsDay() {
        assertNull(RecurringTransferRules.requestError(valid, 0, 90 * day))
        listOf(
                valid.copy(title = " "),
                valid.copy(frequency = ""),
                valid.copy(day = 0),
                valid.copy(day = 32),
                valid.copy(frequency = RecurringTransferRules.WEEKLY, day = 8),
            )
            .forEach {
                assertNotNull(it.toString(), RecurringTransferRules.requestError(it, 0, day))
            }
        assertNull(
            RecurringTransferRules.requestError(
                valid.copy(frequency = RecurringTransferRules.DAILY, day = 0),
                0,
                day,
            )
        )
        assertNull(
            RecurringTransferRules.requestError(
                valid.copy(frequency = RecurringTransferRules.WEEKLY, day = 6),
                0,
                day,
            )
        )
    }

    @Test
    fun monthEndWarningAndTitlesFollowRecoveredDays() {
        assertFalse(RecurringTransferRules.monthWarning(valid.copy(day = 29)))
        assertTrue(RecurringTransferRules.monthWarning(valid.copy(day = 30)))
        assertTrue(RecurringTransferRules.monthWarning(valid.copy(day = 31)))
        assertEquals("ماهانه (۱۵ ام)", RecurringTransferRules.frequencyTitle("MONTHLY", 15))
        assertEquals("هفتگی (شنبه)", RecurringTransferRules.frequencyTitle("WEEKLY", 6))
        assertEquals("تعریف نشده", RecurringTransferRules.frequencyTitle("OTHER", 0))
        assertEquals("ماهانه", RecurringTransferRules.frequencyTitle("MONTHLY", 0))
        assertEquals("هفتگی", RecurringTransferRules.frequencyTitle("WEEKLY", 0))
        assertEquals("شنبه", RecurringTransferRules.weekDays.first().first)
    }

    @Test
    fun detailActionAndCancelOnlyTouchWaitingExecutions() {
        val (partly, waiting, cancelled) = RecurringTransferFixture.records
        assertEquals("CANCEL", RecurringTransferRules.detailAction(partly))
        assertEquals("DELETE", RecurringTransferRules.detailAction(waiting))
        assertEquals("BACK", RecurringTransferRules.detailAction(cancelled))
        val done = partly.copy(executions = partly.executions.map { it.copy(status = "SUCCESS") })
        assertEquals("BACK", RecurringTransferRules.detailAction(done))
        val after = RecurringTransferRules.cancel(partly)
        assertEquals(
            listOf("SUCCESS", "FAILED", "CANCELLED", "CANCELLED"),
            after.executions.map { it.status },
        )
        assertEquals("BACK", RecurringTransferRules.detailAction(after))
    }
}
