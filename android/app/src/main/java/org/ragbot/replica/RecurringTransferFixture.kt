package org.ragbot.replica

import java.io.Serializable

object RecurringTransferDestinations {
    const val MENU = "S-180"
    const val REQUEST = "S-181"
    const val EXECUTION = "S-177"
    const val HISTORY = "S-178"
    const val DETAIL = "S-179"
    // S-199 is only the host activity of this single repetitive-transactions route.
    const val REPETITIVE = "S-200"
    val implemented = setOf(MENU, REQUEST, EXECUTION, HISTORY, DETAIL, REPETITIVE)
}

/** RT-RECOVERY: non-secret local schedule request. Nothing is scheduled or submitted. */
data class ScheduleDraft(
    val title: String = "",
    val start: String = "",
    val end: String = "",
    val frequency: String = "",
    val day: Int = 0,
    val transferType: String = "",
) : Serializable

data class ScheduleExecution(val date: String, val status: String) : Serializable

/** A synthetic standing-order record; executions use the recovered order statuses. */
data class ContinuousRecord(
    val traceId: String,
    val name: String,
    val type: String,
    val source: String,
    val destination: String,
    val amount: Long,
    val created: String,
    val start: String,
    val end: String,
    val frequency: String,
    val cancelled: Boolean,
    val executions: List<ScheduleExecution>,
) : Serializable

data class RepetitiveRecord(
    val id: String,
    val type: String,
    val title: String,
    val source: String,
    val destination: String,
    val destinationName: String,
    val bank: String,
    val amount: Long,
    val created: String,
) : Serializable

internal object RecurringTransferRules {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val NOT_STARTED = "NOT_STARTED"
    const val CANCELLED = "CANCELLED"
    const val MONTH_WARNING =
        "با انتخاب تاریخ 30 و 31 ام ماه، برای نیمه دوم سال، 31 به 30 و برای اسفند به 29 یا 30 ام تغییر می‌یابد."
    private const val DAY_MILLIS = 86_400_000L

    val frequencies = listOf(DAILY to "روزانه", WEEKLY to "هفتگی", MONTHLY to "ماهانه")
    // Recovered Saturday-first order with each day's recovered order value.
    val weekDays =
        listOf(
            "شنبه" to 6,
            "یکشنبه" to 7,
            "دوشنبه" to 1,
            "سه‌شنبه" to 2,
            "چهارشنبه" to 3,
            "پنجشنبه" to 4,
            "جمعه" to 5,
        )
    val orderStatuses =
        mapOf(
            "SUCCESS" to "موفق",
            "FAILED" to "ناموفق",
            "PROCESS_FAILED" to "خطا در پردازش",
            NOT_STARTED to "در انتظار اجرا",
            CANCELLED to "لغو شده",
            "UNKNOWN" to "نامشخص",
        )

    fun frequencyTitle(frequency: String, day: Int): String {
        val title = frequencies.firstOrNull { it.first == frequency }?.second ?: "تعریف نشده"
        val weekDay = weekDays.firstOrNull { it.second == day }?.first
        return when {
            frequency == WEEKLY && weekDay != null -> "$title ($weekDay)"
            frequency == MONTHLY && day in 1..31 -> "$title (${persianNumber(day.toLong())} ام)"
            else -> title
        }
    }

    /** Recovered date checks on parsed day starts: start not after end, span within a year. */
    fun datesError(start: Long?, end: Long?): String? =
        when {
            start == null || end == null -> "تاریخ را به صورت سال/ماه/روز وارد کنید."
            start > end -> "تاریخ ابتدایی باید قبل از تاریخ انتهایی بازه زمانی باشد."
            // Designed for mock: 365 days; the original allows 365 or 366 by a year predicate.
            (end - start) / DAY_MILLIS > 365 -> "فاصله تاریخ شروع و پایان باید کمتر از یک سال باشد."
            else -> null
        }

    fun requestError(draft: ScheduleDraft, start: Long?, end: Long?): String? =
        when {
            draft.title.isBlank() -> "عنوان درخواست را وارد کنید."
            draft.frequency !in frequencies.map { it.first } ->
                "نوع تکرار انتقال وجه را انتخاب کنید."
            draft.frequency == WEEKLY && weekDays.none { it.second == draft.day } ->
                "روز هفته را انتخاب کنید."
            draft.frequency == MONTHLY && draft.day !in 1..31 -> "روز ماه را انتخاب کنید."
            else -> datesError(start, end)
        }

    fun monthWarning(draft: ScheduleDraft) = draft.frequency == MONTHLY && draft.day >= 30

    /**
     * Recovered details action: back when cancelled, delete before any run, else cancel waiting
     * runs.
     */
    fun detailAction(record: ContinuousRecord): String =
        when {
            record.cancelled -> "BACK"
            record.executions.all { it.status == NOT_STARTED } -> "DELETE"
            record.executions.any { it.status == NOT_STARTED } -> "CANCEL"
            else -> "BACK"
        }

    /** Recovered cancel scope: only waiting executions become cancelled. */
    fun cancel(record: ContinuousRecord): ContinuousRecord =
        record.copy(
            cancelled = true,
            executions =
                record.executions.map {
                    if (it.status == NOT_STARTED) it.copy(status = CANCELLED) else it
                },
        )
}

internal object RecurringTransferFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ انتقال خودکاری ثبت یا اجرا نمی‌شود."

    val records =
        listOf(
            ContinuousRecord(
                "DEMO-STANDING-0001",
                "اجارهٔ نمایشی",
                AccountTransferDestinations.INTERNAL,
                "0000000000001",
                "0000000000002",
                1_500_000L,
                "۱۴۰۵/۰۷/۰۱",
                "۱۴۰۵/۰۷/۰۱",
                "۱۴۰۵/۱۲/۰۱",
                RecurringTransferRules.MONTHLY,
                false,
                listOf(
                    ScheduleExecution("۱۴۰۵/۰۷/۰۱", "SUCCESS"),
                    ScheduleExecution("۱۴۰۵/۰۸/۰۱", "FAILED"),
                    ScheduleExecution("۱۴۰۵/۰۹/۰۱", RecurringTransferRules.NOT_STARTED),
                    ScheduleExecution("۱۴۰۵/۱۰/۰۱", RecurringTransferRules.NOT_STARTED),
                ),
            ),
            ContinuousRecord(
                "DEMO-STANDING-0002",
                "پس‌انداز هفتگی نمایشی",
                AccountTransferDestinations.INTERBANK,
                "0000000000001",
                "IR920000000000000000000002",
                250_000L,
                "۱۴۰۵/۰۸/۰۵",
                "۱۴۰۵/۰۹/۰۱",
                "۱۴۰۵/۰۹/۲۹",
                RecurringTransferRules.WEEKLY,
                false,
                listOf(
                    ScheduleExecution("۱۴۰۵/۰۹/۰۱", RecurringTransferRules.NOT_STARTED),
                    ScheduleExecution("۱۴۰۵/۰۹/۰۸", RecurringTransferRules.NOT_STARTED),
                ),
            ),
            ContinuousRecord(
                "DEMO-STANDING-0003",
                "قسط لغوشدهٔ نمایشی",
                AccountTransferDestinations.INTERNAL,
                "0000000000001",
                "0000000000003",
                400_000L,
                "۱۴۰۵/۰۵/۱۰",
                "۱۴۰۵/۰۵/۱۰",
                "۱۴۰۵/۰۷/۱۰",
                RecurringTransferRules.MONTHLY,
                true,
                listOf(
                    ScheduleExecution("۱۴۰۵/۰۵/۱۰", "SUCCESS"),
                    ScheduleExecution("۱۴۰۵/۰۶/۱۰", RecurringTransferRules.CANCELLED),
                ),
            ),
        )

    val repetitive =
        listOf(
            RepetitiveRecord(
                "repeat-card",
                "CARD_TO_CARD",
                "کارت به کارت نمایشی",
                CardTransferFixture.source,
                CardTransferFixture.destination,
                "گیرندهٔ نمایشی",
                "بانک نمایشی",
                250_000L,
                "۱۴۰۵/۰۸/۰۱",
            ),
            RepetitiveRecord(
                "repeat-internal",
                "INTERNAL",
                "انتقال داخلی نمایشی",
                "account-1",
                "0000000000002",
                "گیرندهٔ نمایشی",
                "بانک نمایشی",
                300_000L,
                "۱۴۰۵/۰۸/۰۲",
            ),
            RepetitiveRecord(
                "repeat-paya",
                "PAYA",
                "پایای نمایشی",
                "account-1",
                "IR220000000000000000000001",
                "گیرندهٔ نمایشی",
                "بانک نمایشی",
                500_000L,
                "۱۴۰۵/۰۸/۰۳",
            ),
            RepetitiveRecord(
                "repeat-charge",
                "CHARGE",
                "شارژ نمایشی",
                "account-1",
                "09000000000",
                "اپراتور نمایشی",
                "-",
                100_000L,
                "۱۴۰۵/۰۸/۰۴",
            ),
        )

    val typeTitles =
        mapOf(
            "CARD_TO_CARD" to "کارت به کارت",
            "CHARGE" to "خرید شارژ",
            "INTERNAL" to "انتقال داخلی",
            "PAYA" to "پایا",
            "POL" to "پل",
            "SATNA" to "ساتنا",
        )
}
