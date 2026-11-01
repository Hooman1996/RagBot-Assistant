package org.ragbot.replica

import java.io.Serializable

internal object ChequeDestinations {
    const val MENU = "S-147"
    const val BOOKS = "S-142"
    const val CHEQUES = "S-146"
    const val TRACK_MY = "S-149"
    const val TRACK_TRANSFERRED = "S-150"
    const val REPORT_FILTER = "S-148"
    const val REPORT = "S-153"
    const val ISSUE = "S-143"
    const val ISSUE_CONFIRM = "S-144"
    const val ISSUANCE = "S-145"
    const val CARDBOARD = "S-154"
    const val PAY_DETAIL = "S-155"
    const val REQUEST_DETAIL = "S-156"
    val implemented =
        setOf(
            MENU,
            REPORT_FILTER,
            BOOKS,
            CHEQUES,
            TRACK_MY,
            TRACK_TRANSFERRED,
            REPORT,
            ISSUE,
            ISSUE_CONFIRM,
            ISSUANCE,
            CARDBOARD,
            PAY_DETAIL,
            REQUEST_DETAIL,
        )
}

/** Recovered cheque media; the issue form receives one from the menu. */
internal enum class ChequeMedia(val title: String, val request: String) {
    PAPER("کاغذی", "درخواست صدور دسته چک کاغذی"),
    DIGITAL("دیجیتال", "درخواست صدور دسته چک دیجیتال"),
}

internal data class ChequeBook(
    val reference: String,
    val accountKey: String,
    val pageCount: Int,
    val startNo: String,
    val endNo: String,
    val issueDate: String,
    val docType: String,
    // Labels recovered; which server counts fill them is Unknown.
    val summary: List<Pair<String, Int>>,
)

internal data class ChequePaper(
    val no: String,
    val amount: Long?,
    val sayadId: String,
    val branch: String,
    val date: String,
    val statusDesc: String,
    val lastStatusDate: String,
)

internal data class TransferredCheque(
    val no: String,
    val amount: Long,
    val date: String,
    val type: String,
    val subtype: String,
)

internal data class IssuanceRecord(
    val trace: String,
    val accountKey: String,
    val media: ChequeMedia,
    val pageCount: String,
    val date: String,
    val status: String,
    val rejectReason: String?,
)

/** Recovered card board task fields; [pichak] tasks open S-155, others S-156. */
internal data class WorkflowTask(
    val id: String,
    val tab: String,
    val processKey: String,
    val processName: String,
    val issuer: String,
    val date: String,
    val statusDesc: String,
    val amount: Long,
) {
    val pichak
        get() = processKey.startsWith("pichak_")
}

/** Recovered report subtypes in their spinner order. */
internal val chequeReportSubtypes =
    listOf("PASSED" to "وصولی", "RETURNED" to "برگشتی", "RECEIVING" to "در جریان")

/** S-148 filter; [type] is INTERNAL or OTHER, passed by the menu key. */
internal data class ChequeReportFilter(
    val type: String = "OTHER",
    val subtype: String? = null,
    val from: String = "",
    val to: String = "",
) : Serializable {
    // Recovered: only other-bank reports outside "receiving" take a date range.
    val dated
        get() = type == "OTHER" && subtype != "RECEIVING"
}

/** Non-secret issue form fields; no OTP is kept. */
internal data class ChequeIssueDraft(
    val media: ChequeMedia = ChequeMedia.PAPER,
    val accountKey: String = "",
    val pageCount: String = "",
) : Serializable

internal object ChequeRules {
    // Recovered: only an empty cheque number is rejected locally.
    fun chequeNoError(value: String) = if (value.isBlank()) PaymentRules.MANDATORY else null

    /** Recovered input filter: drop a trailing character that is not a digit or '/'. */
    fun chequeNoInput(value: String): String =
        latinDigits(value).let {
            if (it.isNotEmpty() && it.last() !in "0123456789/") it.dropLast(1) else it
        }

    /** Designed for mock lookup of the tracked cheque. */
    fun track(accountKey: String, chequeNo: String): ChequePaper? =
        ChequeFixture.books
            .filter { it.accountKey == accountKey }
            .flatMap { ChequeFixture.cheques(it) }
            .find { it.no == latinDigits(chequeNo).trim() }

    fun search(cheques: List<ChequePaper>, query: String) =
        cheques.filter { latinDigits(query).let { q -> q.isEmpty() || q in it.no } }

    /** Recovered S-148 checks: status first, then the date range of a dated report. */
    fun reportErrors(filter: ChequeReportFilter): Map<String, String> = buildMap {
        if (filter.subtype == null) {
            put("status", PaymentRules.MANDATORY)
            return@buildMap
        }
        if (!filter.dated) return@buildMap
        val from = persianDateMillis(filter.from)
        val to = persianDateMillis(filter.to)
        if (from != null && to != null) {
            if (from > to) {
                put("range", "تاریخ ابتدایی باید قبل از تاریخ انتهایی بازه زمانی باشد.")
                return@buildMap
            }
            if ((to - from) / 86_400_000L > 30) {
                put("range", "بازه زمانی انتخابی، باید کمتر از 30 روز باشد.")
                return@buildMap
            }
        }
        if (from == null) put("from", PaymentRules.MANDATORY)
        if (to == null) put("to", PaymentRules.MANDATORY)
    }

    fun searchReport(rows: List<TransferredCheque>, query: String) =
        rows.filter {
            latinDigits(query).let { q -> q.isEmpty() || q in it.no || q in it.amount.toString() }
        }

    // Designed for mock: a shared account sends the request to the other members' card board.
    fun cardBoard(accountKey: String) =
        AccountFixture.accounts.find { it.key == accountKey }?.ownership == "مشترک"

    fun issueErrors(draft: ChequeIssueDraft): Map<String, String> = buildMap {
        if (draft.accountKey.isEmpty()) put("account", PaymentRules.MANDATORY)
        if (draft.pageCount.isEmpty()) put("page", PaymentRules.MANDATORY)
    }
}

internal object ChequeFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ درخواست یا عملیات چکی ارسال نمی‌شود."
    const val FEE = 150_000L

    // Recovered menu keys and targets; titles are server-supplied, so synthetic here.
    val menu =
        listOf(
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_CHEQUEBOOK",
                "دسته چک‌ها",
                ChequeDestinations.BOOKS,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_PAPER_CHECKBOOK",
                "درخواست دسته چک کاغذی",
                ChequeDestinations.ISSUE,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_DIGITAL_CHECKBOOK",
                "درخواست دسته چک دیجیتال",
                ChequeDestinations.ISSUE,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRACE_ISSUANCE_CHECKBOOK",
                "پیگیری درخواست دسته چک",
                ChequeDestinations.ISSUANCE,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_MY_CHEQUE_TRACE",
                "پیگیری چک‌های من",
                ChequeDestinations.TRACK_MY,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_FOLLOWUP",
                "پیگیری چک‌های واگذار شده",
                ChequeDestinations.TRACK_TRANSFERRED,
            ),
            // Recovered: both report keys open S-148, with other-bank or internal cheques.
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_REPORT",
                "گزارش چک‌های واگذار شده",
                ChequeDestinations.REPORT_FILTER,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_KARAFARIN_TRANSFERRED_CHEQUES_REPORT",
                "گزارش چک‌های واگذار شده داخلی",
                ChequeDestinations.REPORT_FILTER,
            ),
            // DC-RECOVERY: the digital-cheque keys and their targets.
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_CHAKAD_ACTIVATION",
                "فعالسازی چکاد",
                DigitalChequeDestinations.ACTIVATION,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFER_DIGITAL_CHEQUE",
                "واگذاری چک دیجیتال",
                DigitalChequeDestinations.TRANSFER,
            ),
            WalletMenuItem(
                "AUTHENTICATED_HOME_CHEQUE_CHAKAD_CARTABLE_DIGITAL_CHEQUE",
                "کارتابل چک دیجیتال",
                DigitalChequeDestinations.CARTABLE,
            ),
            WalletMenuItem("AUTHENTICATED_HOME_CHEQUE_PICHAK_ISSUE_PAPER_CHEQUE", "صدور چک", null),
            WalletMenuItem("AUTHENTICATED_HOME_CHEQUE_INQUIRY_BYHOLDER", "استعلام چک", null),
        )

    // Server-supplied page counts per media; Designed for mock.
    val pageCounts =
        mapOf(
            ChequeMedia.PAPER to listOf("10", "25", "50"),
            ChequeMedia.DIGITAL to listOf("10", "20"),
        )

    val books =
        listOf(
            ChequeBook(
                "DEMO-BOOK-01",
                "account-1",
                25,
                "534001",
                "534025",
                "۱۴۰۵/۰۲/۱۰",
                "دسته چک کاغذی",
                listOf(
                    "نقد شده" to 6,
                    "ثبت شده" to 3,
                    "برگشتی و مسدود" to 1,
                    "قابل استفاده" to 14,
                    "سایر" to 1,
                ),
            ),
            ChequeBook(
                "DEMO-BOOK-02",
                "account-2",
                10,
                "229901",
                "229910",
                "۱۴۰۵/۰۶/۰۱",
                "دسته چک دیجیتال",
                listOf(
                    "نقد شده" to 0,
                    "ثبت شده" to 1,
                    "برگشتی و مسدود" to 0,
                    "قابل استفاده" to 9,
                    "سایر" to 0,
                ),
            ),
        )

    // Status descriptions come from the server; these are synthetic.
    fun cheques(book: ChequeBook): List<ChequePaper> =
        (0 until 4).map { i ->
            val no = (book.startNo.toLong() + i).toString()
            val (status, amount) =
                when (i) {
                    0 -> "نقد شده" to 12_500_000L
                    1 -> "برگشتی" to 3_000_000L
                    2 -> "ثبت شده" to 7_250_000L
                    else -> "استفاده نشده" to null
                }
            ChequePaper(
                no,
                amount,
                "00000000000${100 + i}",
                "شعبه نمایشی",
                "۱۴۰۵/۰۷/${persianNumber(10L + i)}",
                status,
                "۱۴۰۵/۰۷/${persianNumber(20L + i)}",
            )
        }

    val report =
        listOf(
            TransferredCheque("880001", 4_000_000L, "۱۴۰۵/۰۷/۰۵", "INTERNAL", "PASSED"),
            TransferredCheque("880002", 9_500_000L, "۱۴۰۵/۰۷/۰۸", "INTERNAL", "PASSED"),
            TransferredCheque("880003", 1_800_000L, "۱۴۰۵/۰۷/۰۹", "INTERNAL", "RETURNED"),
            TransferredCheque("990001", 2_200_000L, "۱۴۰۵/۰۷/۱۱", "OTHER", "PASSED"),
            TransferredCheque("990002", 6_000_000L, "۱۴۰۵/۰۷/۱۲", "OTHER", "RECEIVING"),
        )

    val issuance =
        listOf(
            IssuanceRecord(
                "DEMO-TRACE-201",
                "account-1",
                ChequeMedia.PAPER,
                "25",
                "۱۴۰۵/۰۷/۰۱",
                "در حال بررسی",
                null,
            ),
            IssuanceRecord(
                "DEMO-TRACE-202",
                "account-2",
                ChequeMedia.DIGITAL,
                "10",
                "۱۴۰۵/۰۶/۲۰",
                "رد شده",
                "دلیل رد نمایشی",
            ),
        )

    // Recovered tabs: waiting, track and done.
    val tabs = listOf("WAITING" to "در انتظار اقدام", "TRACK" to "پیگیری", "DONE" to "انجام شده")

    val tasks =
        listOf(
            WorkflowTask(
                "task-1",
                "WAITING",
                "cheque_book_request_process",
                "درخواست دسته چک",
                "عضو نمایشی",
                "۱۴۰۵/۰۷/۲۵",
                "در انتظار تایید",
                ChequeFixture.FEE,
            ),
            WorkflowTask(
                "task-2",
                "WAITING",
                "pichak_issue_process",
                "صدور چک کاغذی",
                "عضو نمایشی",
                "۱۴۰۵/۰۷/۲۶",
                "در انتظار تایید",
                5_000_000L,
            ),
            WorkflowTask(
                "task-3",
                "DONE",
                "cheque_book_request_process",
                "درخواست دسته چک",
                "کاربر نمایشی",
                "۱۴۰۵/۰۶/۱۰",
                "تایید شده",
                ChequeFixture.FEE,
            ),
        )
}
