package org.ragbot.replica

import java.io.Serializable

/** WO-RECOVERY: the six wallet operations, each a form and its confirmation. */
internal enum class WalletOperation(val form: String, val confirm: String, val title: String) {
    W2W("S-116", "S-117", "کیف به کیف"),
    BUY("S-100", "S-101", "خرید با کیف"),
    DECHARGE("S-106", "S-107", "برداشت از کیف"),
    CHARGE("S-104", "S-105", "واریز به کیف"),
    MOBILE("S-102", "S-103", "خرید شارژ"),
    BILL("S-109", "S-099", "پرداخت قبض");

    companion object {
        fun of(page: String) = entries.first { page == it.form || page == it.confirm }
    }
}

internal object WalletOperationDestinations {
    // S-108 (fast-payment QR scanner) is deferred by D-021 and stays unregistered.
    val implemented = WalletOperation.entries.flatMap { listOf(it.form, it.confirm) }.toSet()
}

/**
 * Non-secret form fields. [source] is a wallet number, or an account key for CHARGE; [destination]
 * is a wallet or merchant number, or an account key for DECHARGE. OTP and signature values never
 * enter it.
 */
internal data class WalletOperationDraft(
    val source: String = "",
    val destination: String = "",
    val amount: String = "",
    val mobile: String = "",
    val operator: String = "",
    val billId: String = "",
    val payId: String = "",
    val note: String = "",
    val favorite: Boolean = false,
) : Serializable

/** Designed for mock: what the bank's pre-validation would return for the confirmation. */
internal data class WalletPreview(val destinationName: String, val amount: Long) : Serializable

internal object WalletOperationRules {
    const val SAME = "مبدا و مقصد نمی‌تواند یکسان باشد."

    /** Recovered: a missing amount is mandatory; no client limit exists. */
    fun amountError(value: String): String? =
        if (latinDigits(value).toLongOrNull() == null) PaymentRules.MANDATORY else null

    /** Recovered wallet-to-wallet and purchase destination check, in this order. */
    fun destinationError(value: String, source: String): String? {
        val digits = latinDigits(value)
        return when {
            digits.isEmpty() -> PaymentRules.MANDATORY
            digits == source -> SAME
            digits.length != 16 -> PaymentRules.INVALID
            else -> null
        }
    }

    fun errors(operation: WalletOperation, draft: WalletOperationDraft): Map<String, String> =
        buildMap {
            if (draft.source.isEmpty()) put("source", PaymentRules.MANDATORY)
            when (operation) {
                WalletOperation.W2W ->
                    destinationError(draft.destination, draft.source)?.let {
                        put("destination", it)
                    }
                // A purchase destination comes only from a scanned payment.
                WalletOperation.BUY ->
                    if (draft.destination.isNotEmpty())
                        destinationError(draft.destination, draft.source)?.let {
                            put("destination", it)
                        }
                WalletOperation.DECHARGE,
                WalletOperation.CHARGE ->
                    if (draft.destination.isEmpty()) put("destination", PaymentRules.MANDATORY)
                WalletOperation.MOBILE -> {
                    TopUpRules.phoneError(draft.mobile)?.let { put("mobile", it) }
                    if (TopUpRules.config(draft.operator) == null)
                        put("operator", TopUpRules.CHOOSE_OPERATOR)
                }
                // Recovered: bill and payment IDs both take 6 to 13 digits.
                WalletOperation.BILL -> {
                    PaymentRules.idError(draft.billId)?.let { put("bill", it) }
                    PaymentRules.idError(draft.payId)?.let { put("pay", it) }
                }
            }
            if (operation != WalletOperation.BILL)
                amountError(draft.amount)?.let { put("amount", it) }
        }

    /** Designed for mock pre-validation: fixture lookups and the synthetic balance. */
    fun preview(
        operation: WalletOperation,
        draft: WalletOperationDraft,
    ): Pair<WalletPreview?, String?> {
        val name =
            when (operation) {
                WalletOperation.W2W,
                WalletOperation.BUY ->
                    WalletOperationFixture.destinations[latinDigits(draft.destination)]
                        ?: return null to "مقصدی با این شناسه در دادهٔ نمایشی یافت نشد."
                WalletOperation.DECHARGE ->
                    AccountFixture.accounts.first { it.key == draft.destination }.owner
                WalletOperation.CHARGE ->
                    WalletFixture.wallets.first { it.number == draft.destination }.owner
                WalletOperation.MOBILE -> checkNotNull(TopUpRules.config(draft.operator)).title
                WalletOperation.BILL -> ""
            }
        val (amount, destinationName) =
            if (operation == WalletOperation.BILL) {
                val (bill, error) =
                    PaymentRules.inquiry(BillDraft(billId = draft.billId, payId = draft.payId))
                if (bill == null) return null to error
                bill.amount to bill.company
            } else latinDigits(draft.amount).toLong() to name
        val balance =
            if (operation == WalletOperation.CHARGE)
                AccountFixture.accounts.first { it.key == draft.source }.balance
            else WalletFixture.wallets.first { it.number == draft.source }.balance
        if (balance != null && amount > balance)
            return null to "موجودی ساختگی مبدا برای این مبلغ کافی نیست."
        return WalletPreview(destinationName, amount) to null
    }
}

internal object WalletOperationFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ عملیات کیف پولی انجام نمی‌شود."

    // Synthetic 16-digit destination wallets and merchants with their owner names.
    val destinations =
        mapOf("0000000000000201" to "گیرندهٔ نمایشی", "0000000000000301" to "فروشگاه نمایشی")

    // Server-supplied children of the wallet charge item; titles are synthetic.
    val chargeTypes =
        listOf(
            WalletMenuItem(
                "AUTHENTICATED_WALLET_CHARGE_ACCOUNT",
                "واریز از حساب",
                WalletOperation.CHARGE.form,
            ),
            WalletMenuItem("AUTHENTICATED_WALLET_CHARGE_IPG", "واریز از درگاه پرداخت", null),
        )
}
