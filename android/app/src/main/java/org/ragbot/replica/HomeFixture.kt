package org.ragbot.replica

/** Local presentation configuration, not recovered server IDs or a banking API. HC-FIXTURE. */
data class HomeMenuItem(val key: String, val title: String, val artwork: String)

data class PreviewTransaction(
    val key: String,
    val title: String,
    val amount: Long,
    val unit: String,
    val time: String,
    val source: String,
    val destination: String,
)

object HomeFixture {
    const val accountNumber = "۰۰۰۰۰۰۰۰۰۱"
    const val accountOwner = "کاربر نمایشی"
    const val accountType = "حساب نمایشی"
    const val accountIban = "IR000000000000000000000000"
    const val balanceRial = 12_500_000L
    const val currencyUnit = "IRR"

    val top =
        listOf(
            HomeMenuItem("wallet", "کیف پول", "replica_menu_wallet"),
            HomeMenuItem("loyalty", "باشگاه مشتریان", "replica_menu_loyalty"),
            HomeMenuItem("support", "پشتیبانی", "replica_menu_support"),
            HomeMenuItem("investment", "سرمایه‌گذاری", "replica_menu_investment"),
            HomeMenuItem("performance", "عملکرد مالی", "replica_menu_performance"),
        )
    val quick =
        listOf(
            HomeMenuItem("card-transfer", "کارت‌به‌کارت", "replica_quick_card_transfer"),
            HomeMenuItem("transfer", "انتقال وجه", "replica_quick_transfer"),
            HomeMenuItem("topup", "شارژ", "replica_quick_topup"),
            HomeMenuItem("bill", "قبض", "replica_quick_bill"),
            HomeMenuItem("statement", "صورتحساب", "replica_quick_statement"),
        )
    val services =
        listOf(
            HomeMenuItem("cheque", "چک", "replica_service_cheque"),
            HomeMenuItem("loan", "تسهیلات", "replica_service_loan"),
            HomeMenuItem("merchant", "پایانه فروش", "replica_service_merchant"),
            HomeMenuItem("wage", "مساعده", "replica_service_wage"),
            HomeMenuItem("promissory", "سفته الکترونیک", "replica_service_promissory"),
            HomeMenuItem("vehicle", "خدمات خودرو", "replica_service_vehicle"),
            HomeMenuItem("conversion", "تبدیل بانکی", "replica_service_conversion"),
            HomeMenuItem("inquiry", "استعلامات", "replica_service_inquiry"),
            HomeMenuItem("charity", "خیریه", "replica_service_charity"),
            HomeMenuItem("insurance", "بیمه", "replica_service_insurance"),
            HomeMenuItem("cartable", "کارتابل", "replica_service_cartable"),
            HomeMenuItem("history", "سوابق", "replica_service_history"),
            HomeMenuItem("proxy", "حساب وکالتی", "replica_service_proxy"),
            HomeMenuItem("guarantee", "ضمانت‌نامه", "replica_service_guarantee"),
            HomeMenuItem("open-account", "افتتاح حساب", "replica_service_open_account"),
            HomeMenuItem("branches", "شعب", "replica_service_branches"),
        )
    val transactions =
        listOf(
            PreviewTransaction(
                "preview-1",
                "کارت به کارت",
                1_250_000L,
                "IRR",
                "دیروز - ۱۵:۳۰:۰۰",
                "کارت نمایشی ۰۰۰۱",
                "کارت نمایشی ۰۰۰۲",
            ),
            PreviewTransaction(
                "preview-2",
                "انتقال وجه",
                750_000L,
                "IRR",
                "دیروز - ۱۰:۱۵:۰۰",
                "حساب نمایشی ۰۰۰۱",
                "حساب نمایشی ۰۰۰۲",
            ),
        )
}

fun persianNumber(value: Long): String =
    (if (value < 0) "−" else "") +
        value
            .toString()
            .removePrefix("-")
            .reversed()
            .chunked(3)
            .joinToString("٬")
            .reversed()
            .map { if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it }
            .joinToString("")
