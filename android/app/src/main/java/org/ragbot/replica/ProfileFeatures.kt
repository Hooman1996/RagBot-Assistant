package org.ragbot.replica

import org.ragbot.replica.bridge.Reviews

/*
 * One feature per built journey: what it prepares, where it ends and what it looks up.
 * Read by the profile export. Python's task runner routes through a feature's form to its
 * destination, so a page shared by several services is still reached through the right one.
 * A feature's destination is never a receipt: every journey stops before the user's own step.
 * Each title is the menu or page title the user already sees for that journey, copied as is.
 */
internal object ProfileFeatures {
    private fun needed(form: String?) =
        ProfileForms.all.firstOrNull { it.id == form }?.fields.orEmpty().filter { it.requires }.map { it.ref }

    /** The review is where show_review stops from the destination, as the executor computes it. */
    private fun feature(
        intent: String,
        title: String,
        description: String,
        destination: String,
        form: String? = null,
        lookups: List<String> = emptyList(),
    ) =
        ProfileFeature(
            intent,
            title,
            description,
            needed(form),
            lookups,
            destination,
            form,
            form?.let { Reviews.stop(destination) },
        )

    /** A feature whose destination is its own form. */
    private fun filled(
        intent: String,
        title: String,
        description: String,
        form: String,
        lookups: List<String> = emptyList(),
    ) = feature(intent, title, description, form, form, lookups)

    private val accounts = listOf("accounts")
    private val cards = listOf("cards")
    private val wallets = listOf("wallets")
    private val sayad = listOf("sayad_cheques")
    private val vehicles = listOf("vehicles")
    private val toContact = listOf("contacts")

    /** Each D-022 service's title, what it does, and the data it looks up. */
    private val serviceFeatures =
        mapOf(
            "sayad-register" to Triple("صدور چک", "Prepare a Sayad cheque registration up to its review.", accounts),
            "sayad-answer" to Triple("تایید یا رد چک دریافتی", "Prepare the answer to a received Sayad cheque.", sayad),
            "sayad-transfer" to Triple("انتقال چک", "Prepare a Sayad cheque transfer up to its review.", sayad),
            "sayad-inquiry" to Triple("استعلام چک", "Check a Sayad cheque by its ID.", sayad),
            "sayad-history" to Triple("سوابق چک صیادی", "Show the Sayad cheque history.", sayad),
            "violations" to Triple("خلافی خودرو", "Prepare a driving violations inquiry.", vehicles),
            "transfer-tax" to Triple("مالیات نقل و انتقال", "Prepare a vehicle transfer tax inquiry.", vehicles),
            "documents" to Triple("مدارک خودرو", "Prepare a vehicle documents inquiry.", vehicles),
            "active-plates" to Triple("پلاک‌های فعال", "Prepare an active plates inquiry.", vehicles),
            "licence" to Triple("وضعیت گواهینامه", "Prepare a driving licence status inquiry.", vehicles),
            "negative-points" to Triple("نمره منفی گواهینامه", "Prepare a negative points inquiry.", vehicles),
            "credit-report" to Triple("گزارش اعتباری", "Prepare a paid credit report up to its fee.", accounts),
            "bounced-cheques" to Triple("استعلام چک برگشتی", "Prepare a paid bounced-cheque inquiry.", accounts),
            "credit-rate" to Triple("رتبه اعتباری", "Prepare a paid credit rating inquiry.", accounts),
            "loan-request" to Triple("درخواست تسهیلات", "Prepare a loan request; consent steps stay with the user.", accounts),
            "account-opening" to Triple("افتتاح حساب", "Prepare an account opening; consent stays with the user.", accounts),
            "wage-advance" to Triple("مساعده", "Prepare a wage advance; consent stays with the user.", accounts),
            "credit-product" to Triple("فعال‌سازی اعتبار خرید", "Prepare the credit product; consent stays with the user.", accounts),
            "rewards-history" to Triple("سوابق امتیاز", "Show the reward points history.", emptyList()),
            "rewards-transfer" to Triple("انتقال امتیاز", "Prepare a reward points transfer.", emptyList()),
        )

    /** A service ends on its review page, or earlier on the first page of the user's consent. */
    private fun service(key: String, title: String, description: String, lookups: List<String>): ProfileFeature {
        val steps = ServiceCatalog.flow(key).steps
        val end = steps.firstOrNull(ProfileForms::needsConsent) ?: steps.last()
        val form = steps.firstOrNull { it.fields.isNotEmpty() }?.page
        return feature(key.replace('-', '_'), title, description, end.page, form, lookups)
    }

    private val transfers =
        listOf(
            filled("card_transfer", "کارت به کارت", "Prepare a card-to-card transfer up to its review.", "S-174", cards + toContact),
            filled("internal_transfer", "انتقال داخلی", "Prepare an internal account transfer up to its review.", "S-192", accounts + toContact),
            filled("interbank_transfer", "انتقال بین بانکی", "Prepare an interbank transfer up to its review.", "S-191", accounts + toContact),
            filled("group_transfer", "انتقال وجه گروهی", "Prepare a group transfer from a source account and a total.", "S-187", accounts),
            filled("recurring_transfer", "درخواست انتقال وجه خودکار", "Prepare an automatic transfer schedule.", "S-181"),
            filled("saved_recipients", "مقصدهای آشنا", "Open the saved destinations list.", "S-066"),
            feature("repeat_transaction", "تراکنش‌های پرتکرار", "Open the saved repetitive transactions.", "S-200"),
        )

    private val payments =
        listOf(
            filled("bill_payment", "پرداخت قبض", "Prepare a bill payment up to its review.", "S-209", accounts + "bills"),
            feature("insurance_installment", "اقساط بیمه نامه", "Prepare an insurance installment payment.", "S-404", "S-401", listOf("insurance") + accounts),
            feature("credit_repayment", "جزئیات و بازپرداخت", "Open the credit card repayment; its amount dialog stays with the user.", "S-131", lookups = accounts),
            filled("mobile_top_up", "خرید شارژ", "Prepare a mobile top-up up to its review.", "S-211", accounts),
            filled("charity", "کمک به خیریه", "Prepare a charity donation up to its review.", "S-284", listOf("charities") + accounts),
            feature("loan_repayment", "تسهیلات در جریان و پرداخت", "Prepare a loan installment repayment up to its review.", "S-331", "S-306", listOf("loans") + accounts),
            feature("loan_groups", "تسهیلات باشگاه مشتریان", "Show the loan groups, read-only.", "S-307"),
            filled("identifier_conversion", "تبدیل حساب، کارت و شبا", "Convert an account, card or IBAN identifier.", "S-202"),
            filled("paid_identifier_inquiry", "تبدیل کارت به شبا", "Open the paid identifier inquiry; card secrets stay with the user.", "S-203", cards),
        )

    private val accountsAndCards =
        listOf(
            feature("accounts", "لیست و تغییر حساب", "Show and manage the visible accounts.", "S-085"),
            filled("account_statement", "صورتحساب", "Show an account statement for a date range.", "S-016"),
            filled("statement_request", "درخواست صورتحساب", "Prepare a statement file request.", "S-086", accounts),
            feature("transaction_history", "سوابق تراکنش‌ها", "Show the transaction history.", "S-091"),
            feature("cards", "اطلاعات کارت", "Show card information.", "S-012"),
            filled("card_balance", "موجودی بن کارت", "Open the secure balance form; its secrets stay with the user.", "S-119"),
            feature("card_request", "درخواست کارت", "Prepare a new card request up to its confirmation.", "S-128", "S-124", accounts),
            feature("card_block_and_pin", "مسدودی کارت", "Open card blocking; the block itself stays with the user.", "S-121"),
            feature("wallet_card", "اولین کیف کارت", "Prepare a wallet card request up to its confirmation.", "S-139", "S-140"),
            filled("profile", "اطلاعات تکمیلی", "Open the additional profile information form.", "S-064"),
            filled("saved_addresses", "افزودن آدرس", "Prepare a new saved address.", "S-463"),
            feature("settings", "تنظیمات برنامه", "Open the app settings menu.", "S-025"),
            feature("notifications", "اعلان‌ها", "Show the notifications.", "S-075"),
            feature("support", "سوالات متداول", "Show the frequently asked questions.", "S-077"),
            feature("service_search", "جستجوی خدمات", "Search services and pins.", "S-084"),
        )

    private val wallet =
        listOf(
            feature("wallet_overview", "کیف پول", "Show the wallets.", "S-111", lookups = wallets),
            filled("wallet_statement", "صورتحساب کیف", "Show a wallet statement for a date range.", "S-115", wallets),
            filled("wallet_to_wallet", "کیف به کیف", "Prepare a wallet transfer up to its review.", "S-116", wallets + toContact),
            filled("wallet_purchase", "خرید با کیف", "Open wallet purchase; it needs a scanned payment.", "S-100", wallets),
            filled("wallet_withdrawal", "برداشت از کیف", "Prepare a wallet withdrawal up to its review.", "S-106", wallets + accounts),
            filled("wallet_charge", "واریز به کیف", "Prepare a wallet charge from an account up to its review.", "S-104", accounts + wallets),
            filled("wallet_mobile_top_up", "خرید شارژ", "Prepare a mobile top-up from a wallet up to its review.", "S-102", wallets),
            filled("wallet_bill", "پرداخت قبض", "Prepare a bill payment from a wallet up to its review.", "S-109", wallets + "bills"),
            filled("account_funding", "افزایش موجودی", "Fill the account top-up amount; the gateway stays with the user.", "S-222", accounts),
            filled("wallet_funding", "واریز از درگاه پرداخت", "Fill the wallet top-up amount; the gateway stays with the user.", "S-231", wallets),
        )

    private val chequesAndVehicles =
        listOf(
            feature("cheque_books", "دسته چک‌ها", "Show the chequebooks.", "S-142", lookups = listOf("cheque_books")),
            filled("cheque_tracking", "پیگیری چک‌های من", "Track a cheque by its number.", "S-149", accounts),
            filled("chequebook_request", "درخواست دسته چک کاغذی", "Prepare a chequebook request up to its review.", "S-143", accounts),
            filled("cheque_report", "گزارش چک‌های واگذار شده", "Prepare the transferred-cheques report.", "S-148", accounts),
            feature("card_board", "کارتابل", "Show the card board tasks.", "S-154"),
            feature("digital_cheque_activation", "فعالسازی با هامون", "Open digital cheque activation; the signature stays with the user.", "S-157"),
            filled("digital_cheque_transfer", "واگذاری چک دیجیتال", "Prepare a digital cheque transfer up to its review.", "S-151", accounts + "digital_cheques"),
            feature("vehicles", "خدمات خودرو", "Show the vehicle services menu.", "S-253"),
            filled("annual_toll", "عوارض سالانه", "Fill the annual toll inquiry; its OTP stays with the user.", "S-241"),
            filled("traffic_toll", "عوارض شهرداری", "Fill the traffic toll inquiry; its OTP stays with the user.", "S-275"),
            filled("freeway_toll", "عوارض آزادراهی", "Fill the freeway toll inquiry; its OTP stays with the user.", "S-250"),
            filled("pollution_inquiry", "تردد طرح آلودگی هوا", "Fill the pollution inquiry; its OTP stays with the user.", "S-257"),
            feature("service_inquiries", "استعلامات", "Show the paid inquiries menu.", "S-443"),
            feature("finance_dashboard", "عملکرد مالی", "Show spending and budgets.", "S-436"),
            filled("budget_limit", "جزئیات بودجه", "Set a category's monthly budget limit.", "S-435"),
            feature("rewards", "باشگاه مشتریان", "Show the rewards menu.", "S-432"),
        )

    /** One per built journey. Independent receipts are never a destination. */
    val all =
        transfers +
            payments +
            accountsAndCards +
            wallet +
            chequesAndVehicles +
            serviceFeatures.map { (key, info) -> service(key, info.first, info.second, info.third) }
}
