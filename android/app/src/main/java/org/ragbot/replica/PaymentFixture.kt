package org.ragbot.replica

import java.io.Serializable

internal object BillPaymentDestinations {
    const val ACCOUNT = "S-209"
    const val CARD = "S-210"
    const val WITHOUT_SOURCE = "S-208"
    const val CONFIRM = "S-207"
    const val CARD_INFO = "S-205"
    const val CARD_INFO_NO_LOGIN = "S-206"
    const val RECEIPT = "S-092"
    // The repeated bill-transfer nodes are distinct source modes: account, card, typed card.
    val forms = setOf(ACCOUNT, CARD, WITHOUT_SOURCE)
    val implemented = forms + setOf(CONFIRM, CARD_INFO, CARD_INFO_NO_LOGIN, RECEIPT)
}

internal object InsuranceDestinations {
    const val MENU = "S-403"
    const val LIST = "S-402"
    const val INSTALLMENTS = "S-401"
    const val CONFIRM = "S-404"
    const val RECEIPT = "S-405"
    val implemented = setOf(MENU, LIST, INSTALLMENTS, CONFIRM, RECEIPT)
}

internal object CreditRepaymentDestinations {
    const val CONFIRM = "S-132"
    const val RECEIPT = "S-133"
    val implemented = setOf(CONFIRM, RECEIPT)
}

/** BP-RECOVERY: non-secret bill form fields. Card credentials never enter this saved draft. */
internal data class BillDraft(
    val source: String = "",
    val billId: String = "",
    val payId: String = "",
    val note: String = "",
) : Serializable

internal data class BillInquiry(
    val billId: String,
    val payId: String,
    val company: String,
    val amount: Long,
    val paid: Boolean,
)

internal data class Installment(
    val number: Int,
    val year: Int,
    val dueDate: String,
    val amount: Long,
    val remained: Long,
    val paymentId: String,
    val status: String,
) : Serializable

internal data class InsurancePolicy(
    val id: String,
    val number: String,
    val begin: String,
    val end: String,
    val state: String,
    val addendum: Int,
    val installments: List<Installment>,
) : Serializable

internal data class CreditRepayment(val amount: Long, val sourceId: String) : Serializable

internal object PaymentRules {
    const val MANDATORY = "تکمیل این فیلد اجباری است."
    const val INVALID = "مقدار وارد شده را بررسی فرمایید."
    const val SELECT_INSTALLMENT = "یکی از اقساط را جهت پرداخت انتخاب کنید."
    const val OUT_OF_RANGE = "مبلغ ورودی در محدوده نمی باشد."
    const val OVER_DEBT = "مبلغ نمی تواند بیشتر از مبلغ بدهی اعتباری باشد."
    const val PAID = "PAID"
    const val VERIFICATION_WAITING = "VERIFICATION_WAITING"
    const val POSTPONED = "POSTPONED"
    const val DUE_DATE = "DUE_DATE"

    /** Recovered bill and payment ID check: required, digits, at least 6 (input keeps 13). */
    fun idError(value: String): String? {
        val digits = latinDigits(value)
        return when {
            digits.isEmpty() -> MANDATORY
            digits.length < 6 || digits.any { it !in '0'..'9' } -> INVALID
            else -> null
        }
    }

    fun billErrors(page: String, draft: BillDraft): Map<String, String> = buildMap {
        val source = latinDigits(draft.source)
        if (source.isEmpty()) put("source", MANDATORY)
        // Shape check only; the recovered card check of the typed number is not reproduced.
        else if (
            page == BillPaymentDestinations.WITHOUT_SOURCE &&
                (source.length != 16 || source.any { it !in '0'..'9' })
        )
            put("source", INVALID)
        idError(draft.billId)?.let { put("bill", it) }
        idError(draft.payId)?.let { put("pay", it) }
    }

    /** Designed for mock: the bank's inquiry returns the amount; here a fixture lookup does. */
    fun inquiry(draft: BillDraft): Pair<BillInquiry?, String?> {
        val bill =
            PaymentFixture.bills.find {
                it.billId == latinDigits(draft.billId) && it.payId == latinDigits(draft.payId)
            }
        return when {
            bill == null -> null to "قبضی با این شناسه‌ها در دادهٔ نمایشی یافت نشد."
            bill.paid -> null to "این قبض در دادهٔ نمایشی پرداخت شده است."
            else -> bill to null
        }
    }

    // Recovered: paid and verification-waiting installments cannot be chosen.
    fun selectable(installment: Installment) =
        installment.status != PAID && installment.status != VERIFICATION_WAITING

    // Recovered: the Pay button appears only while a due or postponed installment exists.
    fun payable(policy: InsurancePolicy) =
        policy.installments.any { it.status == DUE_DATE || it.status == POSTPONED }

    /** Recovered repayment amount checks; any amount up to the total payable is allowed. */
    fun repaymentError(value: String, totalPayable: Long): String? {
        val digits = latinDigits(value)
        val amount = digits.toLongOrNull()
        return when {
            digits.isEmpty() -> MANDATORY
            digits.any { it !in '0'..'9' } -> INVALID
            amount == 0L -> OUT_OF_RANGE
            amount == null || amount > totalPayable -> OVER_DEBT
            else -> null
        }
    }
}

internal object PaymentFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ پرداخت یا استعلامی ارسال نمی‌شود."
    const val insuredName = "بیمه‌شدهٔ نمایشی"
    const val insuredNationalCode = "0000000000"

    val bills =
        listOf(
            BillInquiry("9000000000001", "1250001", "شرکت آب نمایشی", 1_250_000L, false),
            BillInquiry("9000000000002", "430002", "شرکت برق نمایشی", 430_000L, true),
        )

    // Status titles come from the service; these are Designed for mock.
    val statusTitles =
        mapOf(
            PaymentRules.PAID to "پرداخت شده",
            PaymentRules.VERIFICATION_WAITING to "در انتظار تایید",
            PaymentRules.POSTPONED to "معوق",
            PaymentRules.DUE_DATE to "سررسید",
        )

    val policies =
        listOf(
            InsurancePolicy(
                "policy-1",
                "DEMO-POLICY-0001",
                "۱۴۰۵/۰۱/۰۱",
                "۱۴۰۵/۱۲/۲۹",
                "فعال",
                0,
                listOf(
                    Installment(
                        1,
                        1405,
                        "۱۴۰۵/۰۲/۰۱",
                        2_000_000L,
                        0L,
                        "1000001",
                        PaymentRules.PAID,
                    ),
                    Installment(
                        2,
                        1405,
                        "۱۴۰۵/۰۳/۰۱",
                        2_000_000L,
                        2_000_000L,
                        "1000002",
                        PaymentRules.VERIFICATION_WAITING,
                    ),
                    Installment(
                        3,
                        1405,
                        "۱۴۰۵/۰۴/۰۱",
                        2_000_000L,
                        1_500_000L,
                        "1000003",
                        PaymentRules.POSTPONED,
                    ),
                    Installment(
                        4,
                        1405,
                        "۱۴۰۵/۰۵/۰۱",
                        2_000_000L,
                        2_000_000L,
                        "1000004",
                        PaymentRules.DUE_DATE,
                    ),
                ),
            ),
            InsurancePolicy(
                "policy-2",
                "DEMO-POLICY-0002",
                "۱۴۰۴/۰۱/۰۱",
                "۱۴۰۴/۱۲/۲۹",
                "خاتمه یافته",
                1,
                listOf(
                    Installment(1, 1404, "۱۴۰۴/۰۲/۰۱", 900_000L, 0L, "2000001", PaymentRules.PAID)
                ),
            ),
        )
}
