package org.ragbot.replica

/** CI-TAB card groups. Group titles and which groups appear are server menus: Designed for mock. */
enum class CardKind(val title: String) {
    DEBIT("کارت‌های بانکی"),
    CREDIT("کارت اعتباری"),
    BON("بن کارت"),
    GIFT("کارت هدیه"),
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
    val cards =
        listOf(
            PreviewCard(
                "card-1",
                CardKind.DEBIT,
                "0000000000000001",
                HomeFixture.accountOwner,
                12_500_000L,
                12_500_000L,
                HomeFixture.accountNumber,
                listOf("۰۰۰۰۰۰۰۰۱۱"),
                HomeFixture.accountIban,
                isDefault = true,
            ),
            PreviewCard(
                "card-2",
                CardKind.DEBIT,
                "0000000000000002",
                "کاربر نمایشی",
                0L,
                3_750_000L,
                "۰۰۰۰۰۰۰۰۰۲",
                iban = "IR000000000000000000000002",
                altLogo = true,
            ),
            PreviewCard(
                "card-3",
                CardKind.DEBIT,
                "000000******0003",
                "کاربر نمایشی",
                null,
                null,
                "۰۰۰۰۰۰۰۰۰۳",
                iban = "IR000000000000000000000003",
                status = "غیرفعال",
                locked = true,
            ),
            PreviewCard(
                "card-4",
                CardKind.CREDIT,
                "0000000000000004",
                HomeFixture.accountOwner,
                null,
                null,
                "۰۰۰۰۰۰۰۰۰۴",
                iban = "IR000000000000000000000004",
            ),
            PreviewCard(
                "card-5",
                CardKind.CREDIT,
                "0000000000000005",
                HomeFixture.accountOwner,
                null,
                null,
                "۰۰۰۰۰۰۰۰۰۵",
                iban = "IR000000000000000000000005",
            ),
            PreviewCard(
                "card-6",
                CardKind.BON,
                "0000000000000006",
                HomeFixture.accountOwner,
                null,
                null,
                "۰۰۰۰۰۰۰۰۰۶",
                iban = "IR000000000000000000000006",
            ),
        )

    /** Candidate cards offered by the fetch-card review page; never added to any profile. */
    val candidates =
        listOf(
            cards[0],
            cards[1],
            PreviewCard(
                "card-7",
                CardKind.DEBIT,
                "0000000000000007",
                "کاربر نمایشی",
                null,
                null,
                "۰۰۰۰۰۰۰۰۰۷",
                iban = "IR000000000000000000000007",
            ),
        )

    val credit =
        mapOf(
            "card-4" to
                CreditSummary("فعال", 500_000_000L, 320_000_000L, 180_000_000L, 0L, 180_000_000L),
            "card-5" to CreditSummary("فعال", 200_000_000L, 200_000_000L, 0L, 0L, 0L),
        )

    val fileLoans =
        mapOf(
            "card-4" to
                listOf(
                    FileLoan("۰۰۰۱", "۱۴۰۵/۰۹/۰۱", 12, 150_000_000L),
                    FileLoan("۰۰۰۲", "۱۴۰۵/۱۰/۰۱", 6, 30_000_000L),
                )
        )

    // AI-MENU style: deterministic preview configuration; original server menus and order are
    // unknown.
    val actions =
        mapOf(
            CardKind.DEBIT to
                listOf(
                    CardAction("info", "اطلاعات کارت", CardDestinations.INFO),
                    CardAction("list", "لیست کارت‌ها", CardDestinations.LIST),
                    CardAction("issue", "درخواست کارت", null),
                    CardAction("block", "مسدودسازی کارت", null),
                    CardAction("pin", "رمز کارت", null),
                ),
            CardKind.CREDIT to
                listOf(
                    CardAction("credit", "جزئیات و بازپرداخت", CardDestinations.CREDIT),
                    CardAction("loan", "خلاصه پرونده تسهیلات", CardDestinations.FILE_LOAN),
                    CardAction("history", "سوابق کارت اعتباری", AccountDestinations.HISTORY),
                    CardAction("block", "مسدودسازی کارت", null),
                ),
            CardKind.BON to
                listOf(
                    CardAction("bon", "موجودی بن کارت", CardDestinations.BON_BALANCE),
                    CardAction("block", "مسدودسازی کارت", null),
                ),
            CardKind.GIFT to listOf(CardAction("info", "اطلاعات کارت", CardDestinations.INFO)),
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
