package org.ragbot.replica

import java.io.Serializable

object GroupTransferDestinations {
    const val FIRST = "S-187"
    const val MANUAL = "S-184"
    const val FILE_PICK = "S-186"
    const val FILE_LIST = "S-182"
    const val WAITING = "S-190"
    const val VALIDATION = "S-188"
    const val ERRORS = "S-185"
    const val VERIFY = "S-189"
    const val RECEIPT = "S-096"
    const val HISTORY = "S-089"
    const val DESTINATIONS = "S-087"
    const val DESTINATION = "S-088"
    val flow = setOf(MANUAL, FILE_PICK, FILE_LIST, WAITING, VALIDATION, ERRORS, VERIFY)
    // Recovered: Back on these pages asks whether to cancel the whole process.
    val guarded = setOf(WAITING, VALIDATION, ERRORS)
    val implemented = flow + setOf(FIRST, RECEIPT, HISTORY, DESTINATIONS, DESTINATION)
}

/** One destination row. The IBAN is stored without IR; the amount stays text until checked. */
data class GroupRow(
    val id: Long,
    val iban: String = "",
    val amount: String = "",
    val reason: String = "",
    val paymentId: String = "",
    val description: String = "",
) : Serializable

/** GT-RECOVERY: non-secret local preview state. No request, authorization or transfer exists. */
data class GroupTransferDraft(
    val sourceId: String = "account-1",
    val total: String = "",
    val byFile: Boolean = false,
    val withdrawId: String = "",
    val sourceDescription: String = "",
    val rows: List<GroupRow> = listOf(GroupRow(1)),
    val fileKey: String? = null,
    val rejected: List<Long> = emptyList(),
    val second: Boolean = false,
) : Serializable

/** A synthetic sheet: cell text by row, row 0 being the header. Never a device file. */
class GroupFile(
    val key: String,
    val name: String,
    val sheet: List<List<String?>>,
    val rejected: List<Long> = emptyList(),
)

class GroupHistory(
    val traceId: String,
    val date: String,
    val status: String,
    val source: String,
    val sourceName: String,
    val withdrawId: String,
    val sourceDescription: String,
    val note: String,
    val rows: List<GroupRow>,
)

internal object GroupTransferRules {
    const val MANUAL_MAX = 30
    const val FILE_MAX = 500
    const val SINGLE_WARNING =
        "برای ادامه در فرآیند انتقال وجه گروهی باید بیش از یک مقصد وجود داشته باشد."

    /** Recovered column order; header and all-blank rows are skipped, IDs keep the sheet row. */
    fun parse(sheet: List<List<String?>>): List<GroupRow> =
        sheet.drop(1).mapIndexedNotNull { index, cells ->
            fun cell(column: Int) = cells.getOrNull(column).orEmpty()
            if ((0..5).all { cell(it).isBlank() }) null
            else GroupRow(index + 1L, cell(1).replace("IR", ""), cell(2), cell(3), cell(4), cell(5))
        }

    fun fileError(rows: List<GroupRow>): String? =
        if (rows.size !in 1..FILE_MAX) "فایل باید بین ۱ تا ۵۰۰ رکورد داشته باشد." else null

    fun rowError(row: GroupRow): String? {
        val iban = latinDigits(row.iban).trim()
        val paymentId = latinDigits(row.paymentId)
        return when {
            iban.isEmpty() -> "شماره شبا مقصد را وارد کنید."
            !AccountTransferFixture.validIban(iban) -> "شماره شبا مقصد معتبر نیست."
            TransferRules.amountRial(row.amount) == null -> "مبلغ صحیح و مثبت به ریال وارد کنید."
            // Designed for mock: every row needs a synthetic reason (see requirements report).
            row.reason !in AccountTransferFixture.reasons -> "بابت را انتخاب کنید."
            paymentId.length > 30 || paymentId.any { it !in '0'..'9' } ->
                "شناسه واریز حداکثر ۳۰ رقم است."
            else -> null
        }
    }

    /** Exact IRR sum; null when the preview would overflow a Long. */
    fun total(rows: List<GroupRow>): Long? =
        try {
            rows.fold(0L) { sum, row ->
                Math.addExact(sum, TransferRules.amountRial(row.amount) ?: 0L)
            }
        } catch (_: ArithmeticException) {
            null
        }

    fun acceptError(rows: List<GroupRow>): String? =
        when {
            rows.any { rowError(it) != null } -> "ردیف‌های دارای خطا را اصلاح یا حذف کنید."
            rows.size < 2 -> SINGLE_WARNING
            total(rows) == null -> "جمع مبالغ از حد این پیش‌نمایش بیشتر است."
            else -> null
        }

    fun firstPageError(draft: GroupTransferDraft): String? =
        when {
            AccountFixture.accounts.none { it.key == draft.sourceId } -> "حساب مبدأ را انتخاب کنید."
            TransferRules.amountRial(draft.total) == null ->
                "مبلغ کل صحیح و مثبت به ریال وارد کنید."
            else -> null
        }
}

internal object GroupTransferFixture {
    private val header = listOf("ردیف", "شماره شبا", "مبلغ", "بابت", "شناسه واریز", "شرح مقصد")
    private val first = AccountTransferFixture.reasons.first()

    private fun row(n: Int, iban: String, amount: String, reason: String = first) =
        listOf("$n", iban, amount, reason, "", "مقصد نمایشی $n")

    val files =
        listOf(
            GroupFile(
                "valid",
                "نمونه-معتبر-ساختگی.xlsx",
                listOf(
                    header,
                    row(1, "IR920000000000000000000002", "150000"),
                    row(2, "IR650000000000000000000003", "250000"),
                    row(3, "IR380000000000000000000004", "100000"),
                ),
            ),
            GroupFile(
                "invalid",
                "نمونه-با-خطا-ساختگی.xlsx",
                listOf(
                    header,
                    row(1, "IR920000000000000000000002", "150000"),
                    row(2, "IR930000000000000000000002", "250000"),
                    listOf(null, null, null, null, null, null),
                    row(4, "IR650000000000000000000003", "0"),
                    row(5, "IR380000000000000000000004", "100000", ""),
                    row(6, "IR110000000000000000000005", "300000"),
                ),
            ),
            GroupFile(
                "rejected",
                "نمونه-رد-ثبت-ساختگی.xlsx",
                listOf(
                    header,
                    row(1, "IR920000000000000000000002", "150000"),
                    row(2, "IR650000000000000000000003", "250000"),
                    row(3, "IR380000000000000000000004", "100000"),
                ),
                rejected = listOf(2L),
            ),
            GroupFile("empty", "نمونه-خالی-ساختگی.xlsx", listOf(header)),
            GroupFile(
                "large",
                "نمونه-۵۰۱-رکورد-ساختگی.xlsx",
                listOf(header) + (1..501).map { row(it, "IR920000000000000000000002", "1000") },
            ),
        )

    val history =
        listOf(
            GroupHistory(
                "DEMO-GROUP-0001",
                "۱۴۰۵/۰۸/۰۹ - ۱۰:۲۰",
                "نمونهٔ نمایشی — بدون تراکنش بانکی",
                "0000000000001",
                "حساب نمایشی ۰۰۰۱",
                "",
                "پرداخت نمایشی گروهی",
                "یادداشت ساختگی",
                GroupTransferRules.parse(files.first().sheet),
            ),
            GroupHistory(
                "DEMO-GROUP-0002",
                "۱۴۰۵/۰۸/۰۲ - ۰۹:۰۵",
                "نمونهٔ نمایشی — بدون تراکنش بانکی",
                "0000000000001",
                "حساب نمایشی ۰۰۰۱",
                "12345",
                "",
                "",
                GroupTransferRules.parse(files.first().sheet).take(2),
            ),
        )
}
