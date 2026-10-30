package org.ragbot.replica

/** IT-ACCOUNT: non-secret UI state only. No request, authorization or transaction is created. */
data class AccountTransferDraft(
    val mode: String = AccountTransferDestinations.INTERNAL,
    val sourceId: String = "account-1",
    val destination: String = "",
    val amount: String = "",
    val reason: String = "",
    val paymentId: String = "",
    val withdrawId: String = "",
    val sourceDescription: String = "",
    val destinationDescription: String = "",
    val note: String = "",
    val addFavorite: Boolean = false,
) : java.io.Serializable

object AccountTransferDestinations {
    const val MENU = "S-193"
    const val INTERNAL = "S-192"
    const val INTERBANK = "S-191"
    const val CONFIRM = "S-173"
    const val INTERNAL_RECEIPT = "S-098"
    const val IBAN_RECEIPT = "S-097"
    val implemented = setOf(MENU, INTERNAL, INTERBANK, CONFIRM, INTERNAL_RECEIPT, IBAN_RECEIPT)
}

internal object AccountTransferFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ وجهی منتقل نمی‌شود."
    val reasons = listOf("بابت نمونهٔ اول — ساختگی", "بابت نمونهٔ دوم — ساختگی")

    fun validate(draft: AccountTransferDraft): String? {
        val destination = latinDigits(draft.destination).trim().uppercase()
        val source = AccountFixture.accounts.find { it.key == draft.sourceId }
        if (source == null) return "حساب مبدأ را انتخاب کنید."
        if (draft.mode == AccountTransferDestinations.INTERNAL) {
            if (destination.length != 13 || destination.any { it !in '0'..'9' }) {
                return "شماره حساب مقصد باید ۱۳ رقم باشد."
            }
            if (destination == latinDigits(source.number)) return "مبدأ و مقصد یکسان است."
        } else {
            val digits = ibanDigits(destination)
            if (
                digits.length != 24 ||
                    digits.any { it !in '0'..'9' } ||
                    digits.all { it == digits[0] } ||
                    (digits.drop(2) + "1827" + digits.take(2)).fold(0) { remainder, ch ->
                        (remainder * 10 + ch.digitToInt()) % 97
                    } != 1
            ) {
                return "شماره شبا معتبر نیست."
            }
            if (digits == ibanDigits(source.iban)) {
                return "مبدأ و مقصد یکسان است."
            }
        }
        if (TransferRules.amountRial(draft.amount) == null)
            return "مبلغ صحیح و مثبت به ریال وارد کنید."
        if (draft.reason !in reasons) return "بابت را انتخاب کنید."
        return null
    }

    private fun ibanDigits(value: String): String =
        latinDigits(value).trim().uppercase().removePrefix("IR").trim()
}
