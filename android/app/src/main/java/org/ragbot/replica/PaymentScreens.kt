package org.ragbot.replica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

/** BP-RECOVERY: bill, insurance and credit repayment previews. Nothing is paid or inquired. */
@Composable
internal fun PaymentPage(
    page: String,
    scenario: String,
    billForm: String,
    bill: BillDraft,
    onBill: (BillDraft) -> Unit,
    policy: InsurancePolicy?,
    installment: Int,
    insuranceSource: String,
    onInstallment: (Int) -> Unit,
    onInsuranceSource: (String) -> Unit,
    repayment: CreditRepayment?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onPolicy: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            BillPaymentDestinations.ACCOUNT -> "پرداخت قبض"
            BillPaymentDestinations.CARD -> "پرداخت قبض با کارت"
            BillPaymentDestinations.WITHOUT_SOURCE -> "پرداخت قبض بدون ورود"
            BillPaymentDestinations.CONFIRM -> "پرداخت قبض"
            BillPaymentDestinations.CARD_INFO,
            BillPaymentDestinations.CARD_INFO_NO_LOGIN -> "اطلاعات کارت"
            BillPaymentDestinations.RECEIPT -> "رسید قبض"
            InsuranceDestinations.MENU -> "بیمه"
            InsuranceDestinations.LIST -> "اقساط بیمه نامه"
            InsuranceDestinations.INSTALLMENTS -> "لیست اقساط بیمه نامه"
            InsuranceDestinations.CONFIRM -> "تایید پرداخت"
            InsuranceDestinations.RECEIPT -> "پرداخت اقساط بیمه نامه"
            CreditRepaymentDestinations.CONFIRM -> "بازپرداخت کارت اعتباری"
            else -> "رسید بازپرداخت کارت اعتباری"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(PaymentFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    in BillPaymentDestinations.forms ->
                        BillForm(page, bill, onBill, onBack, onOpen, onUnavailable)
                    BillPaymentDestinations.CONFIRM ->
                        BillConfirmation(billForm, bill, onBill, onBack, onOpen, onUnavailable)
                    BillPaymentDestinations.CARD_INFO,
                    BillPaymentDestinations.CARD_INFO_NO_LOGIN ->
                        BillCardInfo(page, bill, onBack, onUnavailable)
                    InsuranceDestinations.MENU -> {
                        Text("فهرست نمونهٔ خدمات؛ ترتیب و دسترسی محلی است.")
                        Button(
                            { onOpen(InsuranceDestinations.LIST) },
                            Modifier.fillMaxWidth().testTag("insurance-list"),
                        ) {
                            Text("اقساط بیمه نامه")
                        }
                        Button(
                            { onUnavailable("سوابق پرداخت اقساط بیمه") },
                            Modifier.fillMaxWidth().testTag("insurance-history"),
                        ) {
                            Text("سوابق پرداخت اقساط بیمه")
                        }
                    }
                    InsuranceDestinations.LIST -> InsuranceList(onPolicy)
                    InsuranceDestinations.INSTALLMENTS ->
                        if (policy != null)
                            InsuranceInstallments(
                                policy,
                                installment,
                                onInstallment,
                                onInsuranceSource,
                                onBack,
                                onOpen,
                            )
                    InsuranceDestinations.CONFIRM -> {
                        val chosen = policy?.installments?.getOrNull(installment)
                        if (policy != null && chosen != null) {
                            PaymentSource(insuranceSource)
                            TransferValue("بابت", "بابت پرداخت اقساط بیمه نامه")
                            TransferValue("شماره بیمه نامه", policy.number)
                            TransferValue("شماره قسط", persianNumber(chosen.number.toLong()))
                            // Designed for mock: the service's pre-validation sets the amount.
                            TransferValue("مبلغ (ریال)", persianNumber(chosen.remained))
                            PaymentActions(
                                "تایید و پرداخت",
                                "پرداخت قسط بیمه",
                                onBack,
                                onUnavailable,
                            )
                        }
                    }
                    CreditRepaymentDestinations.CONFIRM -> {
                        val card = CardFixture.of(CardKind.CREDIT).firstOrNull()
                        if (card != null && repayment != null) {
                            TransferValue("شماره کارت", cardNumberGroups(card.number))
                            PaymentSource(repayment.sourceId)
                            TransferValue("بابت", "بازپرداخت کارت اعتباری")
                            TransferValue("مبلغ (ریال)", persianNumber(repayment.amount))
                            PaymentActions(
                                "تایید و ادامه",
                                "بازپرداخت کارت اعتباری",
                                onBack,
                                onUnavailable,
                            )
                        }
                    }
                    else -> PaymentReceipt(page, onBack, onUnavailable)
                }
            }
        }
    }
}

@Composable
private fun BillForm(
    page: String,
    draft: BillDraft,
    onChange: (BillDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val focus = LocalFocusManager.current
    val sources =
        if (page == BillPaymentDestinations.ACCOUNT)
            AccountFixture.accounts.map { it.key to "${it.type} · ${it.number}" }
        else CardFixture.of(CardKind.DEBIT).map { it.key to it.number }
    if (page == BillPaymentDestinations.WITHOUT_SOURCE) {
        PaymentInput("bill-source", "کارت مبدا", draft.source, errors["source"]) {
            if (it.length <= 16) {
                // An edited field drops its stale error until the next Continue.
                errors = errors - "source" - "inquiry"
                onChange(draft.copy(source = it))
            }
        }
    } else {
        Text(if (page == BillPaymentDestinations.ACCOUNT) "حساب مبدا" else "کارت مبدا")
        OutlinedButton(
            {
                focus.clearFocus()
                picker = true
            },
            Modifier.fillMaxWidth().testTag("bill-source"),
        ) {
            Text(sources.find { it.first == draft.source }?.second ?: "انتخاب مبدا")
        }
    }
    PaymentInput("bill-id", "شناسه قبض", draft.billId, errors["bill"]) {
        if (it.length <= 13) {
            // An edited field drops its stale error until the next Continue.
            errors = errors - "bill" - "inquiry"
            onChange(draft.copy(billId = it))
        }
    }
    PaymentInput(
        "bill-pay-id",
        "شناسه پرداخت",
        draft.payId,
        errors["pay"],
        "شناسه پرداخت حداکثر 13 رقم",
    ) {
        if (it.length <= 13) {
            // An edited field drops its stale error until the next Continue.
            errors = errors - "pay" - "inquiry"
            onChange(draft.copy(payId = it))
        }
    }
    TextButton(
        {
            focus.clearFocus()
            onUnavailable("اسکن بارکد قبض")
        },
        Modifier.testTag("bill-scan"),
    ) {
        Text("اسکن بارکد")
    }
    errors["inquiry"]?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("bill-error"))
    }
    Text(
        "مبلغ و نام شرکت را سرویس استعلام می‌کند؛ در این نمونه از دادهٔ ساختگی خوانده می‌شود.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        {
            focus.clearFocus()
            errors = PaymentRules.billErrors(page, draft)
            if (errors.isEmpty()) {
                val error = PaymentRules.inquiry(draft).second
                if (error != null) errors = mapOf("inquiry" to error)
                else onOpen(BillPaymentDestinations.CONFIRM)
            }
        },
        Modifier.fillMaxWidth().testTag("bill-continue"),
    ) {
        Text("ادامه")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("انتخاب مبدا") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.forEach { (key, title) ->
                        TextButton(
                            {
                                onChange(draft.copy(source = key))
                                picker = false
                            },
                            Modifier.testTag("bill-source-$key"),
                        ) {
                            Text(title)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ picker = false }) { Text("انصراف") } },
        )
    }
    BackHandler(picker) { picker = false }
}

@Composable
private fun BillConfirmation(
    form: String,
    draft: BillDraft,
    onChange: (BillDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val bill = PaymentRules.inquiry(draft).first ?: return
    when (form) {
        BillPaymentDestinations.ACCOUNT -> PaymentSource(draft.source)
        BillPaymentDestinations.CARD ->
            TransferValue("مبدا", CardFixture.cards.find { it.key == draft.source }?.number ?: "")
        else -> TransferValue("مبدا", latinDigits(draft.source))
    }
    TransferValue("مقصد", bill.company)
    TransferValue("مبلغ (ریال)", persianNumber(bill.amount))
    TransferValue("شناسه قبض", bill.billId)
    TransferValue("شناسه پرداخت", bill.payId)
    OutlinedTextField(
        draft.note,
        { onChange(draft.copy(note = it)) },
        Modifier.fillMaxWidth().testTag("bill-note"),
        label = { Text("یادداشت محلی") },
    )
    TextButton({ onUnavailable("انتخاب و بارگذاری پیوست") }, Modifier.testTag("bill-attachment")) {
        Text("افزودن یادداشت و پیوست تراکنش")
    }
    Button(
        {
            when (form) {
                // Recovered: account payment asks for a signature or OTP in a confirmation sheet.
                BillPaymentDestinations.ACCOUNT -> onUnavailable("پرداخت قبض")
                BillPaymentDestinations.CARD -> onOpen(BillPaymentDestinations.CARD_INFO)
                else -> onOpen(BillPaymentDestinations.CARD_INFO_NO_LOGIN)
            }
        },
        Modifier.fillMaxWidth().testTag("bill-accept"),
    ) {
        Text(
            if (form == BillPaymentDestinations.ACCOUNT) "تایید و پرداخت — در دسترس نیست"
            else "تایید"
        )
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("bill-edit")) { Text("بازگشت و ویرایش") }
}

@Composable
private fun BillCardInfo(
    page: String,
    draft: BillDraft,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    PaymentRules.inquiry(draft).first?.let {
        TransferValue("مقصد", it.company)
        TransferValue("مبلغ (ریال)", persianNumber(it.amount))
    }
    CardPaymentInfo(page == BillPaymentDestinations.CARD_INFO, "پرداخت قبض", onBack, onUnavailable)
}

/** Recovered card-info fields; the login variant adds the password type and save choice. */
@Composable
internal fun CardPaymentInfo(
    login: Boolean,
    title: String,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    // Credentials reset through recreation and Back. They never enter SavedState or a fixture.
    var cvv2 by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dynamic by remember { mutableStateOf(true) }
    var keep by remember { mutableStateOf(false) }
    if (login) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                dynamic,
                { dynamic = true },
                Modifier.testTag("payment-card-dynamic").semantics {
                    contentDescription = "رمز پویا"
                },
            )
            Text("رمز پویا")
            RadioButton(
                !dynamic,
                { dynamic = false },
                Modifier.testTag("payment-card-static").semantics {
                    contentDescription = "رمز ثابت"
                },
            )
            Text("رمز ثابت")
        }
    }
    CardTransferSecret("CVV2", cvv2, "payment-card-cvv2") { cvv2 = it }
    CardTransferSecret("تاریخ انقضا", expiry, "payment-card-expiry") { expiry = it }
    CardTransferSecret("رمز دوم", password, "payment-card-password") { password = it }
    if (dynamic)
        OutlinedButton({ onUnavailable("دریافت رمز پویا") }, Modifier.testTag("payment-card-otp")) {
            Text("درخواست رمز پویا — غیرفعال")
        }
    if (login)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                keep,
                { keep = it },
                Modifier.testTag("payment-card-keep").semantics {
                    contentDescription = "ذخیره اطلاعات کارت فقط در پیش‌نمایش"
                },
            )
            Text("ذخیره اطلاعات کارت • فقط انتخاب محلی، چیزی ذخیره نمی‌شود")
        }
    PaymentActions("پرداخت", title, onBack, onUnavailable)
}

@Composable
private fun InsuranceList(onPolicy: (String) -> Unit) {
    TransferValue("نام بیمه شده", PaymentFixture.insuredName)
    TransferValue("کد ملی بیمه شده", PaymentFixture.insuredNationalCode)
    PaymentFixture.policies.forEach { policy ->
        OutlinedCard(
            Modifier.fillMaxWidth().testTag("insurance-policy-${policy.id}").clickable(
                role = Role.Button
            ) {
                onPolicy(policy.id)
            }
        ) {
            Column(Modifier.padding(12.dp)) {
                PaymentRow("شماره بیمه نامه", policy.number)
                PaymentRow("تاریخ شروع", policy.begin)
                PaymentRow("تاریخ پایان", policy.end)
                PaymentRow("وضعیت", policy.state)
            }
        }
    }
}

@Composable
private fun InsuranceInstallments(
    policy: InsurancePolicy,
    selected: Int,
    onSelect: (Int) -> Unit,
    onSource: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            PaymentRow("شماره بیمه نامه", policy.number)
            PaymentRow("وضعیت", policy.state)
            PaymentRow("تاریخ شروع", policy.begin)
            PaymentRow("تاریخ پایان", policy.end)
            PaymentRow("نام بیمه شده", PaymentFixture.insuredName)
            PaymentRow("کد ملی بیمه شده", PaymentFixture.insuredNationalCode)
            PaymentRow("شماره الحاقیه", persianNumber(policy.addendum.toLong()))
        }
    }
    policy.installments.forEachIndexed { index, item ->
        val enabled = PaymentRules.selectable(item)
        OutlinedCard(
            Modifier.fillMaxWidth().testTag("installment-${item.number}").selectable(
                index == selected,
                enabled,
                Role.RadioButton,
            ) {
                error = null
                onSelect(index)
            }
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(index == selected, null, enabled = enabled)
                Column(Modifier.weight(1f)) {
                    PaymentRow("شماره قسط", persianNumber(item.number.toLong()))
                    PaymentRow("سال قسط", persianNumber(item.year.toLong()).replace("٬", ""))
                    PaymentRow("تاریخ سررسید", item.dueDate)
                    PaymentRow("مبلغ قسط", "${persianNumber(item.amount)} ریال")
                    PaymentRow("مانده", "${persianNumber(item.remained)} ریال")
                    PaymentRow("شناسه پرداخت", item.paymentId)
                    PaymentRow("وضعیت", PaymentFixture.statusTitles.getValue(item.status))
                }
            }
        }
    }
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("insurance-error"),
        )
    }
    if (PaymentRules.payable(policy))
        Button(
            { if (selected == -1) error = PaymentRules.SELECT_INSTALLMENT else picker = true },
            Modifier.fillMaxWidth().testTag("insurance-pay"),
        ) {
            Text("پرداخت")
        }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("انتخاب حساب مبدا") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountFixture.accounts.forEach { account ->
                        TextButton(
                            {
                                picker = false
                                onSource(account.key)
                                onOpen(InsuranceDestinations.CONFIRM)
                            },
                            Modifier.testTag("insurance-source-${account.key}"),
                        ) {
                            Text("${account.type}\n${account.number}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ picker = false }) { Text("انصراف") } },
        )
    }
    BackHandler(picker) { picker = false }
}

@Composable
private fun PaymentReceipt(page: String, onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    Text(
        "رسید ساختگی مستقل برای بازبینی؛ حاصل پرداخت نیست.",
        modifier = Modifier.testTag("payment-receipt-disclosure"),
    )
    TransferValue("مبدا", "0000000000001")
    when (page) {
        BillPaymentDestinations.RECEIPT -> {
            TransferValue("به نام", "شرکت آب نمایشی")
            TransferValue("شناسه قبض", "9000000000001")
            TransferValue("شناسه واریز", "1250001")
            TransferValue("مبلغ (ریال)", persianNumber(1_250_000L))
        }
        InsuranceDestinations.RECEIPT -> {
            TransferValue("بابت", "پرداخت اقساط بیمه نامه")
            TransferValue("مبلغ (ریال)", persianNumber(2_000_000L))
        }
        else -> {
            TransferValue("بابت", "بازپرداخت کارت اعتباری")
            TransferValue("مبلغ (ریال)", persianNumber(50_000_000L))
        }
    }
    TransferValue("زمان", "۱۴۰۵/۰۸/۰۱ - ۱۰:۰۰")
    TransferValue("کد رهگیری ساختگی", "DEMO-PAYMENT-0001")
    Button(
        { onUnavailable("ذخیره و اشتراک‌گذاری رسید") },
        Modifier.fillMaxWidth().testTag("payment-receipt-share"),
    ) {
        Text("ذخیره و اشتراک‌گذاری")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
}

@Composable
internal fun PaymentSource(accountKey: String) {
    val account = AccountFixture.accounts.find { it.key == accountKey }
    TransferValue("مبدا", account?.let { "${it.type} · ${it.number}" } ?: "")
}

@Composable
internal fun PaymentActions(
    label: String,
    title: String,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Button({ onUnavailable(title) }, Modifier.fillMaxWidth().testTag("payment-submit")) {
        Text("$label — در دسترس نیست")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("payment-cancel")) { Text("انصراف") }
}

@Composable
internal fun PaymentRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(value, style = TextStyle(textDirection = TextDirection.Content))
    }
}

@Composable
internal fun PaymentInput(
    tag: String,
    label: String,
    value: String,
    error: String?,
    hint: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        textStyle = TextStyle(textDirection = TextDirection.Ltr),
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
