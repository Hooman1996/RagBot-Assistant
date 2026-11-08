package org.ragbot.replica

/*
 * The replica's forms: every field's tag, type, lookup and whether the screen needs it.
 * Read by the profile export, and by the bridge's Policy and Observer.
 * A field marked needed is one its screen's own validation refuses to leave empty. Secret fields
 * are listed so they can be refused, and must never be marked needed.
 */
internal object ProfileForms {
    /** A commitment in the user's own name: accepting terms, a note or a signature. */
    private val consentFields = setOf("agree", "note", "sign")

    /** True when [page] holds a consent only the user may give. */
    fun needsConsent(step: FlowStep) = step.fields.any { it.key in consentFields }

    /** A service field, in the words of its own declaration. */
    private fun serviceField(field: FlowField): FormField {
        val consent = field.key in consentFields
        return FormField(
            "svc-${field.key}",
            field.type,
            secret = consent,
            requires = !consent && field.check("") != null,
            ask = if (consent) null else "${field.label} چیست؟",
            description = if (consent) null else field.label,
            lookup = if (field.type == "ref") "accounts" else null,
        )
    }

    private val handWritten =
        with(FormParts) {
            listOf(
                form("S-027", text("login-username"), secret("login-password")),
                form("S-435", money("budget-limit").needed),
                ProfileForm("S-174", cardTransfer("cards")),
                ProfileForm("S-175", cardTransfer("cards.number")),
                form(
                    "S-176",
                    text("card-transfer-note"),
                    secret("card-transfer-cvv2"),
                    secret("card-transfer-expiry"),
                    secret("card-transfer-password"),
                ),
                ProfileForm("S-192", accountTransfer("contacts.account")),
                ProfileForm("S-191", accountTransfer("contacts.iban")),
                form("S-173", text("account-transfer-note")),
                form(
                    "S-187",
                    ref("group-source", "accounts").needed,
                    money("group-total").needed,
                    text("group-withdraw-id"),
                    text("group-source-description"),
                ),
                form(
                    "S-184",
                    text("group-row-*-iban"),
                    money("group-row-*-amount"),
                    choice("group-row-*-reason"),
                    text("group-row-*-payment-id"),
                    text("group-row-*-description"),
                ),
                form(
                    "S-181",
                    text("schedule-title").needed,
                    text("schedule-start").needed,
                    text("schedule-end").needed,
                    choice("schedule-frequency").needed,
                    choice("schedule-day"),
                ),
                ProfileForm("S-066", savedDestination),
                ProfileForm("S-197", savedDestination),
                ProfileForm("S-209", bill("accounts")),
                ProfileForm("S-210", bill("cards")),
                ProfileForm("S-208", bill("cards.number")),
                form("S-207", text("bill-note")),
                ProfileForm("S-205", cardSecrets),
                ProfileForm("S-206", cardSecrets),
                form("S-401", choice("installment").needed, ref("insurance-source", "accounts")),
                form("S-131", money("credit-amount"), ref("credit-source", "accounts")),
                ProfileForm("S-211", topUp("accounts")),
                ProfileForm("S-213", topUp("cards")),
                ProfileForm("S-214", topUp("cards.number")),
                ProfileForm("S-215", cardSecrets),
                ProfileForm("S-216", cardSecrets),
                form(
                    "S-284",
                    money("charity-amount").needed,
                    ref("charity-source", "accounts").needed,
                    text("charity-payment-id"),
                    text("charity-description"),
                ),
                form(
                    "S-306",
                    choice("loan-installment").needed,
                    ref("loan-source", "accounts"),
                    text("loan-payment-id"),
                ),
                ProfileForm("S-201", conversion),
                ProfileForm("S-202", typedConversion),
                ProfileForm("S-203", paidConversion("cards")),
                ProfileForm("S-204", paidConversion("cards.number")),
                form("S-016", text("ledger-start"), text("ledger-end")),
                form(
                    "S-086",
                    ref("export-account", "accounts").needed,
                    text("statement-start").needed,
                    text("statement-end").needed,
                    choice("export-format").needed,
                ),
                ProfileForm("S-119", secureSecrets),
                ProfileForm("S-120", listOf(text("secure-number")) + secureSecrets),
                ProfileForm("S-118", noLoginBlock),
                ProfileForm("S-122", noLoginBlock),
                ProfileForm("S-124", cardRequestSource),
                ProfileForm("S-127", cardRequestSource),
                form("S-129", choice("address").needed),
                form("S-123", choice("color").needed),
                form("S-140", choice("wallet")),
                form("S-137", choice("wallet-address").needed),
                form(
                    "S-064",
                    text("field-firstNameEn"),
                    text("field-lastNameEn"),
                    text("field-fathersName"),
                    text("field-issueDate"),
                    choice("pick-province"),
                    choice("pick-city"),
                    text("field-taxCode"),
                ),
                ProfileForm("S-463", profileAddress),
                form("S-076", text("chatbot-draft")),
                form("S-079", text("suggestion-description")),
                form("S-082", text("support-draft")),
                form(
                    "S-115",
                    ref("wallet-picker", "wallets"),
                    text("wallet-start").needed,
                    text("wallet-end").needed,
                ),
                form("S-112", ref("wallet-picker", "wallets"), money("wallet-amount").needed),
                ProfileForm(
                    "S-116",
                    walletAmount("wallets") + ref("wallet-op-destination", "contacts.wallet").needed,
                ),
                ProfileForm("S-100", walletAmount("wallets")),
                ProfileForm(
                    "S-106",
                    walletAmount("wallets") + ref("wallet-op-destination", "accounts").needed,
                ),
                ProfileForm(
                    "S-104",
                    walletAmount("accounts") + ref("wallet-op-destination", "wallets").needed,
                ),
                form(
                    "S-102",
                    ref("wallet-op-source", "wallets").needed,
                    text("wallet-op-mobile").needed,
                    choice("wallet-op-operator").needed,
                    money("wallet-op-amount").needed,
                ),
                form(
                    "S-109",
                    ref("wallet-op-source", "wallets").needed,
                    text("wallet-op-bill").needed,
                    text("wallet-op-pay").needed,
                ),
                ProfileForm("S-222", funding("accounts")),
                ProfileForm("S-231", funding("wallets")),
                ProfileForm("S-221", funding("accounts")),
                ProfileForm("S-230", funding("wallets")),
                ProfileForm("S-149", chequeTrack),
                ProfileForm("S-150", chequeTrack),
                form("S-143", chequeAccount, choice("cheque-page").needed),
                form(
                    "S-148",
                    chequeAccount,
                    choice("cheque-status").needed,
                    text("cheque-from"),
                    text("cheque-to"),
                ),
                form(
                    "S-151",
                    chequeAccount,
                    ref("digital-sayad", "digital_cheques").needed,
                    choice("digital-bounce"),
                ),
                ProfileForm("S-241", toll),
                ProfileForm("S-275", toll),
                ProfileForm("S-250", toll),
                ProfileForm("S-257", toll),
            )
        }

    val all =
        handWritten +
            ServiceCatalog.flows
                .flatMap { it.steps }
                .filter { it.fields.isNotEmpty() }
                .groupBy { it.page }
                .map { (page, steps) ->
                    val fields = steps.flatMap { it.fields }.map(::serviceField).distinct()
                    ProfileForm(page, fields)
                }
}
