package org.ragbot.replica

/** AI-FIXTURE: invented local records, never original customer or server data. */
data class PreviewAccount(
    val key: String,
    val number: String,
    val owner: String,
    val type: String,
    val iban: String,
    val balance: Long?,
    val unit: String = "IRR",
    val ownership: String = "انفرادی",
)

object AccountFixture {
    val accounts =
        listOf(
            PreviewAccount(
                "account-1",
                HomeFixture.accountNumber,
                HomeFixture.accountOwner,
                HomeFixture.accountType,
                HomeFixture.accountIban,
                HomeFixture.balanceRial,
            ),
            PreviewAccount(
                "account-2",
                "۰۰۰۰۰۰۰۰۰۲",
                "کاربر نمایشی",
                "پس‌انداز نمایشی",
                "IR000000000000000000000002",
                3_750_000L,
            ),
            PreviewAccount(
                "account-3",
                "۰۰۰۰۰۰۰۰۰۳",
                "کاربر نمایشی",
                "حساب بدون موجودی دریافتی",
                "IR000000000000000000000003",
                null,
                ownership = "مشترک",
            ),
        )
}

/** Semantic IDs identify implemented screens, not every embedded tab or dialog. */
object AccountDestinations {
    const val LIST = "S-085"
    const val MANAGEMENT = "S-063"
    const val FETCH = "S-067"
    const val STATEMENT = "S-086"
    const val STATEMENT_LIST = "S-016"
    const val STATEMENT_DETAIL = "S-014"
    const val HISTORY = "S-091"
    const val TRANSACTION = "S-090"
    val implemented =
        setOf(
            LIST,
            MANAGEMENT,
            FETCH,
            STATEMENT,
            STATEMENT_LIST,
            STATEMENT_DETAIL,
            HISTORY,
            TRANSACTION,
        )
}

/** Only local text normalization; it does not coerce money through floating point. */
internal fun latinDigits(value: String): String =
    value
        .map {
            when (it) {
                in '۰'..'۹' -> '0' + (it - '۰')
                in '٠'..'٩' -> '0' + (it - '٠')
                else -> it
            }
        }
        .joinToString("")

internal fun accountMatches(account: PreviewAccount, query: String): Boolean {
    val normalized = latinDigits(query.trim())
    return normalized.isEmpty() ||
        latinDigits(account.number).contains(normalized) ||
        account.owner.contains(query.trim())
}
