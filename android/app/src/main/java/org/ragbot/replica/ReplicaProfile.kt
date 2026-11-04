package org.ragbot.replica

/** One field of a form; [ref] is its test tag. Secret fields are listed and never filled. */
internal data class FormField(val ref: String, val type: String, val secret: Boolean = false)

internal data class ProfileForm(val id: String, val fields: List<FormField>)

internal data class ProfileFeature(
    val intent: String,
    val description: String,
    val requiredSlots: List<String>,
    val lookups: List<String>,
    val destination: String,
    val form: String?,
)

private fun text(ref: String) = FormField(ref, "text")

private fun money(ref: String) = FormField(ref, "money")

private fun ref(ref: String) = FormField(ref, "ref")

private fun choice(ref: String) = FormField(ref, "choice")

private fun secret(ref: String) = FormField(ref, "text", secret = true)

/** What the replica exports for the executor. Generated into app_profiles/replica.profile.json. */
internal object ReplicaProfile {
    private val cardSecrets =
        listOf(
            secret("payment-card-cvv2"),
            secret("payment-card-expiry"),
            secret("payment-card-password"),
        )
    private val cardTransfer =
        listOf(
            text("card-transfer-source"),
            text("card-transfer-destination"),
            money("card-transfer-amount"),
        )
    private val accountTransfer =
        listOf(
            ref("account-transfer-source"),
            text("account-transfer-destination"),
            money("account-transfer-amount"),
            choice("account-transfer-reason"),
            text("account-transfer-payment-id"),
            text("account-transfer-withdraw-id"),
            text("account-transfer-source-description"),
            text("account-transfer-destination-description"),
        )
    private val bill = listOf(ref("bill-source"), text("bill-id"), text("bill-pay-id"))
    private val topUp =
        listOf(
            ref("topup-source"),
            text("topup-phone"),
            choice("topup-operator"),
            money("topup-amount"),
        )
    private val funding = listOf(money("funding-amount"), ref("funding-target"))
    private val conversion =
        listOf(choice("conversion-mode"), text("conversion-identifier"), choice("conversion-bank"))
    private val paidConversion = listOf(ref("conversion-source")) + cardSecrets
    private val secureBalance =
        listOf(
            text("secure-number"),
            secret("secure-cvv2"),
            secret("secure-expiry"),
            secret("secure-password"),
        )
    private val noLoginBlock =
        listOf(
            text("noLogin-number"),
            secret("noLogin-expiry"),
            secret("secret-cvv2"),
            secret("secret-password"),
        )
    private val walletAmount = listOf(ref("wallet-op-source"), money("wallet-op-amount"))
    private val toll =
        listOf(
            text("toll-plate-0"),
            choice("toll-letter"),
            text("toll-plate-1"),
            text("toll-plate-2"),
            text("toll-plate-3"),
            choice("toll-type"),
            choice("toll-owner"),
            text("toll-code"),
            text("toll-phone"),
            text("toll-vin"),
        )
    private val savedDestination =
        listOf(text("saved-name"), text("saved-number"), text("saved-description"))
    private val cardRequestSource = listOf(choice("source"), choice("delivery"))
    private val chequeTrack = listOf(ref("cheque-account"), text("cheque-no"))
    private val profileAddress =
        listOf(
            text("field-title"),
            choice("pick-addressProvince"),
            choice("pick-addressCity"),
            text("field-postal"),
            text("field-address"),
        )

    val forms =
        listOf(
            ProfileForm("S-174", cardTransfer),
            ProfileForm("S-175", cardTransfer),
            ProfileForm(
                "S-176",
                listOf(
                    text("card-transfer-note"),
                    secret("card-transfer-cvv2"),
                    secret("card-transfer-expiry"),
                    secret("card-transfer-password"),
                ),
            ),
            ProfileForm("S-192", accountTransfer),
            ProfileForm("S-191", accountTransfer),
            ProfileForm("S-173", listOf(text("account-transfer-note"))),
            ProfileForm(
                "S-187",
                listOf(
                    ref("group-source"),
                    money("group-total"),
                    text("group-withdraw-id"),
                    text("group-source-description"),
                ),
            ),
            ProfileForm(
                "S-184",
                listOf(
                    text("group-row-*-iban"),
                    money("group-row-*-amount"),
                    choice("group-row-*-reason"),
                    text("group-row-*-payment-id"),
                    text("group-row-*-description"),
                ),
            ),
            ProfileForm(
                "S-181",
                listOf(
                    text("schedule-title"),
                    text("schedule-start"),
                    text("schedule-end"),
                    choice("schedule-frequency"),
                    choice("schedule-day"),
                ),
            ),
            ProfileForm("S-066", savedDestination),
            ProfileForm("S-197", savedDestination),
            ProfileForm("S-209", bill),
            ProfileForm("S-210", bill),
            ProfileForm("S-208", bill),
            ProfileForm("S-207", listOf(text("bill-note"))),
            ProfileForm("S-205", cardSecrets),
            ProfileForm("S-206", cardSecrets),
            ProfileForm("S-401", listOf(choice("installment"), ref("insurance-source"))),
            ProfileForm("S-131", listOf(money("credit-amount"), ref("credit-source"))),
            ProfileForm("S-211", topUp),
            ProfileForm("S-213", topUp),
            ProfileForm("S-214", topUp),
            ProfileForm("S-215", cardSecrets),
            ProfileForm("S-216", cardSecrets),
            ProfileForm(
                "S-284",
                listOf(
                    money("charity-amount"),
                    ref("charity-source"),
                    text("charity-payment-id"),
                    text("charity-description"),
                ),
            ),
            ProfileForm(
                "S-306",
                listOf(choice("loan-installment"), ref("loan-source"), text("loan-payment-id")),
            ),
            ProfileForm("S-201", conversion),
            ProfileForm("S-202", conversion),
            ProfileForm("S-203", paidConversion),
            ProfileForm("S-204", paidConversion),
            ProfileForm("S-016", listOf(text("ledger-start"), text("ledger-end"))),
            ProfileForm(
                "S-086",
                listOf(
                    ref("export-account"),
                    text("statement-start"),
                    text("statement-end"),
                    choice("export-format"),
                ),
            ),
            ProfileForm("S-119", secureBalance - text("secure-number")),
            ProfileForm("S-120", secureBalance),
            ProfileForm("S-118", noLoginBlock),
            ProfileForm("S-122", noLoginBlock),
            ProfileForm("S-124", cardRequestSource),
            ProfileForm("S-127", cardRequestSource),
            ProfileForm("S-129", listOf(choice("address"))),
            ProfileForm("S-123", listOf(choice("color"))),
            ProfileForm("S-140", listOf(choice("wallet"))),
            ProfileForm("S-137", listOf(choice("wallet-address"))),
            ProfileForm(
                "S-064",
                listOf(
                    text("field-firstNameEn"),
                    text("field-lastNameEn"),
                    text("field-fathersName"),
                    text("field-issueDate"),
                    choice("pick-province"),
                    choice("pick-city"),
                    text("field-taxCode"),
                ),
            ),
            ProfileForm("S-463", profileAddress),
            ProfileForm("S-076", listOf(text("chatbot-draft"))),
            ProfileForm("S-079", listOf(text("suggestion-description"))),
            ProfileForm("S-082", listOf(text("support-draft"))),
            ProfileForm(
                "S-115",
                listOf(ref("wallet-picker"), text("wallet-start"), text("wallet-end")),
            ),
            ProfileForm("S-112", listOf(ref("wallet-picker"), money("wallet-amount"))),
            ProfileForm("S-116", walletAmount + text("wallet-op-destination")),
            ProfileForm("S-100", walletAmount),
            ProfileForm("S-106", walletAmount + ref("wallet-op-destination")),
            ProfileForm("S-104", walletAmount + ref("wallet-op-destination")),
            ProfileForm(
                "S-102",
                listOf(
                    ref("wallet-op-source"),
                    text("wallet-op-mobile"),
                    choice("wallet-op-operator"),
                    money("wallet-op-amount"),
                ),
            ),
            ProfileForm(
                "S-109",
                listOf(ref("wallet-op-source"), text("wallet-op-bill"), text("wallet-op-pay")),
            ),
            ProfileForm("S-222", funding),
            ProfileForm("S-231", funding),
            ProfileForm("S-221", funding),
            ProfileForm("S-230", funding),
            ProfileForm("S-149", chequeTrack),
            ProfileForm("S-150", chequeTrack),
            ProfileForm("S-143", listOf(ref("cheque-account"), choice("cheque-page"))),
            ProfileForm(
                "S-148",
                listOf(
                    ref("cheque-account"),
                    choice("cheque-status"),
                    text("cheque-from"),
                    text("cheque-to"),
                ),
            ),
            ProfileForm(
                "S-151",
                listOf(ref("cheque-account"), ref("digital-sayad"), choice("digital-bounce")),
            ),
            ProfileForm("S-241", toll),
            ProfileForm("S-275", toll),
            ProfileForm("S-250", toll),
            ProfileForm("S-257", toll),
        )

    private fun feature(
        intent: String,
        description: String,
        destination: String,
        form: String?,
        slots: List<String> = emptyList(),
        lookups: List<String> = emptyList(),
    ) = ProfileFeature(intent, description, slots, lookups, destination, form)

    private val accounts = listOf("accounts")
    private val cards = listOf("cards")
    private val wallets = listOf("wallets")

    /** One per built journey. Independent receipts are never a destination. */
    val features =
        listOf(
            feature(
                "card_transfer",
                "Prepare a card-to-card transfer up to its review.",
                "S-175",
                "S-175",
                listOf("card-transfer-source", "card-transfer-destination", "card-transfer-amount"),
                cards,
            ),
            feature(
                "internal_transfer",
                "Prepare an internal account transfer up to its review.",
                "S-192",
                "S-192",
                listOf(
                    "account-transfer-source",
                    "account-transfer-destination",
                    "account-transfer-amount",
                    "account-transfer-reason",
                ),
                accounts,
            ),
            feature(
                "interbank_transfer",
                "Prepare an interbank transfer up to its review.",
                "S-191",
                "S-191",
                listOf(
                    "account-transfer-source",
                    "account-transfer-destination",
                    "account-transfer-amount",
                    "account-transfer-reason",
                ),
                accounts,
            ),
            feature(
                "group_transfer",
                "Prepare a group transfer from a source account and a total.",
                "S-187",
                "S-187",
                listOf("group-source", "group-total"),
                accounts,
            ),
            feature(
                "recurring_transfer",
                "Prepare an automatic transfer schedule.",
                "S-181",
                "S-181",
                listOf("schedule-title", "schedule-start", "schedule-end", "schedule-frequency"),
            ),
            feature("saved_recipients", "Open the saved destinations list.", "S-066", "S-066"),
            feature("repeat_transaction", "Open the saved repetitive transactions.", "S-200", null),
            feature(
                "bill_payment",
                "Prepare a bill payment up to its review.",
                "S-209",
                "S-209",
                listOf("bill-source", "bill-id", "bill-pay-id"),
                accounts,
            ),
            feature(
                "insurance_installment",
                "Prepare an insurance installment payment.",
                "S-403",
                null,
                lookups = listOf("insurance_policies"),
            ),
            feature(
                "credit_repayment",
                "Prepare a credit card repayment up to its review.",
                "S-131",
                "S-131",
                listOf("credit-amount", "credit-source"),
                accounts,
            ),
            feature(
                "mobile_top_up",
                "Prepare a mobile top-up up to its review.",
                "S-211",
                "S-211",
                listOf("topup-source", "topup-phone", "topup-amount"),
                accounts,
            ),
            feature(
                "charity",
                "Prepare a charity donation up to its review.",
                "S-283",
                null,
                lookups = listOf("charities"),
            ),
            feature(
                "loan_repayment",
                "Prepare a loan installment repayment up to its review.",
                "S-338",
                null,
                lookups = listOf("loans"),
            ),
            feature("loan_groups", "Show the loan groups, read-only.", "S-307", null),
            feature(
                "identifier_conversion",
                "Convert an account, card or IBAN identifier.",
                "S-201",
                "S-201",
                listOf("conversion-mode", "conversion-identifier"),
            ),
            feature(
                "paid_identifier_inquiry",
                "Open the paid identifier inquiry; card secrets stay with the user.",
                "S-203",
                "S-203",
                listOf("conversion-source"),
                cards,
            ),
            feature("accounts", "Show and manage the visible accounts.", "S-085", null),
            feature(
                "account_statement",
                "Show an account statement for a date range.",
                "S-016",
                "S-016",
                listOf("ledger-start", "ledger-end"),
            ),
            feature(
                "statement_request",
                "Prepare a statement file request.",
                "S-086",
                "S-086",
                listOf("export-account", "statement-start", "statement-end", "export-format"),
                accounts,
            ),
            feature("transaction_history", "Show the transaction history.", "S-091", null),
            feature("cards", "Show card information.", "S-012", null),
            feature(
                "card_balance",
                "Open the secure balance form; its secrets stay with the user.",
                "S-119",
                "S-119",
            ),
            feature(
                "card_request",
                "Prepare a new card request up to its confirmation.",
                "S-124",
                "S-124",
                listOf("source", "delivery"),
                accounts,
            ),
            feature(
                "card_block_and_pin",
                "Open card blocking; the block itself stays with the user.",
                "S-121",
                null,
            ),
            feature(
                "wallet_card",
                "Prepare a wallet card request up to its confirmation.",
                "S-140",
                "S-140",
            ),
            feature("profile", "Open the additional profile information form.", "S-064", "S-064"),
            feature(
                "saved_addresses",
                "Prepare a new saved address.",
                "S-463",
                "S-463",
                listOf("field-title", "field-postal", "field-address"),
            ),
            feature("settings", "Open the app settings menu.", "S-025", null),
            feature("notifications", "Show the notifications.", "S-075", null),
            feature("support", "Show the frequently asked questions.", "S-077", null),
            feature("service_search", "Search services and pins.", "S-084", null),
            feature("wallet_overview", "Show the wallets.", "S-111", null, lookups = wallets),
            feature(
                "wallet_statement",
                "Show a wallet statement for a date range.",
                "S-115",
                "S-115",
                listOf("wallet-start", "wallet-end"),
                wallets,
            ),
            feature(
                "wallet_to_wallet",
                "Prepare a wallet transfer up to its review.",
                "S-116",
                "S-116",
                listOf("wallet-op-source", "wallet-op-destination", "wallet-op-amount"),
                wallets,
            ),
            feature(
                "wallet_purchase",
                "Open wallet purchase; it needs a scanned payment.",
                "S-100",
                "S-100",
                listOf("wallet-op-source", "wallet-op-amount"),
                wallets,
            ),
            feature(
                "wallet_withdrawal",
                "Prepare a wallet withdrawal up to its review.",
                "S-106",
                "S-106",
                listOf("wallet-op-source", "wallet-op-destination", "wallet-op-amount"),
                wallets,
            ),
            feature(
                "wallet_charge",
                "Prepare a wallet charge from an account up to its review.",
                "S-104",
                "S-104",
                listOf("wallet-op-source", "wallet-op-destination", "wallet-op-amount"),
                accounts,
            ),
            feature(
                "wallet_mobile_top_up",
                "Prepare a mobile top-up from a wallet up to its review.",
                "S-102",
                "S-102",
                listOf("wallet-op-source", "wallet-op-mobile", "wallet-op-amount"),
                wallets,
            ),
            feature(
                "wallet_bill",
                "Prepare a bill payment from a wallet up to its review.",
                "S-109",
                "S-109",
                listOf("wallet-op-source", "wallet-op-bill", "wallet-op-pay"),
                wallets,
            ),
            feature(
                "account_funding",
                "Fill the account top-up amount; the gateway stays with the user.",
                "S-222",
                "S-222",
                listOf("funding-amount"),
                accounts,
            ),
            feature(
                "wallet_funding",
                "Fill the wallet top-up amount; the gateway stays with the user.",
                "S-231",
                "S-231",
                listOf("funding-amount"),
                wallets,
            ),
            feature("cheque_books", "Show the chequebooks.", "S-142", null, lookups = accounts),
            feature(
                "cheque_tracking",
                "Track a cheque by its number.",
                "S-149",
                "S-149",
                listOf("cheque-account", "cheque-no"),
                accounts,
            ),
            feature(
                "chequebook_request",
                "Prepare a chequebook request up to its review.",
                "S-143",
                "S-143",
                listOf("cheque-account", "cheque-page"),
                accounts,
            ),
            feature(
                "cheque_report",
                "Prepare the transferred-cheques report.",
                "S-148",
                "S-148",
                listOf("cheque-account", "cheque-status"),
                accounts,
            ),
            feature("card_board", "Show the card board tasks.", "S-154", null),
            feature(
                "digital_cheque_activation",
                "Open digital cheque activation; the signature stays with the user.",
                "S-157",
                null,
            ),
            feature(
                "digital_cheque_transfer",
                "Prepare a digital cheque transfer up to its review.",
                "S-151",
                "S-151",
                listOf("cheque-account", "digital-sayad"),
                accounts,
            ),
            feature("vehicles", "Show the vehicle services menu.", "S-253", null),
            feature(
                "annual_toll",
                "Fill the annual toll inquiry; its OTP stays with the user.",
                "S-241",
                "S-241",
            ),
            feature(
                "traffic_toll",
                "Fill the traffic toll inquiry; its OTP stays with the user.",
                "S-275",
                "S-275",
            ),
            feature(
                "freeway_toll",
                "Fill the freeway toll inquiry; its OTP stays with the user.",
                "S-250",
                "S-250",
            ),
            feature(
                "pollution_inquiry",
                "Fill the pollution inquiry; its OTP stays with the user.",
                "S-257",
                "S-257",
            ),
        )

    /** Every confirm, submit, pay, PIN, OTP and password control. No exported edge uses these. */
    val userOnlyActions =
        listOf(
            "account-transfer-submit",
            "activation-send",
            "additional-ok",
            "address-save",
            "apply-accounts",
            "bill-accept",
            "block-ok",
            "card-transfer-otp",
            "card-transfer-submit",
            "card-visible",
            "cardboard-act",
            "cardboard-reject",
            "continuous-confirm",
            "delete-ok",
            "digital-activate",
            "digital-confirm",
            "digital-deactivate",
            "digital-update",
            "edit-ok",
            "facility-ok",
            "facility-password",
            "faq-support",
            "fetch-ok",
            "funding-confirm",
            "funding-fetch",
            "funding-outcome-*",
            "group-verify-accept",
            "noLogin-ok",
            "notification-delete-confirm",
            "notify-off",
            "otp-code",
            "otp-ok",
            "payment-card-otp",
            "payment-submit",
            "pin-confirm",
            "pin-ok",
            "repetitive-delete-confirm",
            "request-submit",
            "saved-delete-confirm",
            "secret-*",
            "secure-ok",
            "statement-request",
            "suggestion-submit",
            "support-identity-continue",
            "support-mobile",
            "support-otp",
            "support-otp-continue",
            "support-send",
            "toll-fee-*",
            "topup-accept",
            "vehicle-delete-confirm",
            "wallet-op-submit",
            "wallet-submit",
        )
}
