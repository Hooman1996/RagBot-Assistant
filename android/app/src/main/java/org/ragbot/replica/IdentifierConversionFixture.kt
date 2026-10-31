package org.ragbot.replica

import java.io.Serializable

internal object ConversionDestinations {
    const val LOGIN = "S-201"
    const val NO_LOGIN = "S-202"
    const val PAID = "S-203"
    const val PAID_TYPED = "S-204"
    const val RESULT = "S-011"
    val forms = setOf(LOGIN, NO_LOGIN)
    val paid = setOf(PAID, PAID_TYPED)
    val implemented = forms + paid + RESULT
}

internal enum class ConversionMode(val title: String, val inputLabel: String) {
    ACCOUNT_TO_IBAN("حساب به شبا", "شماره حساب"),
    CARD_TO_IBAN("کارت به شبا", "شماره کارت"),
    IBAN_TO_ACCOUNT("شبا به حساب", "شماره شبا بدون IR"),
    IBAN_TO_CARD("شبا به کارت", "شماره شبا بدون IR"),
    CARD_TO_ACCOUNT("کارت به حساب", "شماره کارت");

    val isCardInput: Boolean
        get() = this == CARD_TO_IBAN || this == CARD_TO_ACCOUNT

    val isIbanInput: Boolean
        get() = this == IBAN_TO_ACCOUNT || this == IBAN_TO_CARD
}

/** Only non-secret local preview fields are saved. */
internal data class ConversionDraft(
    val mode: ConversionMode = ConversionMode.ACCOUNT_TO_IBAN,
    val identifier: String = "",
    val bankKey: String = "",
    val addFavorite: Boolean = false,
) : Serializable

internal data class ConversionBank(val key: String, val title: String, val isInternal: Boolean)

internal data class ConversionResult(
    val owner: String,
    val bank: String,
    val account: String,
    val iban: String,
    val cards: List<String>,
)

/** IDENTIFIER-RECOVERY: presentation rules only; bank namespaces and records are synthetic. */
internal object ConversionFixture {
    const val account = "0000000000001"
    const val card = "0000000000000001"
    const val iban = "IR220000000000000000000001"
    val cards = listOf(card, "0000000000000002")
    val banks =
        listOf(
            ConversionBank("internal", "بانک داخلی ساختگی", true),
            ConversionBank("external", "بانک دیگر ساختگی", false),
        )

    fun normalizeIdentifier(mode: ConversionMode, value: String): String {
        val normalized = latinDigits(value).trim()
        return when {
            mode.isIbanInput -> normalized.filter { it in '0'..'9' }
            mode.isCardInput -> normalized.replace(" ", "")
            else -> normalized
        }
    }

    fun error(draft: ConversionDraft): String? {
        val identifier = normalizeIdentifier(draft.mode, draft.identifier)
        if (identifier.isEmpty()) return "تکمیل این فیلد اجباری است."
        return when {
            draft.mode.isCardInput ->
                if (
                    identifier.length != 16 ||
                        identifier.any { it !in '0'..'9' } ||
                        cardBank(identifier) == null
                )
                    "مقدار وارد شده را بررسی فرمایید."
                else null
            draft.mode.isIbanInput -> {
                if (!AccountTransferFixture.validIban(identifier)) {
                    "شماره شبا معتبر نیست."
                } else
                    when (identifier.substring(2, 5)) {
                        "000" -> null
                        "001" -> "این تبدیل فقط برای بانک داخلی ساختگی در دسترس است."
                        else -> "بانک این شماره شبا در فهرست ساختگی نیست."
                    }
            }
            identifier.length > 30 -> "شماره حساب حداکثر ۳۰ نویسه دارد."
            banks.none { it.key == draft.bankKey } -> "بانک را انتخاب کنید."
            else -> null
        }
    }

    fun paidQuery(draft: ConversionDraft): Boolean {
        if (error(draft) != null) return false
        return if (draft.mode.isCardInput) {
            cardBank(normalizeIdentifier(draft.mode, draft.identifier)) == banks[1]
        } else draft.mode == ConversionMode.ACCOUNT_TO_IBAN && draft.bankKey == banks[1].key
    }

    /** Exact fixture lookup supplies ownership; arithmetic never invents a conversion. */
    fun result(draft: ConversionDraft): ConversionResult? {
        if (error(draft) != null || paidQuery(draft)) return null
        val identifier = normalizeIdentifier(draft.mode, draft.identifier)
        val known =
            when {
                draft.mode.isCardInput -> identifier in cards
                draft.mode.isIbanInput -> identifier == iban.removePrefix("IR")
                else -> identifier == account && draft.bankKey == banks[0].key
            }
        return if (known) {
            ConversionResult("دارندهٔ ساختگی", banks[0].title, account, iban, cards)
        } else null
    }

    private fun cardBank(identifier: String): ConversionBank? =
        when {
            identifier.startsWith("000000") -> banks[0]
            identifier.startsWith("000001") -> banks[1]
            else -> null
        }
}
