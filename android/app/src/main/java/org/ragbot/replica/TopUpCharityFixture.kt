package org.ragbot.replica

import java.io.Serializable

internal object TopUpDestinations {
    const val ACCOUNT = "S-211"
    const val CARD = "S-213"
    const val WITHOUT_SOURCE = "S-214"
    const val CONFIRM = "S-212"
    const val CARD_INFO = "S-215"
    const val CARD_INFO_NO_LOGIN = "S-216"
    const val RECEIPT = "S-094"
    // As for bills, the repeated charge nodes are account, card and typed-card source modes.
    val forms = setOf(ACCOUNT, CARD, WITHOUT_SOURCE)
    val implemented = forms + setOf(CONFIRM, CARD_INFO, CARD_INFO_NO_LOGIN, RECEIPT)
}

internal object CharityDestinations {
    const val PICKER = "S-283"
    const val TRANSFER = "S-284"
    const val CONFIRM = "S-282"
    const val RECEIPT = "S-095"
    val implemented = setOf(PICKER, TRANSFER, CONFIRM, RECEIPT)
}

/** MC-RECOVERY: non-secret top-up form fields. Card credentials never enter this draft. */
internal data class TopUpDraft(
    val source: String = "",
    val phone: String = "",
    val operator: String = "",
    val amount: String = "",
) : Serializable

internal data class CharityDraft(
    val charityId: Long = 0,
    val sourceId: String = "",
    val amount: String = "",
    val paymentId: String = "",
    val description: String = "",
) : Serializable

/** The service supplies one of these per operator; the values here are Designed for mock. */
internal data class OperatorConfig(
    val operator: String,
    val title: String,
    val min: Long,
    val max: Long,
    val multiplier: Long,
    val defaults: List<Long>,
    val prefixes: List<String>,
)

internal data class Charity(val id: Long, val name: String, val desc: String, val accountNo: String)

internal object TopUpRules {
    const val CHOOSE_OPERATOR = "اپراتور تلفن همراه خود را انتخاب کنید."

    fun config(operator: String) = TopUpCharityFixture.operators.find { it.operator == operator }

    /**
     * Recovered: from four digits a matching prefix selects the operator; three or fewer clear it.
     */
    fun detect(phone: String, current: String): String {
        val digits = latinDigits(phone)
        if (digits.length <= 3) return ""
        return TopUpCharityFixture.operators
            .find { config -> config.prefixes.any { digits.startsWith(it) } }
            ?.operator ?: current
    }

    // Recovered phone check: 11 digits starting with 09.
    fun phoneError(phone: String): String? {
        val digits = latinDigits(phone)
        return when {
            digits.isEmpty() -> PaymentRules.MANDATORY
            digits.length != 11 || !digits.startsWith("09") || digits.any { it !in '0'..'9' } ->
                PaymentRules.INVALID
            else -> null
        }
    }

    /** Recovered order: below the minimum, above the maximum, then not a multiple. */
    fun amountError(config: OperatorConfig, value: String): String? {
        val digits = latinDigits(value)
        val amount = digits.toLongOrNull()
        return when {
            digits.isEmpty() -> PaymentRules.MANDATORY
            digits.any { it !in '0'..'9' } -> PaymentRules.INVALID
            amount != null && amount < config.min ->
                "حداقل مبلغ شارژ برای این اپراتور ${persianNumber(config.min)} ریال است."
            amount == null || amount > config.max ->
                "حداکثر مبلغ شارژ برای این اپراتور ${persianNumber(config.max)} ریال است."
            amount % config.multiplier != 0L ->
                "مبلغ وارد شده باید ضریبی از ${persianNumber(config.multiplier)} ریال باشد."
            else -> null
        }
    }

    fun errors(page: String, draft: TopUpDraft): Map<String, String> = buildMap {
        val source = latinDigits(draft.source)
        if (source.isEmpty()) put("source", PaymentRules.MANDATORY)
        // Shape check only; the recovered card check of the typed number is not reproduced.
        else if (
            page == TopUpDestinations.WITHOUT_SOURCE &&
                (source.length != 16 || source.any { it !in '0'..'9' })
        )
            put("source", PaymentRules.INVALID)
        phoneError(draft.phone)?.let { put("phone", it) }
        val config = config(draft.operator)
        if (config == null) put("operator", CHOOSE_OPERATOR)
        if (latinDigits(draft.amount).isEmpty()) put("amount", PaymentRules.MANDATORY)
        else if (config != null) amountError(config, draft.amount)?.let { put("amount", it) }
    }
}

internal object CharityRules {
    // Designed for mock: the original checks the service's internal transfer range.
    const val MIN = 10_000L
    const val MAX = 100_000_000L

    fun amountError(value: String): String? {
        val digits = latinDigits(value)
        val amount = digits.toLongOrNull()
        return when {
            digits.isEmpty() -> PaymentRules.MANDATORY
            digits.any { it !in '0'..'9' } -> PaymentRules.INVALID
            amount == null || amount !in MIN..MAX -> PaymentRules.OUT_OF_RANGE
            else -> null
        }
    }
}

internal object TopUpCharityFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ خرید یا کمکی ارسال نمی‌شود."

    // Operator keys and titles are recovered; limits, amounts and prefixes are synthetic.
    val operators =
        listOf(
            OperatorConfig(
                "HAMRAH_AVAL",
                "همراه اول",
                50_000L,
                5_000_000L,
                50_000L,
                listOf(100_000L, 200_000L, 500_000L, 1_000_000L),
                listOf("0900"),
            ),
            OperatorConfig(
                "IRANCELL",
                "ایرانسل",
                50_000L,
                5_000_000L,
                50_000L,
                listOf(100_000L, 200_000L, 500_000L, 1_000_000L),
                listOf("0901"),
            ),
            OperatorConfig(
                "RIGHTEL",
                "رایتل",
                100_000L,
                2_000_000L,
                100_000L,
                listOf(200_000L, 500_000L, 1_000_000L),
                listOf("0902"),
            ),
        )

    val charities =
        listOf(
            Charity(1, "خیریهٔ نمایشی آموزش", "حمایت از آموزش — نمونه", "0000000000009"),
            Charity(2, "خیریهٔ نمایشی سلامت", "حمایت از درمان — نمونه", "0000000000008"),
        )
}
