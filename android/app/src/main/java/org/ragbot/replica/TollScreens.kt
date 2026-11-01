package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** TL-RECOVERY: toll inquiry, result, confirmation and receipt previews. */
@Composable
internal fun TollPage(
    page: String,
    scenario: String,
    draft: TollDraft,
    onDraft: (TollDraft) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val service = if (page == TollDestinations.RECEIPT) null else TollService.of(page)
    PageFrame(page, service?.title ?: "رسید عوارض", onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(TollFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when {
                    service == null -> TollReceipt(onBack, onUnavailable)
                    page == service.inquiry -> TollInquiry(draft, onDraft, onOpen)
                    page == service.result -> TollResult(draft, onDraft, onOpen)
                    else -> TollConfirm(draft, onDraft, onBack, onUnavailable)
                }
            }
        }
    }
}

@Composable
private fun TollPlateInput(draft: TollDraft, onChange: (TollDraft) -> Unit) {
    Text("شماره پلاک")
    // Recovered: the pollution inquiry takes automobile plates only.
    if (draft.service != TollService.POLLUTION)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true to "خودرو", false to "موتورسیکلت").forEach { (automobile, label) ->
                FilterChip(
                    draft.automobile == automobile,
                    {
                        onChange(
                            draft.copy(automobile = automobile, plate = listOf("", "", "", ""))
                        )
                    },
                    { Text(label) },
                    Modifier.testTag("toll-type-$automobile"),
                )
            }
        }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sizes = if (draft.automobile) listOf(2, 0, 3, 2) else listOf(3, 5)
            sizes.forEachIndexed { index, size ->
                if (size == 0) {
                    var picking by rememberSaveable { mutableStateOf(false) }
                    OutlinedButton({ picking = true }, Modifier.testTag("toll-letter")) {
                        Text(draft.plate[index].ifEmpty { "حرف" })
                    }
                    if (picking)
                        AlertDialog(
                            onDismissRequest = { picking = false },
                            title = { Text("انتخاب حرف پلاک") },
                            text = {
                                Column {
                                    TollFixture.letters.forEach { (_, letter) ->
                                        TextButton(
                                            {
                                                onChange(
                                                    draft.copy(
                                                        plate =
                                                            draft.plate.toMutableList().also {
                                                                it[index] = letter
                                                            }
                                                    )
                                                )
                                                picking = false
                                            },
                                            Modifier.testTag("toll-letter-$letter"),
                                        ) {
                                            Text(letter)
                                        }
                                    }
                                }
                            },
                            confirmButton = { TextButton({ picking = false }) { Text("انصراف") } },
                        )
                } else
                    OutlinedTextField(
                        draft.plate[index],
                        {
                            val digits = latinDigits(it).filter { c -> c in '0'..'9' }.take(size)
                            onChange(
                                draft.copy(
                                    plate =
                                        draft.plate.toMutableList().also { p -> p[index] = digits }
                                )
                            )
                        },
                        Modifier.width((size * 22 + 34).dp).testTag("toll-plate-$index"),
                        singleLine = true,
                    )
            }
        }
    }
}

@Composable
private fun TollInquiry(draft: TollDraft, onDraft: (TollDraft) -> Unit, onOpen: (String) -> Unit) {
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var fee by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf(false) }
    fun change(next: TollDraft) {
        errors = emptyMap()
        onDraft(next)
    }
    TollPlateInput(draft, ::change)
    errors["plate"]?.let {
        Text(it, Modifier.testTag("toll-plate-error"), color = MaterialTheme.colorScheme.error)
    }
    Text("مالکیت")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(true to "خودم", false to "دیگران").forEach { (mine, label) ->
            FilterChip(
                draft.mine == mine,
                { change(draft.copy(mine = mine)) },
                { Text(label) },
                Modifier.testTag("toll-owner-$mine"),
            )
        }
    }
    if (!draft.mine) {
        PaymentInput("toll-code", "کد ملی", draft.code, errors["code"]) {
            if (it.length <= 10) change(draft.copy(code = it))
        }
        PaymentInput("toll-phone", "تلفن همراه", draft.phone, errors["phone"], "09121234567") {
            if (it.length <= 11) change(draft.copy(phone = it))
        }
    }
    if (draft.service == TollService.ANNUAL)
        PaymentInput("toll-vin", "شماره VIN خودرو", draft.vin, errors["vin"], "IRFC*********6903") {
            change(draft.copy(vin = it))
        }
    Button(
        {
            errors = TollRules.errors(draft)
            if (errors.isEmpty()) fee = true
        },
        Modifier.fillMaxWidth().testTag("toll-inquiry"),
    ) {
        Text("استعلام")
    }
    if (fee) {
        // Recovered: the inquiry fee is taken from an account, or a wallet for pollution.
        val pollution = draft.service == TollService.POLLUTION
        AlertDialog(
            onDismissRequest = { fee = false },
            title = {
                Text(if (pollution) "انتخاب کیف پول پرداخت کارمزد" else "انتخاب حساب پرداخت کارمزد")
            },
            text = {
                Column {
                    Text("هزینه کارمزد استعلام: ${persianNumber(TollFixture.INQUIRY_FEE)} ریال")
                    val sources =
                        if (pollution)
                            WalletRules.active(WalletFixture.wallets).map {
                                it.number to "${it.title} · ${it.number}"
                            }
                        else AccountFixture.accounts.map { it.key to chequeAccount(it.key) }
                    sources.forEach { (key, label) ->
                        TextButton(
                            {
                                fee = false
                                otp = true
                            },
                            Modifier.testTag("toll-fee-$key"),
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ fee = false }) { Text("انصراف") } },
        )
    }
    // The owner's code is checked locally and never stored.
    if (otp)
        OtpStep(
            {
                otp = false
                onDraft(draft.copy(selected = emptyList()))
                onOpen(draft.service.result)
            },
            { otp = false },
        )
}

@Composable
private fun TollResult(draft: TollDraft, onDraft: (TollDraft) -> Unit, onOpen: (String) -> Unit) {
    var error by rememberSaveable { mutableStateOf(false) }
    PaymentRow("شماره پلاک:", TollRules.plateNumber(draft))
    if (draft.service == TollService.POLLUTION) {
        PaymentRow("سهمیه مجاز فصل جاری", persianNumber(TollFixture.QUOTA_ALLOWED.toLong()))
        PaymentRow("سهمیه باقیمانده فصل جاری", persianNumber(TollFixture.QUOTA_REMAINING.toLong()))
        TollFixture.consumed.forEach { (year, season, count) ->
            PaymentRow(
                "فصل ${persianNumber(season.toLong())} سال ${persianNumber(year.toLong())}",
                "${persianNumber(count.toLong())} تردد",
            )
        }
        PaymentRow("تاریخ آخرین صورتحساب", "۱۴۰۵/۰۷/۳۰")
        return
    }
    val items = TollRules.items(draft)
    if (items.isEmpty()) {
        Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("toll-no-dues"))
        return
    }
    if (draft.service == TollService.ANNUAL) {
        PaymentRow("سیستم خودرو", "سواری نمایشی")
        PaymentRow("کاربری", "شخصی")
        PaymentRow("شماره VIN:", "IRFC*********0001")
        items.forEach {
            OutlinedCard(Modifier.fillMaxWidth().testTag("toll-item-${it.id}")) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.title)
                    PaymentRow("مبلغ", "${persianNumber(it.price)} ریال")
                    PaymentRow("جریمه:", "${persianNumber(it.penalty)} ریال")
                    PaymentRow("استهلاک خودرو:", "${persianNumber(it.frazzle)} ریال")
                }
            }
        }
        PaymentRow("کارمزد", "${persianNumber(TollFixture.COMMISSION)} ریال")
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                draft.selected.size == items.size,
                { all ->
                    onDraft(draft.copy(selected = if (all) items.map { it.id } else emptyList()))
                },
                Modifier.testTag("toll-select-all"),
            )
            Text("انتخاب همه")
        }
        items.forEach { item ->
            Row(
                Modifier.fillMaxWidth().testTag("toll-item-${item.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    item.id in draft.selected,
                    { on ->
                        error = false
                        onDraft(
                            draft.copy(
                                selected =
                                    if (on) draft.selected + item.id else draft.selected - item.id
                            )
                        )
                    },
                    Modifier.testTag("toll-check-${item.id}"),
                )
                Text(item.title, Modifier.weight(1f))
                Text("${persianNumber(item.price)} ریال")
            }
        }
    }
    Text("مبلغ کل: ${persianNumber(TollRules.total(draft))} ریال", Modifier.testTag("toll-total"))
    if (error)
        Text(
            TollRules.SELECT_ITEM,
            Modifier.testTag("toll-select-error"),
            color = MaterialTheme.colorScheme.error,
        )
    Button(
        {
            error = draft.service != TollService.ANNUAL && draft.selected.isEmpty()
            if (!error) onOpen(checkNotNull(draft.service.confirm))
        },
        Modifier.fillMaxWidth().testTag("toll-pay"),
    ) {
        Text("پرداخت")
    }
}

@Composable
private fun TollConfirm(
    draft: TollDraft,
    onDraft: (TollDraft) -> Unit,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    ChequeAccountPicker(draft.account) { onDraft(draft.copy(account = it)) }
    PaymentRow("پلاک", TollRules.plateNumber(draft))
    PaymentRow("بابت", draft.service.title)
    TollRules.items(draft)
        .filter { draft.service == TollService.ANNUAL || it.id in draft.selected }
        .forEach {
            PaymentRow(it.title, "${persianNumber(it.price + it.penalty + it.frazzle)} ریال")
        }
    Text(
        "مبلغ کل: ${persianNumber(TollRules.total(draft))} ریال",
        Modifier.testTag("toll-confirm-total"),
    )
    // Recovered: payment needs the user's OTP and signature; nothing is paid here.
    PaymentActions("تایید و پرداخت", "پرداخت ${draft.service.title}", onBack, onUnavailable)
}

@Composable
private fun TollReceipt(onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    Text(
        "رسید ساختگی مستقل برای بازبینی؛ حاصل پرداخت نیست.",
        Modifier.testTag("toll-receipt-disclosure"),
    )
    TransferValue("نوع تراکنش", "پرداخت عوارض شهرداری | موفق")
    TransferValue("مبلغ (ریال)", persianNumber(205_000L))
    TransferValue("پلاک", TollFixture.DUES_PLATE)
    TransferValue("زمان", "۱۴۰۵/۰۸/۱۰ - ۱۰:۰۰")
    TransferValue("شماره پیگیری", "DEMO-TOLL-0001")
    Button({ onUnavailable("پیگیری تراکنش") }, Modifier.fillMaxWidth().testTag("toll-track")) {
        Text("پیگیری")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
}
