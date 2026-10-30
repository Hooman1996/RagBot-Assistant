package org.ragbot.replica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

/** IT-ACCOUNT: recovered fields and callbacks, with explicit local-only review boundaries. */
@Composable
internal fun AccountTransferPage(
    page: String,
    scenario: String,
    draft: AccountTransferDraft,
    onChange: (AccountTransferDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            AccountTransferDestinations.MENU -> "انتقال وجه"
            AccountTransferDestinations.INTERNAL -> "انتقال داخلی"
            AccountTransferDestinations.INTERBANK -> "انتقال بین بانکی"
            AccountTransferDestinations.CONFIRM -> "تأیید انتقال وجه"
            AccountTransferDestinations.INTERNAL_RECEIPT -> "رسید انتقال داخلی"
            else -> "رسید انتقال شبا"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(AccountTransferFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    AccountTransferDestinations.MENU -> {
                        Text("فهرست نمونهٔ خدمات؛ ترتیب و دسترسی محلی است.")
                        Button(
                            { onOpen(AccountTransferDestinations.INTERNAL) },
                            Modifier.fillMaxWidth().testTag("transfer-internal"),
                        ) {
                            Text("انتقال داخلی")
                        }
                        Button(
                            { onOpen(AccountTransferDestinations.INTERBANK) },
                            Modifier.fillMaxWidth().testTag("transfer-interbank"),
                        ) {
                            Text("انتقال بین بانکی")
                        }
                    }
                    AccountTransferDestinations.INTERNAL,
                    AccountTransferDestinations.INTERBANK ->
                        AccountTransferForm(page, draft, onChange, onBack, onOpen, onUnavailable)
                    AccountTransferDestinations.CONFIRM ->
                        AccountTransferReview(draft, onChange, onBack, onUnavailable)
                    else -> AccountTransferReceipt(page, onBack, onUnavailable)
                }
            }
        }
    }
}

@Composable
private fun AccountTransferForm(
    page: String,
    draft: AccountTransferDraft,
    onChange: (AccountTransferDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var sourcePicker by rememberSaveable { mutableStateOf(false) }
    var reasonPicker by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val internal = page == AccountTransferDestinations.INTERNAL
    val source = AccountFixture.accounts.find { it.key == draft.sourceId }
    Text("حساب مبدأ")
    OutlinedButton(
        {
            focus.clearFocus()
            sourcePicker = true
        },
        Modifier.fillMaxWidth().testTag("account-transfer-source"),
    ) {
        Text(source?.let { "${it.type} · ${it.number}" } ?: "انتخاب حساب مبدأ")
    }
    TransferInput(
        "account-transfer-destination",
        if (internal) "شماره حساب مقصد" else "شماره شبا مقصد (IR)",
        draft.destination,
        { onChange(draft.copy(destination = it)) },
        numeric = internal,
    )
    TextButton(
        {
            focus.clearFocus()
            onUnavailable("انتخاب مقصد ذخیره‌شده در این پیش‌نمایش در دسترس نیست.")
        },
        Modifier.testTag("account-transfer-favorites"),
    ) {
        Text("مقصدهای ذخیره‌شده")
    }
    TransferInput(
        "account-transfer-amount",
        "مبلغ (ریال)",
        draft.amount,
        { onChange(draft.copy(amount = it)) },
        numeric = true,
    )
    Text("بابت")
    OutlinedButton(
        {
            focus.clearFocus()
            reasonPicker = true
        },
        Modifier.fillMaxWidth().testTag("account-transfer-reason"),
    ) {
        Text(draft.reason.ifEmpty { "لطفاً انتخاب کنید" })
    }
    TextButton(
        {
            focus.clearFocus()
            extra = !extra
        },
        Modifier.testTag("account-transfer-extra"),
    ) {
        Text("افزودن شناسه و توضیحات تراکنش")
    }
    if (extra) {
        TransferInput(
            "account-transfer-payment-id",
            "شناسه واریز (اختیاری)",
            draft.paymentId,
            {
                if (latinDigits(it).all { ch -> ch in '0'..'9' } && it.length <= 30)
                    onChange(draft.copy(paymentId = it))
            },
            numeric = true,
        )
        TransferInput(
            "account-transfer-withdraw-id",
            "شناسه برداشت (اختیاری)",
            draft.withdrawId,
            {
                if (latinDigits(it).all { ch -> ch in '0'..'9' } && it.length <= 17)
                    onChange(draft.copy(withdrawId = it))
            },
            numeric = true,
        )
        TransferInput(
            "account-transfer-source-description",
            "شرح مبدأ (اختیاری)",
            draft.sourceDescription,
            { if (it.length <= 50) onChange(draft.copy(sourceDescription = it)) },
        )
        TransferInput(
            "account-transfer-destination-description",
            "شرح مقصد (اختیاری)",
            draft.destinationDescription,
            { if (it.length <= 50) onChange(draft.copy(destinationDescription = it)) },
        )
    }
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("account-transfer-error"),
        )
    }
    Text(
        "نام گیرنده، مجاز بودن انتقال و سقف‌ها نیازمند سرویس‌اند؛ در این نمونه بررسی نمی‌شوند.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        {
            focus.clearFocus()
            val review = draft.copy(mode = page)
            error = AccountTransferFixture.validate(review)
            if (error == null) {
                onChange(review)
                onOpen(AccountTransferDestinations.CONFIRM)
            }
        },
        Modifier.fillMaxWidth().testTag("account-transfer-continue"),
    ) {
        Text("ادامه — بررسی محلی")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
    if (sourcePicker) {
        AlertDialog(
            onDismissRequest = { sourcePicker = false },
            title = { Text("انتخاب حساب مبدأ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountFixture.accounts.forEach { account ->
                        TextButton(
                            {
                                onChange(draft.copy(sourceId = account.key))
                                sourcePicker = false
                            },
                            Modifier.testTag("account-transfer-source-${account.key}"),
                        ) {
                            Text("${account.type}\n${account.number}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ sourcePicker = false }) { Text("انصراف") } },
        )
    }
    if (reasonPicker) {
        AlertDialog(
            onDismissRequest = { reasonPicker = false },
            title = { Text("بابت — گزینه‌های ساختگی") },
            text = {
                Column {
                    AccountTransferFixture.reasons.forEachIndexed { index, reason ->
                        TextButton(
                            {
                                onChange(draft.copy(reason = reason))
                                reasonPicker = false
                            },
                            Modifier.testTag("account-transfer-reason-$index"),
                        ) {
                            Text(reason)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ reasonPicker = false }) { Text("انصراف") } },
        )
    }
    BackHandler(sourcePicker || reasonPicker) {
        sourcePicker = false
        reasonPicker = false
    }
}

@Composable
private fun AccountTransferReview(
    draft: AccountTransferDraft,
    onChange: (AccountTransferDraft) -> Unit,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val source = AccountFixture.accounts.find { it.key == draft.sourceId }
    Text(
        if (draft.mode == AccountTransferDestinations.INTERNAL) "انتقال داخلی — بررسی محلی"
        else "انتقال شبا — بررسی محلی"
    )
    TransferValue("مبدأ", source?.number ?: "نامشخص")
    TransferValue("مقصد", draft.destination)
    TransferValue(
        "مبلغ (ریال)",
        TransferRules.amountRial(draft.amount)?.let(::persianNumber) ?: "—",
    )
    TransferValue("بابت", draft.reason)
    if (draft.paymentId.isNotEmpty()) TransferValue("شناسه واریز", draft.paymentId)
    if (draft.withdrawId.isNotEmpty()) TransferValue("شناسه برداشت", draft.withdrawId)
    if (draft.sourceDescription.isNotEmpty()) TransferValue("شرح مبدأ", draft.sourceDescription)
    if (draft.destinationDescription.isNotEmpty())
        TransferValue("شرح مقصد", draft.destinationDescription)
    TransferInput(
        "account-transfer-note",
        "یادداشت محلی",
        draft.note,
        { onChange(draft.copy(note = it)) },
    )
    TextButton(
        { onUnavailable("انتخاب پیوست و فایل در پیش‌نمایش در دسترس نیست.") },
        Modifier.testTag("account-transfer-attachment"),
    ) {
        Text("افزودن یادداشت و پیوست")
    }
    Row {
        Checkbox(
            draft.addFavorite,
            { onChange(draft.copy(addFavorite = it)) },
            Modifier.testTag("account-transfer-add-favorite").semantics {
                contentDescription = "افزودن به مقصدها فقط در پیش‌نمایش"
            },
        )
        Text("افزودن به مقصدها — فقط انتخاب محلی")
    }
    Text(
        "تأیید، رمز پویا و ارسال انتقال در این پیش‌نمایش در دسترس نیست. این صفحه مجوز یا رسید بانکی تولید نمی‌کند."
    )
    Button(
        { onUnavailable("ارسال و تأیید بانکی در دسترس نیست؛ هیچ انتقالی انجام نشد.") },
        Modifier.fillMaxWidth().testTag("account-transfer-submit"),
    ) {
        Text("تأیید — در دسترس نیست")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
}

@Composable
private fun AccountTransferReceipt(
    page: String,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Text(
        "رسید ساختگی مستقل برای بازبینی؛ حاصل انتقال وجه نیست.",
        modifier = Modifier.testTag("account-transfer-receipt-disclosure"),
    )
    TransferValue(
        "نوع",
        if (page == AccountTransferDestinations.INTERNAL_RECEIPT) "انتقال داخلی" else "انتقال شبا",
    )
    TransferValue("وضعیت", "نمونهٔ نمایشی — بدون تراکنش بانکی")
    TransferValue("مبدأ", "0000000000001")
    TransferValue(
        "مقصد",
        if (page == AccountTransferDestinations.INTERNAL_RECEIPT) "0000000000002"
        else "IR000000000000000000000002",
    )
    TransferValue("مبلغ (ریال)", persianNumber(250_000L))
    TransferValue("شماره پیگیری ساختگی", "DEMO-ACCOUNT-0001")
    TransferValue("توضیحات", "رکورد ثابت ساختگی برای بررسی چیدمان و پیمایش رسید")
    Button(
        {
            onUnavailable("ذخیره، اشتراک‌گذاری و بازکردن برنامهٔ خارجی در پیش‌نمایش در دسترس نیست.")
        },
        Modifier.fillMaxWidth().testTag("account-transfer-share"),
    ) {
        Text("ذخیره و اشتراک‌گذاری")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
}

@Composable
private fun TransferValue(label: String, value: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = TextStyle(textDirection = TextDirection.Content))
        }
    }
}

@Composable
private fun TransferInput(
    tag: String,
    label: String,
    value: String,
    onChange: (String) -> Unit,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) },
        singleLine = true,
        textStyle =
            TextStyle(
                textDirection =
                    if (numeric || tag == "account-transfer-destination") TextDirection.Ltr
                    else TextDirection.Content
            ),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
    )
}
