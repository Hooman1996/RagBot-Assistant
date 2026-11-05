package org.ragbot.replica

/** CI-TAB card groups. Group titles and which groups appear are server menus: Designed for mock. */
enum class CardKind(val title: String) {
    DEBIT("کارت‌های بانکی"),
    CREDIT("کارت اعتباری"),
    BON("بن کارت"),
    GIFT("کارت هدیه"),
    WALLET("کیف کارت"),
}

/** CI-FIXTURE: invented local records. Card balance and account balance are separate fields. */
data class PreviewCard(
    val key: String,
    val kind: CardKind,
    val number: String,
    val owner: String,
    val cardBalance: Long?,
    val accountBalance: Long?,
    val primaryAccount: String,
    val otherAccounts: List<String> = emptyList(),
    val iban: String,
    val status: String = "فعال",
    val isDefault: Boolean = false,
    val altLogo: Boolean = false,
    val locked: Boolean = false,
    val unit: String = "IRR",
)

/**
 * Read-only credit summary; every amount is its own recovered field, never derived from another.
 */
data class CreditSummary(
    val state: String,
    val creditLimit: Long,
    val remainingCredit: Long,
    val usedAmount: Long,
    val overdueDebt: Long,
    val totalPayable: Long,
    val unit: String = "IRR",
)

data class FileLoan(
    val fileNumber: String,
    val dueDate: String,
    val instalments: Int,
    val remaining: Long,
    val unit: String = "IRR",
)

/**
 * A tab action row. A null destination is a recovered action that this batch does not implement.
 */
data class CardAction(val key: String, val title: String, val destination: String?)

object CardFixture {
    val cards
        get() = MockData.current.cards.map(MockBank::liveCard)

    /** Candidate cards offered by the fetch-card review page; never added to any profile. */
    val candidates
        get() = cards.take(2) + MockData.current.candidates

    val credit
        get() = MockData.current.credit

    val fileLoans
        get() = MockData.current.fileLoans

    // AI-MENU style: deterministic preview configuration; original server menus and order are
    // unknown.
    val actions =
        mapOf(
            CardKind.DEBIT to
                listOf(
                    CardAction("info", "اطلاعات کارت", CardDestinations.INFO),
                    CardAction("list", "لیست کارت‌ها", CardDestinations.LIST),
                    CardAction("issue", "درخواست کارت", CardRequestDestinations.ISSUE),
                    CardAction("extend", "تمدید کارت", CardRequestDestinations.MODIFY),
                    CardAction("replicate", "المثنی کارت", CardRequestDestinations.MODIFY),
                    CardAction("recolor", "تغییر رنگ کارت", CardRequestDestinations.MODIFY),
                    CardAction("block", "مسدود سازی کارت", CardSecurityDestinations.BLOCK),
                    CardAction("pin", "رمز کارت", CardSecurityDestinations.PIN),
                    CardAction(
                        "activation",
                        "فعال‌سازی رمز پویا",
                        CardSecurityDestinations.ACTIVATION,
                    ),
                ),
            CardKind.CREDIT to
                listOf(
                    CardAction("credit", "جزئیات و بازپرداخت", CardDestinations.CREDIT),
                    CardAction("loan", "خلاصه پرونده تسهیلات", CardDestinations.FILE_LOAN),
                    CardAction("history", "سوابق کارت اعتباری", AccountDestinations.HISTORY),
                    CardAction("block", "مسدود سازی کارت", CardSecurityDestinations.BLOCK),
                ),
            CardKind.BON to
                listOf(
                    CardAction("bon", "موجودی بن کارت", CardDestinations.BON_BALANCE),
                    CardAction("block", "مسدود سازی کارت", CardSecurityDestinations.BLOCK),
                ),
            CardKind.GIFT to listOf(CardAction("info", "اطلاعات کارت", CardDestinations.INFO)),
            CardKind.WALLET to
                listOf(
                    CardAction("wallet-issue", "اولین کیف کارت", WalletDestinations.SEND_TYPE),
                    CardAction("wallet-pin", "دریافت و تغییر رمز", WalletDestinations.PIN),
                ),
        )

    fun of(kind: CardKind) = cards.filter { it.kind == kind }
}

/** Semantic IDs of implemented card destinations. The hosted Home card tab has no ID of its own. */
object CardDestinations {
    const val INFO = "S-012"
    const val BON_BALANCE = "S-119"
    const val ANONYMOUS_BALANCE = "S-120"
    const val LIST = "S-125"
    const val MANAGEMENT = "S-126"
    const val FETCH = "S-068"
    const val VISIBILITY = "S-130"
    const val CREDIT = "S-131"
    const val FILE_LOAN = "S-134"
    val implemented =
        setOf(
            INFO,
            BON_BALANCE,
            ANONYMOUS_BALANCE,
            LIST,
            MANAGEMENT,
            FETCH,
            VISIBILITY,
            CREDIT,
            FILE_LOAN,
        )
}

/**
 * Groups by four as the recovered card face does; masked server characters pass through unchanged.
 */
internal fun cardNumberGroups(number: String): String =
    number.chunked(4).joinToString(" ") { group ->
        group.map { if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it }.joinToString("")
    }

internal fun cardMatches(card: PreviewCard, query: String): Boolean {
    val normalized = latinDigits(query.trim())
    return normalized.isEmpty() ||
        card.number.contains(normalized) ||
        card.owner.contains(query.trim())
}

/**
 * S-119/S-120 recovered checks: required fields, exact CVV2 length, minimum second-password length.
 */
internal fun secureFormErrors(
    number: String?,
    cvv2: String,
    expiry: String,
    password: String,
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (number != null && latinDigits(number).length != 16)
        errors["number"] = "شماره کارت باید ۱۶ رقم باشد."
    if (cvv2.isEmpty()) errors["cvv2"] = "وارد کردن CVV2 الزامی است."
    else if (cvv2.length != 4) errors["cvv2"] = "CVV2 معتبر نیست."
    if (!Regex("(0[1-9]|1[0-2])/[0-9]{2}").matches(latinDigits(expiry)))
        errors["expiry"] = "تاریخ انقضا را به صورت ماه/سال وارد کنید."
    if (password.length < 5) errors["password"] = "رمز دوم باید حداقل ۵ رقم باشد."
    return errors
}
