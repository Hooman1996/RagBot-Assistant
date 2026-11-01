package org.ragbot.replica

internal object WalletOverviewDestinations {
    const val HISTORY_MENU = "S-110"
    const val MAIN = "S-111"
    const val QR = "S-112"
    const val RECEIPT = "S-113"
    const val STATEMENT_DETAIL = "S-114"
    const val STATEMENT = "S-115"
    val implemented = setOf(HISTORY_MENU, MAIN, QR, RECEIPT, STATEMENT_DETAIL, STATEMENT)
}

/** WL-RECOVERY: the wallet list item fields. Balances are exact Long IRR. */
internal data class PreviewWallet(
    val number: String,
    val title: String,
    val owner: String,
    val holderType: String,
    val status: String,
    val statusDescription: String,
    val individual: Boolean,
    val balance: Long,
)

/** Recovered transaction types of a wallet statement row. */
internal enum class WalletTranType {
    CHARGE,
    DE_CHARGE,
    TRANSFER,
    BUY,
    WALLET_MOBILE_CREDIT,
    WALLET_BILL_PAYMENT,
    IPG,
}

internal data class WalletStatementRow(
    val key: String,
    val amount: Long,
    val credit: Boolean,
    val balance: Long,
    val type: WalletTranType,
    val typeDescription: String,
    val date: String,
    val time: String,
    val traceId: String,
    val transactionId: String,
    val participant: String,
    val participantName: String,
    val description: String,
)

/** A server-supplied wallet menu item; [destination] is null where no page is built. */
internal data class WalletMenuItem(val key: String, val title: String, val destination: String?)

internal object WalletRules {
    const val PAGE_SIZE = 10

    // Recovered: more than five wallets show only the first four.
    fun shown(wallets: List<PreviewWallet>) = if (wallets.size > 5) wallets.take(4) else wallets

    // Recovered: the QR and statement wallet pickers list active wallets only.
    fun active(wallets: List<PreviewWallet>) = wallets.filter { it.status == "ACTIVE" }

    /** Recovered QR payload. The replica only shows it as text; it starts no payment. */
    fun qrContent(walletNumber: String, amount: String?): String =
        """
        {
          "amount": ${amount?.let { "\"$it\"" }},
          "destination": "$walletNumber"
        }
        """
            .trimIndent()

    /**
     * Recovered detail rows; bill and gateway rows hide the participant, mobile credit the note.
     */
    fun detailFields(row: WalletStatementRow): List<Pair<String, String>> = buildList {
        if (row.type != WalletTranType.WALLET_BILL_PAYMENT && row.type != WalletTranType.IPG)
            add((if (row.credit) "مبدا:" else "مقصد:") to row.participant)
        if (row.type != WalletTranType.IPG) add("نام" to row.participantName)
        add("کد رهگیری:" to row.transactionId)
        if (row.type != WalletTranType.WALLET_MOBILE_CREDIT) add("یادداشت:" to row.description)
    }
}

internal object WalletFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ عملیات کیف پولی انجام نمی‌شود."

    val wallets =
        listOf(
            PreviewWallet(
                "0000000000101",
                "کیف نمایشی شخصی",
                "کاربر نمایشی",
                "حقیقی",
                "ACTIVE",
                "فعال",
                true,
                3_250_000L,
            ),
            PreviewWallet(
                "0000000000102",
                "کیف نمایشی تجاری",
                "کاربر نمایشی",
                "حقوقی",
                "ACTIVE",
                "فعال",
                false,
                18_000_000L,
            ),
            PreviewWallet(
                "0000000000103",
                "کیف نمایشی غیرفعال",
                "کاربر نمایشی",
                "حقیقی",
                "INACTIVE",
                "غیرفعال",
                true,
                0L,
            ),
        )

    // Menu keys and their targets are recovered; titles are server-supplied, so synthetic here.
    // The charge action opens a payment-type sheet instead of a page.
    val actions =
        listOf(
            WalletMenuItem("AUTHENTICATED_WALLET_CHARGE", "شارژ کیف", null),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_DECHARGE",
                "برداشت از کیف",
                WalletOperation.DECHARGE.form,
            ),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_W2W",
                "انتقال کیف به کیف",
                WalletOperation.W2W.form,
            ),
        )
    val menu =
        listOf(
            WalletMenuItem(
                "AUTHENTICATED_WALLET_STATEMENT",
                "صورتحساب کیف",
                WalletOverviewDestinations.STATEMENT,
            ),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_TRANSACTION_HISTORY",
                "سوابق تراکنش‌ها",
                WalletOverviewDestinations.HISTORY_MENU,
            ),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_RECEIVE_BARCODE",
                "دریافت با بارکد",
                WalletOverviewDestinations.QR,
            ),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_BUY_WITH_WALLET",
                "خرید با کیف",
                WalletOperation.BUY.form,
            ),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_PAY_BILL",
                "پرداخت قبض با کیف",
                WalletOperation.BILL.form,
            ),
            WalletMenuItem(
                "AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE",
                "خرید شارژ با کیف",
                WalletOperation.MOBILE.form,
            ),
        )
    // Recovered: every history item opens the transaction list (S-091).
    val history =
        listOf(
                "AUTHENTICATED_WALLET_CHARGE_TRANSACTION_HISTORY" to "سوابق شارژ کیف",
                "AUTHENTICATED_WALLET_DE_CHARGE_TRANSACTION_HISTORY" to "سوابق برداشت از کیف",
                "AUTHENTICATED_WALLET_W2W_TRANSACTION_HISTORY" to "سوابق انتقال کیف به کیف",
                "AUTHENTICATED_WALLET_BUY_TRANSACTION_HISTORY" to "سوابق خرید با کیف",
                "AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE_HISTORY" to "سوابق خرید شارژ",
                "AUTHENTICATED_WALLET_PAY_BILL_HISTORY" to "سوابق پرداخت قبض",
            )
            .map { (key, title) -> WalletMenuItem(key, title, AccountDestinations.HISTORY) }

    private val types =
        listOf(
            WalletTranType.CHARGE to "شارژ کیف",
            WalletTranType.TRANSFER to "انتقال کیف به کیف",
            WalletTranType.WALLET_BILL_PAYMENT to "پرداخت قبض",
            WalletTranType.WALLET_MOBILE_CREDIT to "خرید شارژ",
            WalletTranType.IPG to "پرداخت اینترنتی",
            WalletTranType.DE_CHARGE to "برداشت از کیف",
        )

    /** Twelve rows a day apart so the first page of ten has a second page. */
    fun statement(wallet: PreviewWallet): List<WalletStatementRow> =
        (0 until 12).map { i ->
            val (type, description) = types[i % types.size]
            val credit = type == WalletTranType.CHARGE
            WalletStatementRow(
                "wallet-row-$i",
                100_000L * (i + 1),
                credit,
                wallet.balance - 50_000L * i,
                type,
                description,
                dateDaysAgo(i),
                "۱۰:۱۵",
                "DEMO-TRACE-${100 + i}",
                "DEMO-TX-${100 + i}",
                if (credit) "0000000000001" else "0000000000201",
                if (credit) "حساب نمایشی" else "طرف نمایشی",
                "یادداشت نمایشی ${i + 1}",
            )
        }
}
