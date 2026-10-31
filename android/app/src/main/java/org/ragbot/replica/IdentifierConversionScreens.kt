package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** IDENTIFIER-RECOVERY: shared five-direction form and response-backed result presentation. */
@Composable
internal fun ConversionPage(
    page: String,
    scenario: String,
    resultDraft: ConversionDraft,
    onResult: (ConversionDraft) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    PageFrame(
        page,
        if (page == ConversionDestinations.RESULT) "نتیجه تبدیل" else "تبدیل حساب، کارت و شبا",
        onBack,
    ) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else if (page == ConversionDestinations.RESULT) {
            ConversionResultContent(resultDraft, onBack, onUnavailable)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(PaymentFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                if (page in ConversionDestinations.forms) {
                    ConversionForm(onResult, onUnavailable)
                } else {
                    ConversionPaidPreview(page, onBack, onUnavailable)
                }
            }
        }
    }
}

@Composable
private fun ConversionForm(onResult: (ConversionDraft) -> Unit, onUnavailable: (String) -> Unit) {
    // The recovered layout has five separate expandable fields; values never cross modes.
    var active by rememberSaveable { mutableStateOf("") }
    var drafts by rememberSaveable { mutableStateOf(mapOf<String, ConversionDraft>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var bankPicker by rememberSaveable { mutableStateOf(false) }
    Text("فهرست و بانک‌ها ساختگی هستند؛ نتیجه فقط برای شناسه‌های نمونه نمایش داده می‌شود.")
    Text("حساب نمونه: ${ConversionFixture.account}")
    Text("کارت نمونه: ${ConversionFixture.card}")
    Text("شبای نمونه: ${ConversionFixture.iban}")
    ConversionMode.entries.forEach { mode ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    {
                        active = if (active == mode.name) "" else mode.name
                        error = null
                    },
                    Modifier.fillMaxWidth().testTag("conversion-mode-${mode.name}"),
                ) {
                    Text(mode.title)
                }
                if (active == mode.name) {
                    val draft = drafts[active] ?: ConversionDraft(mode)
                    fun update(value: ConversionDraft) {
                        drafts = drafts + (mode.name to value)
                        error = null
                    }
                    if (mode.isIbanInput) Text("فقط برای بانک داخلی ساختگی؛ پیشوند IR اختیاری است.")
                    PaymentInput("conversion-identifier", mode.inputLabel, draft.identifier, null) {
                        update(draft.copy(identifier = it))
                    }
                    if (mode == ConversionMode.ACCOUNT_TO_IBAN) {
                        OutlinedButton(
                            { bankPicker = true },
                            Modifier.fillMaxWidth().testTag("conversion-bank"),
                        ) {
                            Text(
                                ConversionFixture.banks.find { it.key == draft.bankKey }?.title
                                    ?: "انتخاب بانک"
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            draft.addFavorite,
                            { update(draft.copy(addFavorite = it)) },
                            Modifier.testTag("conversion-favorite").semantics {
                                contentDescription = "افزودن به آشناها — فقط انتخاب محلی"
                            },
                        )
                        Text("افزودن به آشناها — فقط انتخاب محلی")
                    }
                    error?.let {
                        Text(
                            it,
                            Modifier.testTag("conversion-error"),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        {
                            error = ConversionFixture.error(draft)
                            if (error == null) {
                                when {
                                    ConversionFixture.paidQuery(draft) ->
                                        onUnavailable("استعلام کارمزدی؛ مبلغ و شرایط نامشخص")
                                    ConversionFixture.result(draft) != null -> onResult(draft)
                                    else ->
                                        error =
                                            "نتیجه‌ای برای این شناسه در دادهٔ ساختگی وجود ندارد."
                                }
                            }
                        },
                        Modifier.fillMaxWidth().testTag("conversion-calculate"),
                    ) {
                        Text("محاسبه — پیش‌نمایش")
                    }
                    if (bankPicker) {
                        AlertDialog(
                            onDismissRequest = { bankPicker = false },
                            title = { Text("انتخاب بانک") },
                            text = {
                                Column {
                                    ConversionFixture.banks.forEach { bank ->
                                        TextButton(
                                            {
                                                update(draft.copy(bankKey = bank.key))
                                                bankPicker = false
                                            },
                                            Modifier.testTag("conversion-bank-${bank.key}"),
                                        ) {
                                            Text(bank.title)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton({ bankPicker = false }) { Text("انصراف") }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversionPaidPreview(
    page: String,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var source by rememberSaveable { mutableStateOf("") }
    var picker by rememberSaveable { mutableStateOf(false) }
    Text("نمونهٔ مستقل استعلام کارمزدی؛ مبلغ، شرایط و نتیجه نامشخص است.")
    Text("کارت مقصد ساختگی: 0000010000000001")
    if (page == ConversionDestinations.PAID) {
        OutlinedButton(
            { picker = true },
            Modifier.fillMaxWidth().testTag("conversion-source-picker"),
        ) {
            Text(source.ifEmpty { "انتخاب کارت مبدا" })
        }
        if (picker) {
            AlertDialog(
                onDismissRequest = { picker = false },
                title = { Text("کارت‌های مبدا — دادهٔ ساختگی") },
                text = {
                    Column {
                        CardFixture.of(CardKind.DEBIT).forEach { card ->
                            TextButton(
                                {
                                    source = card.number
                                    picker = false
                                },
                                Modifier.testTag("conversion-source-${card.key}"),
                            ) {
                                Text(card.number)
                            }
                        }
                    }
                },
                confirmButton = { TextButton({ picker = false }) { Text("انصراف") } },
            )
        }
    } else {
        PaymentInput("conversion-source", "شماره کارت مبدا", source, null) { source = it }
    }
    // Same recovered card-information include as FR-11. Secrets are remember-only there.
    CardPaymentInfo(false, "استعلام کارمزدی", onBack, onUnavailable)
}

@Composable
private fun ColumnScope.ConversionResultContent(
    draft: ConversionDraft,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val result = ConversionFixture.result(draft)
    val context = LocalContext.current
    var copied by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("نتیجه و مالکیت کاملاً ساختگی است؛ استعلامی انجام نشده است.")
        if (result == null) {
            Text("موردی برای نمایش وجود ندارد.")
        } else {
            val cards =
                if (draft.mode == ConversionMode.IBAN_TO_CARD) result.cards
                else listOf(ConversionFixture.normalizeIdentifier(draft.mode, draft.identifier))
            cards.forEachIndexed { index, card ->
                OutlinedCard(Modifier.fillMaxWidth().testTag("conversion-result-$index")) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TransferValue("دارنده", result.owner)
                        TransferValue("بانک", result.bank)
                        val values = buildList {
                            if (draft.mode.isCardInput || draft.mode == ConversionMode.IBAN_TO_CARD)
                                add("شماره کارت" to card)
                            if (
                                draft.mode != ConversionMode.CARD_TO_IBAN &&
                                    draft.mode != ConversionMode.IBAN_TO_CARD
                            )
                                add("شماره حساب" to result.account)
                            if (draft.mode != ConversionMode.CARD_TO_ACCOUNT)
                                add("شماره شبا" to result.iban)
                        }
                        values.forEach { (label, value) ->
                            TransferValue(label, value)
                            TextButton(
                                {
                                    copyPreview(context, value)
                                    copied = true
                                },
                                Modifier.testTag("conversion-copy-$index-$label"),
                            ) {
                                Text("کپی $label")
                            }
                        }
                    }
                }
            }
        }
        if (copied) Text("کپی شد.", Modifier.testTag("conversion-copied"))
    }
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton({ onUnavailable("اشتراک‌گذاری نتیجه") }, Modifier.weight(1f)) {
            Text("اشتراک‌گذاری")
        }
        Button(onBack, Modifier.testTag("conversion-return")) { Text("بازگشت") }
    }
}
