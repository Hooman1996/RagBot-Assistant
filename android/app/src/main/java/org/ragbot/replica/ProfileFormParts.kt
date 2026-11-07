package org.ragbot.replica

/*
 * The pieces ProfileForms builds its forms from: field makers, and the field groups several forms
 * share. It only describes fields. A secret field gets no words, so it can never be asked for.
 */
internal object FormParts {
    private fun field(ref: String, type: String, lookup: String? = null): FormField {
        val words = ProfileWords.of(ref)
        return FormField(
            ref,
            type,
            ask = words.ask,
            description = words.description,
            lookup = lookup,
        )
    }

    fun text(ref: String) = field(ref, "text")

    fun money(ref: String) = field(ref, "money")

    fun ref(ref: String, lookup: String) = field(ref, "ref", lookup)

    fun choice(ref: String) = field(ref, "choice")

    fun secret(ref: String) = FormField(ref, "text", secret = true)

    /** The same field, marked as one its screen needs. */
    val FormField.needed
        get() = copy(requires = true)

    fun form(id: String, vararg fields: FormField) = ProfileForm(id, fields.toList())

    val cardSecrets =
        listOf(
            secret("payment-card-cvv2"),
            secret("payment-card-expiry"),
            secret("payment-card-password"),
        )

    fun cardTransfer(source: String) =
        listOf(
            ref("card-transfer-source", source).needed,
            ref("card-transfer-destination", "contacts.card").needed,
            money("card-transfer-amount").needed,
        )

    fun accountTransfer(destination: String) =
        listOf(
            ref("account-transfer-source", "accounts").needed,
            ref("account-transfer-destination", destination).needed,
            money("account-transfer-amount").needed,
            choice("account-transfer-reason").needed,
            text("account-transfer-payment-id"),
            text("account-transfer-withdraw-id"),
            text("account-transfer-source-description"),
            text("account-transfer-destination-description"),
        )

    fun bill(source: String) =
        listOf(
            ref("bill-source", source).needed,
            text("bill-id").needed,
            text("bill-pay-id").needed,
        )

    fun topUp(source: String) =
        listOf(
            ref("topup-source", source).needed,
            text("topup-phone").needed,
            choice("topup-operator"),
            money("topup-amount").needed,
        )

    fun funding(target: String) =
        listOf(money("funding-amount").needed, ref("funding-target", target))

    val conversion =
        listOf(choice("conversion-mode"), text("conversion-identifier"), choice("conversion-bank"))
    val typedConversion =
        listOf(
            choice("conversion-mode"),
            text("conversion-identifier").needed,
            choice("conversion-bank"),
        )

    fun paidConversion(source: String) =
        listOf(ref("conversion-source", source).needed) + cardSecrets

    val secureSecrets =
        listOf(secret("secure-cvv2"), secret("secure-expiry"), secret("secure-password"))
    val noLoginBlock =
        listOf(
            text("noLogin-number"),
            secret("noLogin-expiry"),
            secret("secret-cvv2"),
            secret("secret-password"),
        )

    fun walletAmount(source: String) =
        listOf(ref("wallet-op-source", source).needed, money("wallet-op-amount").needed)

    val toll =
        listOf(
            text("toll-plate-0").needed,
            choice("toll-letter").needed,
            text("toll-plate-1"),
            text("toll-plate-2").needed,
            text("toll-plate-3").needed,
            choice("toll-type"),
            choice("toll-owner"),
            text("toll-code"),
            text("toll-phone"),
            text("toll-vin"),
        )
    val savedDestination =
        listOf(text("saved-name"), text("saved-number"), text("saved-description"))
    val cardRequestSource = listOf(choice("source").needed, choice("delivery").needed)
    val chequeAccount = ref("cheque-account", "accounts").needed
    val chequeTrack = listOf(chequeAccount, text("cheque-no").needed)
    val profileAddress =
        listOf(
            text("field-title").needed,
            choice("pick-addressProvince"),
            choice("pick-addressCity"),
            text("field-postal").needed,
            text("field-address").needed,
        )
}
