package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** WO-RECOVERY: wallet operation forms and local confirmations. No operation is sent. */
@Composable
internal fun WalletOperationPage(
    page: String,
    scenario: String,
    draft: WalletOperationDraft,
    preview: WalletPreview?,
    onDraft: (WalletOperationDraft) -> Unit,
    onConfirm: (WalletPreview) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val operation = WalletOperation.of(page)
    PageFrame(page, operation.title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(WalletOperationFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                if (page == operation.form)
                    WalletOperationForm(
                        operation,
                        draft,
                        onDraft,
                        onConfirm,
                        onCancel,
                        onUnavailable,
                    )
                else if (preview != null)
                    WalletOperationConfirm(
                        operation,
                        draft,
                        preview,
                        onDraft,
                        onBack,
                        onCancel,
                        onUnavailable,
                    )
            }
        }
    }
}

private fun walletLabel(number: String) =
    WalletFixture.wallets.find { it.number == number }?.let { "${it.title} · ${it.number}" } ?: ""

private fun accountLabel(key: String) =
    AccountFixture.accounts.find { it.key == key }?.let { "${it.type} · ${it.number}" } ?: ""

@Composable
private fun Picker(
    tag: String,
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    error: String?,
    onSelect: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Text(title)
    OutlinedButton(
        {
            focus.clearFocus()
            open = true
        },
        Modifier.fillMaxWidth().testTag(tag),
    ) {
        Text(options.find { it.first == selected }?.second ?: title)
    }
    error?.let { FieldError(tag, it) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { (key, label) ->
                        TextButton(
                            {
                                onSelect(key)
                                open = false
                            },
                            Modifier.testTag("$tag-$key"),
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ open = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun FieldError(tag: String, text: String) {
    Text(
        text,
        Modifier.testTag("$tag-error"),
        color = MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
    )
}

@Composable
private fun WalletOperationForm(
    operation: WalletOperation,
    draft: WalletOperationDraft,
    onDraft: (WalletOperationDraft) -> Unit,
    onConfirm: (WalletPreview) -> Unit,
    onCancel: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var failure by rememberSaveable { mutableStateOf<String?>(null) }
    var balanceShown by rememberSaveable { mutableStateOf(false) }
    fun change(key: String, next: WalletOperationDraft) {
        // An edited field drops its stale error until the next Continue.
        errors = errors - key
        onDraft(next)
    }
    val wallets =
        WalletRules.active(WalletFixture.wallets).map { it.number to walletLabel(it.number) }
    val accounts = AccountFixture.accounts.map { it.key to accountLabel(it.key) }
    if (operation == WalletOperation.CHARGE)
        Picker("wallet-op-source", "انتخاب حساب مبدا", accounts, draft.source, errors["source"]) {
            balanceShown = false
            change("source", draft.copy(source = it))
        }
    else
        Picker("wallet-op-source", "انتخاب کیف مبدا", wallets, draft.source, errors["source"]) {
            balanceShown = false
            change("source", draft.copy(source = it))
        }
    val balance =
        if (operation == WalletOperation.CHARGE)
            AccountFixture.accounts.find { it.key == draft.source }?.balance
        else WalletFixture.wallets.find { it.number == draft.source }?.balance
    if (draft.source.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (balanceShown)
                Text(
                    balance?.let { "${persianNumber(it)} ریال" } ?: "موجودی در دسترس نیست",
                    Modifier.weight(1f).testTag("wallet-op-balance"),
                )
            else Spacer(Modifier.weight(1f))
            TextButton(
                { balanceShown = !balanceShown },
                Modifier.testTag("wallet-op-show-balance"),
            ) {
                Text(if (balanceShown) "عدم نمایش موجودی" else "مشاهده موجودی")
            }
        }
    }
    when (operation) {
        WalletOperation.W2W -> {
            PaymentInput(
                "wallet-op-destination",
                "شناسه کیف پول مقصد",
                draft.destination,
                errors["destination"],
                "شناسه مقصد را وارد کنید",
            ) {
                change("destination", draft.copy(destination = it))
            }
            // The familiar-destination picker (S-330) is not built.
            TextButton({ onUnavailable("مقصدهای آشنا") }, Modifier.testTag("wallet-op-familiar")) {
                Text("انتخاب از مقصدهای آشنا")
            }
        }
        // The destination arrives only with a scanned payment; this preview never scans.
        WalletOperation.BUY ->
            if (draft.destination.isNotEmpty()) {
                TransferValue("مقصد از بارکد نمونه", draft.destination)
                errors["destination"]?.let { FieldError("wallet-op-destination", it) }
            }
        WalletOperation.DECHARGE ->
            Picker(
                "wallet-op-destination",
                "انتخاب حساب مقصد",
                accounts,
                draft.destination,
                errors["destination"],
            ) {
                change("destination", draft.copy(destination = it))
            }
        WalletOperation.CHARGE ->
            Picker(
                "wallet-op-destination",
                "انتخاب کیف مقصد",
                wallets,
                draft.destination,
                errors["destination"],
            ) {
                change("destination", draft.copy(destination = it))
            }
        WalletOperation.MOBILE -> {
            PaymentInput(
                "wallet-op-mobile",
                "تلفن همراه",
                draft.mobile,
                errors["mobile"],
                "09121234567",
            ) {
                if (it.length <= 11) {
                    errors = errors - "operator"
                    change(
                        "mobile",
                        draft.copy(mobile = it, operator = TopUpRules.detect(it, draft.operator)),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopUpCharityFixture.operators.forEach {
                    FilterChip(
                        draft.operator == it.operator,
                        { change("operator", draft.copy(operator = it.operator, amount = "")) },
                        { Text(it.title) },
                        Modifier.testTag("wallet-op-operator-${it.operator}"),
                    )
                }
            }
            errors["operator"]?.let { FieldError("wallet-op-operator", it) }
        }
        WalletOperation.BILL -> {
            PaymentInput(
                "wallet-op-bill",
                "شناسه قبض",
                draft.billId,
                errors["bill"],
                "1234567891234",
            ) {
                if (it.length <= 13) change("bill", draft.copy(billId = it))
            }
            PaymentInput(
                "wallet-op-pay",
                "شناسه پرداخت",
                draft.payId,
                errors["pay"],
                "شناسه پرداخت حداکثر 13 رقم",
            ) {
                if (it.length <= 13) change("pay", draft.copy(payId = it))
            }
            // Camera scanning is out of scope (D-021).
            TextButton({ onUnavailable("اسکن بارکد قبض") }, Modifier.testTag("wallet-op-scan")) {
                Text("بارکد را اسکن نمایید")
            }
        }
    }
    if (operation == WalletOperation.MOBILE) {
        // Recovered: the amount is picked from the operator's server-supplied charge list.
        Text("مبلغ شارژ")
        TopUpRules.config(draft.operator)?.defaults?.forEach { amount ->
            FilterChip(
                draft.amount == amount.toString(),
                { change("amount", draft.copy(amount = amount.toString())) },
                { Text("${persianNumber(amount)} ریال") },
                Modifier.testTag("wallet-op-amount-$amount"),
            )
        }
        errors["amount"]?.let { FieldError("wallet-op-amount", it) }
    } else if (operation != WalletOperation.BILL) {
        PaymentInput("wallet-op-amount", "مبلغ (ریال)", draft.amount, errors["amount"]) {
            change("amount", draft.copy(amount = it))
        }
    }
    Button(
        {
            errors = WalletOperationRules.errors(operation, draft)
            if (errors.isEmpty()) {
                if (operation == WalletOperation.BUY && draft.destination.isEmpty()) {
                    onUnavailable("خرید بدون بارکد اسکن‌شده")
                } else {
                    val (preview, error) = WalletOperationRules.preview(operation, draft)
                    if (preview != null) onConfirm(preview) else failure = error
                }
            }
        },
        Modifier.fillMaxWidth().testTag("wallet-op-continue"),
    ) {
        Text(if (operation == WalletOperation.BUY) "تایید و ادامه" else "ادامه")
    }
    OutlinedButton(onCancel, Modifier.fillMaxWidth().testTag("wallet-op-cancel")) { Text("انصراف") }
    failure?.let {
        AlertDialog(
            onDismissRequest = { failure = null },
            text = { Text(it, Modifier.testTag("wallet-op-failure")) },
            confirmButton = { TextButton({ failure = null }) { Text("بستن") } },
        )
    }
}

@Composable
private fun WalletOperationConfirm(
    operation: WalletOperation,
    draft: WalletOperationDraft,
    preview: WalletPreview,
    onDraft: (WalletOperationDraft) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var editingNote by rememberSaveable { mutableStateOf(false) }
    TransferValue("نوع تراکنش", operation.title)
    if (operation == WalletOperation.CHARGE) {
        TransferValue("حساب مبدا", accountLabel(draft.source))
        TransferValue("کیف مقصد", walletLabel(draft.destination))
    } else {
        TransferValue("کیف مبدا", walletLabel(draft.source))
    }
    when (operation) {
        WalletOperation.W2W,
        WalletOperation.BUY -> TransferValue("کیف مقصد", latinDigits(draft.destination))
        WalletOperation.DECHARGE -> TransferValue("حساب مقصد", accountLabel(draft.destination))
        WalletOperation.MOBILE -> TransferValue("تلفن همراه", latinDigits(draft.mobile))
        WalletOperation.BILL -> {
            TransferValue("شناسه قبض", latinDigits(draft.billId))
            TransferValue("شناسه پرداخت", latinDigits(draft.payId))
        }
        WalletOperation.CHARGE -> {}
    }
    TransferValue(
        when (operation) {
            WalletOperation.MOBILE -> "اپراتور"
            WalletOperation.BILL -> "به نام"
            else -> "نام مقصد"
        },
        preview.destinationName,
    )
    TransferValue("مبلغ (ریال)", persianNumber(preview.amount))
    if (operation != WalletOperation.MOBILE) {
        if (draft.note.isNotEmpty()) TransferValue("یادداشت", draft.note)
        TextButton({ editingNote = true }, Modifier.testTag("wallet-op-note")) {
            Text("افزودن یادداشت")
        }
    }
    if (operation == WalletOperation.W2W || operation == WalletOperation.BUY) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                draft.favorite,
                { onDraft(draft.copy(favorite = it)) },
                Modifier.testTag("wallet-op-favorite"),
            )
            Text("افزودن به لیست مقصدهای آشنا")
        }
    }
    // Recovered: charge and withdrawal ask for an OTP, the others for a signature; both stay with
    // the user and nothing is sent.
    val otp = operation == WalletOperation.CHARGE || operation == WalletOperation.DECHARGE
    Button(
        {
            onUnavailable(
                if (otp) "تایید تراکنش با رمز پویا" else "امضا و انجام ${operation.title}"
            )
        },
        Modifier.fillMaxWidth().testTag("wallet-op-submit"),
    ) {
        Text(
            when {
                otp -> "تایید تراکنش — در دسترس نیست"
                operation == WalletOperation.BUY || operation == WalletOperation.MOBILE ->
                    "تایید و ادامه — در دسترس نیست"
                else -> "ادامه — در دسترس نیست"
            }
        )
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("wallet-op-edit")) {
        Text("بازگشت و ویرایش")
    }
    OutlinedButton(onCancel, Modifier.fillMaxWidth().testTag("wallet-op-cancel")) { Text("انصراف") }
    if (editingNote) {
        var value by rememberSaveable { mutableStateOf(draft.note) }
        AlertDialog(
            onDismissRequest = { editingNote = false },
            title = { Text("افزودن یادداشت") },
            text = {
                OutlinedTextField(
                    value,
                    { value = it },
                    Modifier.testTag("wallet-op-note-text"),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    {
                        onDraft(draft.copy(note = value.trim()))
                        editingNote = false
                    },
                    Modifier.testTag("wallet-op-note-accept"),
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ editingNote = false }) { Text("انصراف") } },
        )
    }
}
