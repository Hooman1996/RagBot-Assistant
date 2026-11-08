package org.ragbot.replica

/*
 * One feature per built journey: what it prepares, where it ends and what it looks up.
 * Read by the profile export. Python's task runner routes through a feature's form to its
 * destination, so a page shared by several services is still reached through the right one.
 * A feature's destination is never a receipt: every journey stops before the user's own step.
 */
internal object ProfileFeatures {
    private fun needed(form: String?) =
        ProfileForms.all.firstOrNull { it.id == form }?.fields.orEmpty().filter { it.requires }.map { it.ref }

    private fun feature(
        intent: String,
        description: String,
        destination: String,
        form: String? = null,
        lookups: List<String> = emptyList(),
    ) = ProfileFeature(intent, description, needed(form), lookups, destination, form)

    /** A feature whose destination is its own form. */
    private fun filled(
        intent: String,
        description: String,
        form: String,
        lookups: List<String> = emptyList(),
    ) = feature(intent, description, form, form, lookups)

    private val accounts = listOf("accounts")
    private val cards = listOf("cards")
    private val wallets = listOf("wallets")
    private val sayad = listOf("sayad_cheques")
    private val vehicles = listOf("vehicles")
    private val toContact = listOf("contacts")

    /** What each D-022 service does, and the data it looks up. */
    private val serviceFeatures =
        mapOf(
            "sayad-register" to ("Prepare a Sayad cheque registration up to its review." to accounts),
            "sayad-answer" to ("Prepare the answer to a received Sayad cheque." to sayad),
            "sayad-transfer" to ("Prepare a Sayad cheque transfer up to its review." to sayad),
            "sayad-inquiry" to ("Check a Sayad cheque by its ID." to sayad),
            "sayad-history" to ("Show the Sayad cheque history." to sayad),
            "violations" to ("Prepare a driving violations inquiry." to vehicles),
            "transfer-tax" to ("Prepare a vehicle transfer tax inquiry." to vehicles),
            "documents" to ("Prepare a vehicle documents inquiry." to vehicles),
            "active-plates" to ("Prepare an active plates inquiry." to vehicles),
            "licence" to ("Prepare a driving licence status inquiry." to vehicles),
            "negative-points" to ("Prepare a negative points inquiry." to vehicles),
            "credit-report" to ("Prepare a paid credit report up to its fee." to accounts),
            "bounced-cheques" to ("Prepare a paid bounced-cheque inquiry." to accounts),
            "credit-rate" to ("Prepare a paid credit rating inquiry." to accounts),
            "loan-request" to ("Prepare a loan request; consent steps stay with the user." to accounts),
            "account-opening" to ("Prepare an account opening; consent stays with the user." to accounts),
            "wage-advance" to ("Prepare a wage advance; consent stays with the user." to accounts),
            "credit-product" to ("Prepare the credit product; consent stays with the user." to accounts),
            "rewards-history" to ("Show the reward points history." to emptyList()),
            "rewards-transfer" to ("Prepare a reward points transfer." to emptyList()),
        )

    /** A service ends on its review page, or earlier on the first page of the user's consent. */
    private fun service(key: String, description: String, lookups: List<String>): ProfileFeature {
        val steps = ServiceCatalog.flow(key).steps
        val end = steps.firstOrNull(ProfileForms::needsConsent) ?: steps.last()
        val form = steps.firstOrNull { it.fields.isNotEmpty() }?.page
        return feature(key.replace('-', '_'), description, end.page, form, lookups)
    }

    private val transfers =
        listOf(
            filled("card_transfer", "Prepare a card-to-card transfer up to its review.", "S-174", cards + toContact),
            filled("internal_transfer", "Prepare an internal account transfer up to its review.", "S-192", accounts + toContact),
            filled("interbank_transfer", "Prepare an interbank transfer up to its review.", "S-191", accounts + toContact),
            filled("group_transfer", "Prepare a group transfer from a source account and a total.", "S-187", accounts),
            filled("recurring_transfer", "Prepare an automatic transfer schedule.", "S-181"),
            filled("saved_recipients", "Open the saved destinations list.", "S-066"),
            feature("repeat_transaction", "Open the saved repetitive transactions.", "S-200"),
        )

    private val payments =
        listOf(
            filled("bill_payment", "Prepare a bill payment up to its review.", "S-209", accounts + "bills"),
            feature("insurance_installment", "Prepare an insurance installment payment.", "S-404", "S-401", listOf("insurance") + accounts),
            feature("credit_repayment", "Open the credit card repayment; its amount dialog stays with the user.", "S-131", lookups = accounts),
            filled("mobile_top_up", "Prepare a mobile top-up up to its review.", "S-211", accounts),
            filled("charity", "Prepare a charity donation up to its review.", "S-284", listOf("charities") + accounts),
            feature("loan_repayment", "Prepare a loan installment repayment up to its review.", "S-331", "S-306", listOf("loans") + accounts),
            feature("loan_groups", "Show the loan groups, read-only.", "S-307"),
            filled("identifier_conversion", "Convert an account, card or IBAN identifier.", "S-202"),
            filled("paid_identifier_inquiry", "Open the paid identifier inquiry; card secrets stay with the user.", "S-203", cards),
        )

    private val accountsAndCards =
        listOf(
            feature("accounts", "Show and manage the visible accounts.", "S-085"),
            filled("account_statement", "Show an account statement for a date range.", "S-016"),
            filled("statement_request", "Prepare a statement file request.", "S-086", accounts),
            feature("transaction_history", "Show the transaction history.", "S-091"),
            feature("cards", "Show card information.", "S-012"),
            filled("card_balance", "Open the secure balance form; its secrets stay with the user.", "S-119"),
            feature("card_request", "Prepare a new card request up to its confirmation.", "S-128", "S-124", accounts),
            feature("card_block_and_pin", "Open card blocking; the block itself stays with the user.", "S-121"),
            feature("wallet_card", "Prepare a wallet card request up to its confirmation.", "S-139", "S-140"),
            filled("profile", "Open the additional profile information form.", "S-064"),
            filled("saved_addresses", "Prepare a new saved address.", "S-463"),
            feature("settings", "Open the app settings menu.", "S-025"),
            feature("notifications", "Show the notifications.", "S-075"),
            feature("support", "Show the frequently asked questions.", "S-077"),
            feature("service_search", "Search services and pins.", "S-084"),
        )

    private val wallet =
        listOf(
            feature("wallet_overview", "Show the wallets.", "S-111", lookups = wallets),
            filled("wallet_statement", "Show a wallet statement for a date range.", "S-115", wallets),
            filled("wallet_to_wallet", "Prepare a wallet transfer up to its review.", "S-116", wallets + toContact),
            filled("wallet_purchase", "Open wallet purchase; it needs a scanned payment.", "S-100", wallets),
            filled("wallet_withdrawal", "Prepare a wallet withdrawal up to its review.", "S-106", wallets + accounts),
            filled("wallet_charge", "Prepare a wallet charge from an account up to its review.", "S-104", accounts + wallets),
            filled("wallet_mobile_top_up", "Prepare a mobile top-up from a wallet up to its review.", "S-102", wallets),
            filled("wallet_bill", "Prepare a bill payment from a wallet up to its review.", "S-109", wallets + "bills"),
            filled("account_funding", "Fill the account top-up amount; the gateway stays with the user.", "S-222", accounts),
            filled("wallet_funding", "Fill the wallet top-up amount; the gateway stays with the user.", "S-231", wallets),
        )

    private val chequesAndVehicles =
        listOf(
            feature("cheque_books", "Show the chequebooks.", "S-142", lookups = listOf("cheque_books")),
            filled("cheque_tracking", "Track a cheque by its number.", "S-149", accounts),
            filled("chequebook_request", "Prepare a chequebook request up to its review.", "S-143", accounts),
            filled("cheque_report", "Prepare the transferred-cheques report.", "S-148", accounts),
            feature("card_board", "Show the card board tasks.", "S-154"),
            feature("digital_cheque_activation", "Open digital cheque activation; the signature stays with the user.", "S-157"),
            filled("digital_cheque_transfer", "Prepare a digital cheque transfer up to its review.", "S-151", accounts + "digital_cheques"),
            feature("vehicles", "Show the vehicle services menu.", "S-253"),
            filled("annual_toll", "Fill the annual toll inquiry; its OTP stays with the user.", "S-241"),
            filled("traffic_toll", "Fill the traffic toll inquiry; its OTP stays with the user.", "S-275"),
            filled("freeway_toll", "Fill the freeway toll inquiry; its OTP stays with the user.", "S-250"),
            filled("pollution_inquiry", "Fill the pollution inquiry; its OTP stays with the user.", "S-257"),
            feature("service_inquiries", "Show the paid inquiries menu.", "S-443"),
            feature("finance_dashboard", "Show spending and budgets.", "S-436"),
            filled("budget_limit", "Set a category's monthly budget limit.", "S-435"),
            feature("rewards", "Show the rewards menu.", "S-432"),
        )

    /** One per built journey. Independent receipts are never a destination. */
    val all =
        transfers +
            payments +
            accountsAndCards +
            wallet +
            chequesAndVehicles +
            serviceFeatures.map { (key, info) -> service(key, info.first, info.second) }
}
