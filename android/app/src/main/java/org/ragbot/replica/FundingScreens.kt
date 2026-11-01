package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** FN-RECOVERY: increase-balance amount, local gateway stand-in, waiting and receipt previews. */
@Composable
internal fun FundingPage(
    page: String,
    scenario: String,
    draft: FundingDraft,
    onDraft: (FundingDraft) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onLeave: () -> Unit,
    onNotice: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val flow = FundingFlow.of(page)
    PageFrame(page, "افزایش موجودی", onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(FundingFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    flow.amount -> FundingAmount(flow, draft, onDraft, onOpen, onLeave)
                    flow.gateway -> {
                        TransferValue(
                            "مبلغ (ریال)",
                            persianNumber(FundingRules.amount(draft.amount)),
                        )
                        Text(
                            "درگاه پرداخت در این پیش‌نمایش بارگذاری نمی‌شود. یکی از بازگشت‌های نمونه را انتخاب کنید.",
                            Modifier.testTag("funding-gateway-disclosure"),
                        )
                        FundingOutcome.entries.forEach {
                            OutlinedButton(
                                {
                                    onDraft(draft.copy(outcome = it, pendingSeen = false))
                                    onOpen(flow.waiting)
                                },
                                Modifier.fillMaxWidth().testTag("funding-outcome-${it.name}"),
                            ) {
                                Text(it.title)
                            }
                        }
                        // Recovered: a gateway load error shows the connection message and goes up.
                        TextButton(
                            {
                                onNotice("لطفا اتصال اینترنت را بررسی و دوباره تلاش کنید.")
                                onBack()
                            },
                            Modifier.testTag("funding-gateway-error"),
                        ) {
                            Text("خطای بارگذاری درگاه نمونه")
                        }
                    }
                    flow.waiting -> {
                        Text("در انتظار", style = MaterialTheme.typography.titleMedium)
                        Text("این صفحه انجام یا تسویهٔ تراکنش را تایید نمی‌کند.", fontSize = 13.sp)
                        if (draft.pendingSeen)
                            Text(
                                "نتیجه هنوز آماده نیست؛ دوباره درخواست کنید.",
                                Modifier.testTag("funding-pending"),
                            )
                        // Deterministic local exit; the original asks again while the result is
                        // pending.
                        Button(
                            {
                                if (draft.outcome == FundingOutcome.PENDING && !draft.pendingSeen)
                                    onDraft(draft.copy(pendingSeen = true))
                                else onOpen(flow.receipt)
                            },
                            Modifier.fillMaxWidth().testTag("funding-fetch"),
                        ) {
                            Text("دریافت نتیجهٔ نمونه")
                        }
                    }
                    else -> FundingReceipt(flow, draft, onLeave, onUnavailable)
                }
            }
        }
    }
}

internal fun fundingTarget(flow: FundingFlow, target: String): String =
    if (flow.wallet)
        WalletFixture.wallets.find { it.number == target }?.let { "${it.title} · ${it.number}" }
            ?: ""
    else
        AccountFixture.accounts.find { it.key == target }?.let { "${it.type} · ${it.number}" } ?: ""

@Composable
private fun FundingAmount(
    flow: FundingFlow,
    draft: FundingDraft,
    onDraft: (FundingDraft) -> Unit,
    onOpen: (String) -> Unit,
    onLeave: () -> Unit,
) {
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val value = FundingRules.amount(draft.amount)
    fun set(amount: String) {
        error = null
        onDraft(draft.copy(amount = amount))
    }
    val title = if (flow.wallet) "کیف مقصد" else "حساب مقصد"
    if (flow.inFlow) {
        // The calling journey fixes the destination.
        TransferValue(title, fundingTarget(flow, draft.target))
    } else {
        val options =
            if (flow.wallet) WalletRules.active(WalletFixture.wallets).map { it.number }
            else AccountFixture.accounts.map { it.key }
        Text(if (flow.wallet) "انتخاب کیف مقصد" else "انتخاب حساب مقصد")
        OutlinedButton(
            {
                focus.clearFocus()
                picking = true
            },
            Modifier.fillMaxWidth().testTag("funding-target"),
        ) {
            Text(fundingTarget(flow, draft.target))
        }
        if (picking) {
            AlertDialog(
                onDismissRequest = { picking = false },
                title = { Text(if (flow.wallet) "انتخاب کیف مقصد" else "انتخاب حساب مقصد") },
                text = {
                    Column {
                        options.forEachIndexed { index, key ->
                            if (index == 1)
                                Text(
                                    if (flow.wallet) "کیف‌های دیگر" else "حساب‌های دیگر",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            val label = fundingTarget(flow, key)
                            TextButton(
                                {
                                    onDraft(draft.copy(target = key))
                                    picking = false
                                },
                                Modifier.testTag("funding-target-$key"),
                            ) {
                                Text(
                                    if (index == 0)
                                        (if (flow.wallet) "کیف پیش‌فرض" else "حساب پیش‌فرض") +
                                            " ( $label )"
                                    else label
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton({ picking = false }) { Text("انصراف") } },
            )
        }
    }
    Text("مبلغ افزایش موجودی را انتخاب نمایید.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FundingFixture.proposedAmounts.forEach { amount ->
            FilterChip(
                value == amount,
                { set(amount.toString()) },
                { Text(persianNumber(amount), fontSize = 12.sp) },
                Modifier.testTag("funding-proposed-$amount"),
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            { set(FundingRules.minus(value).toString()) },
            Modifier.testTag("funding-minus"),
        ) {
            Text("−", fontSize = 20.sp)
        }
        OutlinedTextField(
            draft.amount,
            { set(it) },
            Modifier.weight(1f).testTag("funding-amount"),
            placeholder = { Text("۰ ریال") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, Modifier.testTag("funding-error")) } },
            textStyle = TextStyle(textDirection = TextDirection.Ltr),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        TextButton({ set(FundingRules.plus(value).toString()) }, Modifier.testTag("funding-plus")) {
            Text("+", fontSize = 20.sp)
        }
    }
    Text(
        "افزایش موجودی تنها با کارت بانکی متعلق به صاحب حساب امکان‌پذیر است و سقف تراکنش روزانه " +
            "${persianNumber(FundingFixture.limits.last)} ریال می‌باشد.",
        fontSize = 12.sp,
    )
    Button(
        {
            error = FundingRules.error(value)
            if (error == null) onOpen(flow.gateway)
        },
        Modifier.fillMaxWidth().testTag("funding-confirm"),
    ) {
        Text("افزایش موجودی")
    }
    OutlinedButton(onLeave, Modifier.fillMaxWidth().testTag("funding-return")) { Text("بازگشت") }
}

@Composable
private fun FundingReceipt(
    flow: FundingFlow,
    draft: FundingDraft,
    onLeave: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val amount = persianNumber(FundingRules.amount(draft.amount))
    Text(
        "رسید ساختگی برای بازبینی؛ هیچ مبلغی شارژ نشده است.",
        Modifier.testTag("funding-receipt-disclosure"),
    )
    when (draft.outcome) {
        FundingOutcome.SUCCESS,
        FundingOutcome.PENDING ->
            Text(
                "فرایند افزایش موجودی با موفقیت انجام شد.",
                Modifier.testTag("funding-status"),
                color = ServiceGreen,
            )
        FundingOutcome.FAILED ->
            Text(
                "فرایند افزایش موجودی بامشکل مواجه شده است.",
                Modifier.testTag("funding-status"),
                color = MaterialTheme.colorScheme.error,
            )
        FundingOutcome.UNKNOWN ->
            Text("فرایند افزایش موجودی نامعلوم است.", Modifier.testTag("funding-status"))
    }
    val success = draft.outcome == FundingOutcome.SUCCESS || draft.outcome == FundingOutcome.PENDING
    if (!flow.inFlow) {
        // Recovered legacy receipt fields of S-217.
        TransferValue("مقصد", fundingTarget(flow, draft.target))
        TransferValue(
            "به نام",
            AccountFixture.accounts.find { it.key == draft.target }?.owner ?: "",
        )
        TransferValue("مبلغ (ریال)", amount)
        TransferValue("زمان", "۱۴۰۵/۰۸/۱۰ - ۱۰:۰۰")
        TransferValue("شماره مرجع", "DEMO-REF-0001")
        TransferValue("شماره پیگیری", FundingFixture.TRACKING)
        Button(
            { onUnavailable("اشتراک‌گذاری رسید") },
            Modifier.fillMaxWidth().testTag("funding-share"),
        ) {
            Text("اشتراک‌گذاری")
        }
        OutlinedButton(onLeave, Modifier.fillMaxWidth().testTag("funding-done")) { Text("بازگشت") }
    } else if (success) {
        Text("مبلغ $amount ریال شارژ شد")
        Text("برای ادامه فرایند دکمه ادامه را کلیک کنید.", fontSize = 13.sp)
        // The calling journey is not built, so continuing returns to where the flow started.
        Button(onLeave, Modifier.fillMaxWidth().testTag("funding-done")) { Text("ادامه فرایند") }
        OutlinedButton(onLeave, Modifier.fillMaxWidth().testTag("funding-cancel")) {
            Text("انصراف")
        }
    } else if (draft.outcome == FundingOutcome.FAILED) {
        Button(onLeave, Modifier.fillMaxWidth().testTag("funding-done")) { Text("بستن") }
    } else {
        Text("شماره پیگیری: ${FundingFixture.TRACKING}")
        Button(
            { onUnavailable("پیگیری نتیجهٔ افزایش موجودی") },
            Modifier.fillMaxWidth().testTag("funding-track"),
        ) {
            Text("پیگیری")
        }
        OutlinedButton(onLeave, Modifier.fillMaxWidth().testTag("funding-done")) { Text("بستن") }
    }
}
