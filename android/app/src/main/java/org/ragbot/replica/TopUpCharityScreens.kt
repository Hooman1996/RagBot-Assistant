package org.ragbot.replica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** MC-RECOVERY: mobile top-up and charity previews. Nothing is bought or donated. */
@Composable
internal fun TopUpCharityPage(
    page: String,
    scenario: String,
    topUpForm: String,
    topUp: TopUpDraft,
    onTopUp: (TopUpDraft) -> Unit,
    charity: CharityDraft,
    onCharity: (CharityDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            TopUpDestinations.ACCOUNT -> "خرید شارژ"
            TopUpDestinations.CARD -> "خرید شارژ با کارت"
            TopUpDestinations.WITHOUT_SOURCE -> "خرید شارژ بدون ورود"
            TopUpDestinations.CONFIRM -> "خرید شارژ"
            TopUpDestinations.CARD_INFO,
            TopUpDestinations.CARD_INFO_NO_LOGIN -> "اطلاعات کارت"
            TopUpDestinations.RECEIPT -> "رسید خرید شارژ"
            CharityDestinations.PICKER -> "خیریه"
            CharityDestinations.TRANSFER -> "کمک به خیریه"
            CharityDestinations.CONFIRM -> "انتقال وجه داخلی"
            else -> "رسید خیریه"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(TopUpCharityFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                val chosen = TopUpCharityFixture.charities.find { it.id == charity.charityId }
                when (page) {
                    in TopUpDestinations.forms ->
                        TopUpForm(page, topUp, onTopUp, onBack, onOpen, onUnavailable)
                    TopUpDestinations.CONFIRM -> {
                        TopUpSummary(topUpForm, topUp)
                        Button(
                            {
                                when (topUpForm) {
                                    // Recovered: account purchase asks for a signature or OTP.
                                    TopUpDestinations.ACCOUNT -> onUnavailable("خرید شارژ")
                                    TopUpDestinations.CARD -> onOpen(TopUpDestinations.CARD_INFO)
                                    else -> onOpen(TopUpDestinations.CARD_INFO_NO_LOGIN)
                                }
                            },
                            Modifier.fillMaxWidth().testTag("topup-accept"),
                        ) {
                            Text(
                                if (topUpForm == TopUpDestinations.ACCOUNT)
                                    "تایید و پرداخت — در دسترس نیست"
                                else "تایید"
                            )
                        }
                        OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("topup-edit")) {
                            Text("بازگشت و ویرایش")
                        }
                    }
                    TopUpDestinations.CARD_INFO,
                    TopUpDestinations.CARD_INFO_NO_LOGIN -> {
                        TopUpSummary(topUpForm, topUp)
                        CardPaymentInfo(
                            page == TopUpDestinations.CARD_INFO,
                            "خرید شارژ",
                            onBack,
                            onUnavailable,
                        )
                    }
                    CharityDestinations.PICKER -> {
                        Text("خیریه مورد نظر خود را برای حمایت مالی انتخاب کنید.")
                        TopUpCharityFixture.charities.forEach {
                            OutlinedCard(
                                Modifier.fillMaxWidth().testTag("charity-${it.id}").clickable(
                                    role = Role.Button
                                ) {
                                    onCharity(charity.copy(charityId = it.id))
                                    onOpen(CharityDestinations.TRANSFER)
                                }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(it.name)
                                    Text(it.desc, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    CharityDestinations.TRANSFER ->
                        if (chosen != null) CharityForm(chosen, charity, onCharity, onBack, onOpen)
                    CharityDestinations.CONFIRM ->
                        if (chosen != null) {
                            PaymentSource(charity.sourceId)
                            TransferValue("مقصد", "${chosen.name} · ${chosen.accountNo}")
                            TransferValue(
                                "مبلغ (ریال)",
                                persianNumber(latinDigits(charity.amount).toLongOrNull() ?: 0L),
                            )
                            if (charity.paymentId.isNotEmpty())
                                TransferValue("شناسه برداشت", charity.paymentId)
                            if (charity.description.isNotEmpty())
                                TransferValue("شرح مبدا", charity.description)
                            TextButton(
                                { onUnavailable("افزودن یادداشت و پیوست") },
                                Modifier.testTag("charity-attachment"),
                            ) {
                                Text("افزودن یادداشت و پیوست تراکنش")
                            }
                            PaymentActions("تایید", "کمک به خیریه", onBack, onUnavailable)
                        }
                    else -> TopUpCharityReceipt(page, onBack, onUnavailable)
                }
            }
        }
    }
}

@Composable
private fun TopUpForm(
    page: String,
    draft: TopUpDraft,
    onChange: (TopUpDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val focus = LocalFocusManager.current
    val sources =
        if (page == TopUpDestinations.ACCOUNT)
            AccountFixture.accounts.map { it.key to "${it.type} · ${it.number}" }
        else CardFixture.of(CardKind.DEBIT).map { it.key to it.number }
    fun change(key: String, next: TopUpDraft) {
        // An edited field drops its stale error until the next Continue.
        errors = errors - key
        onChange(next)
    }
    if (page == TopUpDestinations.WITHOUT_SOURCE) {
        PaymentInput("topup-source", "کارت مبدا", draft.source, errors["source"]) {
            if (it.length <= 16) change("source", draft.copy(source = it))
        }
    } else {
        Text(if (page == TopUpDestinations.ACCOUNT) "حساب مبدا" else "کارت مبدا")
        OutlinedButton(
            {
                focus.clearFocus()
                picker = true
            },
            Modifier.fillMaxWidth().testTag("topup-source"),
        ) {
            Text(sources.find { it.first == draft.source }?.second ?: "انتخاب مبدا")
        }
    }
    PaymentInput("topup-phone", "تلفن همراه", draft.phone, errors["phone"], "نمونه: 09123456789") {
        if (it.length <= 11) {
            errors = errors - "operator"
            change(
                "phone",
                draft.copy(phone = it, operator = TopUpRules.detect(it, draft.operator)),
            )
        }
    }
    Text("اپراتور")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TopUpCharityFixture.operators.forEach {
            FilterChip(
                draft.operator == it.operator,
                { change("operator", draft.copy(operator = it.operator)) },
                { Text(it.title) },
                Modifier.testTag("topup-operator-${it.operator}"),
            )
        }
    }
    errors["operator"]?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("topup-error"),
        )
    }
    val config = TopUpRules.config(draft.operator)
    PaymentInput(
        "topup-amount",
        "مبلغ شارژ (ریال)",
        draft.amount,
        errors["amount"],
        config?.let {
            "مبلغ را ضریبی از ${persianNumber(it.multiplier)} ریال وارد نمایید و یا از لیست زیر انتخاب کنید."
        },
    ) {
        change("amount", draft.copy(amount = it))
    }
    config?.defaults?.forEach {
        TextButton(
            {
                focus.clearFocus()
                change("amount", draft.copy(amount = it.toString()))
            },
            Modifier.testTag("topup-amount-$it"),
        ) {
            Text("${persianNumber(it)} ریال · ${persianNumber(it / 10)} تومان")
        }
    }
    Text(
        "حدود مبلغ و پیش‌شماره‌ها را سرویس اعلام می‌کند؛ در این نمونه ساختگی‌اند.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        {
            focus.clearFocus()
            errors = TopUpRules.errors(page, draft)
            if (errors.isEmpty()) onOpen(TopUpDestinations.CONFIRM)
        },
        Modifier.fillMaxWidth().testTag("topup-continue"),
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
                                change("source", draft.copy(source = key))
                                picker = false
                            },
                            Modifier.testTag("topup-source-$key"),
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
private fun TopUpSummary(form: String, draft: TopUpDraft) {
    when (form) {
        TopUpDestinations.ACCOUNT -> PaymentSource(draft.source)
        TopUpDestinations.CARD ->
            TransferValue("مبدا", CardFixture.cards.find { it.key == draft.source }?.number ?: "")
        else -> TransferValue("مبدا", latinDigits(draft.source))
    }
    TransferValue(
        "مقصد",
        "${TopUpRules.config(draft.operator)?.title} · ${latinDigits(draft.phone)}",
    )
    latinDigits(draft.amount).toLongOrNull()?.let {
        TransferValue("مبلغ (ریال)", persianNumber(it))
    }
}

@Composable
private fun CharityForm(
    chosen: Charity,
    draft: CharityDraft,
    onChange: (CharityDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable {
        mutableStateOf(draft.paymentId.isNotEmpty() || draft.description.isNotEmpty())
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    TransferValue("مقصد", chosen.name)
    TextButton(onBack, Modifier.testTag("charity-change")) { Text("تغییر خیریه") }
    Text("حساب مبدا")
    OutlinedButton(
        {
            focus.clearFocus()
            picker = true
        },
        Modifier.fillMaxWidth().testTag("charity-source"),
    ) {
        Text(
            AccountFixture.accounts
                .find { it.key == draft.sourceId }
                ?.let { "${it.type} · ${it.number}" } ?: "انتخاب حساب مبدا"
        )
    }
    PaymentInput("charity-amount", "مبلغ (ریال)", draft.amount, error) {
        error = null
        onChange(draft.copy(amount = it))
    }
    TextButton(
        {
            focus.clearFocus()
            extra = !extra
        },
        Modifier.testTag("charity-extra"),
    ) {
        Text("افزودن شناسه و شرح تراکنش")
    }
    if (extra) {
        // Designed for mock: limits follow FR-08's optional transfer fields.
        TransferInput(
            "charity-payment-id",
            "شناسه برداشت (اختیاری)",
            draft.paymentId,
            {
                if (latinDigits(it).all { ch -> ch in '0'..'9' } && it.length <= 17)
                    onChange(draft.copy(paymentId = it))
            },
            numeric = true,
        )
        TransferInput(
            "charity-description",
            "شرح مبدا (اختیاری)",
            draft.description,
            { if (it.length <= 50) onChange(draft.copy(description = it)) },
        )
    }
    Button(
        {
            focus.clearFocus()
            error = CharityRules.amountError(draft.amount)
            if (error == null) onOpen(CharityDestinations.CONFIRM)
        },
        Modifier.fillMaxWidth().testTag("charity-continue"),
    ) {
        Text("ادامه")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("انتخاب حساب مبدا") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountFixture.accounts.forEach { account ->
                        TextButton(
                            {
                                onChange(draft.copy(sourceId = account.key))
                                picker = false
                            },
                            Modifier.testTag("charity-source-${account.key}"),
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
private fun TopUpCharityReceipt(page: String, onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    Text(
        "رسید ساختگی مستقل برای بازبینی؛ حاصل پرداخت نیست.",
        modifier = Modifier.testTag("payment-receipt-disclosure"),
    )
    TransferValue("مبدا", "0000000000001")
    if (page == TopUpDestinations.RECEIPT) {
        TransferValue("تلفن همراه", "09000000000")
        TransferValue("اپراتور", "همراه اول")
        TransferValue("مبلغ (ریال)", persianNumber(100_000L))
    } else {
        TransferValue("خیریه", "خیریهٔ نمایشی آموزش")
        TransferValue("مقصد", "0000000000009")
        TransferValue("مبلغ (ریال)", persianNumber(500_000L))
    }
    TransferValue("زمان", "۱۴۰۵/۰۸/۰۱ - ۱۰:۰۰")
    TransferValue("کد رهگیری ساختگی", "DEMO-PAYMENT-0002")
    Button(
        { onUnavailable("ذخیره و اشتراک‌گذاری رسید") },
        Modifier.fillMaxWidth().testTag("payment-receipt-share"),
    ) {
        Text("ذخیره و اشتراک‌گذاری")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
}
