package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** WL-RECOVERY: read-only wallet previews. No wallet operation is sent. */
@Composable
internal fun WalletPage(
    page: String,
    scenario: String,
    walletNumber: String,
    rowKey: String,
    onWallet: (String) -> Unit,
    onRow: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
    funding: FundingDraft? = null,
) {
    val title =
        when (page) {
            WalletOverviewDestinations.MAIN -> "کیف پول"
            WalletOverviewDestinations.HISTORY_MENU -> "سوابق تراکنش‌ها"
            WalletOverviewDestinations.QR -> "دریافت با بارکد"
            WalletOverviewDestinations.STATEMENT -> "صورتحساب کیف"
            WalletOverviewDestinations.STATEMENT_DETAIL -> "جزئیات"
            else -> "رسید کیف پول"
        }
    val wallet =
        WalletFixture.wallets.find { it.number == walletNumber } ?: WalletFixture.wallets.first()
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else if (page == WalletOverviewDestinations.STATEMENT) {
            WalletStatementList(wallet, onWallet, onRow, onUnavailable)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(WalletFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    WalletOverviewDestinations.MAIN -> WalletMain(onOpen, onUnavailable)
                    WalletOverviewDestinations.HISTORY_MENU ->
                        WalletFixture.history.forEach { WalletMenuRow(it, onOpen, onUnavailable) }
                    WalletOverviewDestinations.QR -> WalletQr(wallet, onWallet, onUnavailable)
                    WalletOverviewDestinations.STATEMENT_DETAIL -> {
                        val row = WalletFixture.statement(wallet).first { it.key == rowKey }
                        TransferValue("کیف", wallet.number)
                        WalletStatementCard(row)
                        WalletRules.detailFields(row).forEach { (label, value) ->
                            TransferValue(label, value)
                        }
                    }
                    else -> WalletReceipt(funding, onBack, onUnavailable)
                }
            }
        }
    }
}

@Composable
private fun WalletMain(onOpen: (String) -> Unit, onUnavailable: (String) -> Unit) {
    var shownBalances by rememberSaveable { mutableStateOf(listOf<String>()) }
    var choosingCharge by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WalletFixture.actions.forEach {
            OutlinedButton(
                { it.destination?.let(onOpen) ?: run { choosingCharge = true } },
                Modifier.weight(1f).testTag("wallet-action-${it.key}"),
            ) {
                Text(it.title, fontSize = 12.sp)
            }
        }
    }
    // Recovered: the charge item opens a sheet of its children (account charge or gateway top-up).
    if (choosingCharge) {
        AlertDialog(
            onDismissRequest = { choosingCharge = false },
            title = { Text("انتخاب روش پرداخت") },
            text = {
                Column {
                    WalletOperationFixture.chargeTypes.forEach {
                        TextButton(
                            {
                                choosingCharge = false
                                it.destination?.let(onOpen) ?: onUnavailable(it.title)
                            },
                            Modifier.testTag("wallet-charge-${it.key}"),
                        ) {
                            Text(it.title)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ choosingCharge = false }) { Text("انصراف") } },
        )
    }
    WalletRules.shown(WalletFixture.wallets).forEach { wallet ->
        val shown = wallet.number in shownBalances
        Surface(
            Modifier.fillMaxWidth().testTag("wallet-${wallet.number}"),
            shape = RoundedCornerShape(25.dp),
            color = if (wallet.individual) Color(0xFF145D32) else Color(0xFF2B4C7E),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(wallet.title, color = Color.White)
                Text(wallet.number, color = Color.White)
                Text(wallet.owner, color = Color.White, fontSize = 13.sp)
                Text(
                    "${wallet.holderType}/ ${wallet.statusDescription}",
                    color = Color.White,
                    fontSize = 13.sp,
                )
                if (shown) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${persianNumber(wallet.balance)} ریال",
                            Modifier.weight(1f).testTag("wallet-balance-${wallet.number}"),
                            color = Color.White,
                        )
                        TextButton(
                            { shownBalances = shownBalances - wallet.number },
                            Modifier.testTag("wallet-hide-${wallet.number}"),
                        ) {
                            Text("عدم نمایش موجودی", color = Color.White)
                        }
                    }
                } else {
                    TextButton(
                        { shownBalances = shownBalances + wallet.number },
                        Modifier.testTag("wallet-show-${wallet.number}"),
                    ) {
                        Text("مشاهده موجودی", color = Color.White)
                    }
                }
            }
        }
    }
    WalletFixture.menu.forEach { WalletMenuRow(it, onOpen, onUnavailable) }
}

@Composable
private fun WalletMenuRow(
    item: WalletMenuItem,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth().testTag("wallet-menu-${item.key}").clickable(role = Role.Button) {
            item.destination?.let(onOpen) ?: onUnavailable(item.title)
        }
    ) {
        Text(item.title, Modifier.padding(16.dp))
    }
}

@Composable
private fun WalletPicker(wallet: PreviewWallet, onWallet: (String) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    OutlinedButton({ picking = true }, Modifier.fillMaxWidth().testTag("wallet-picker")) {
        Text("${wallet.title} · ${wallet.number}")
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("انتخاب کیف") },
            text = {
                Column {
                    WalletRules.active(WalletFixture.wallets).forEach {
                        TextButton(
                            {
                                onWallet(it.number)
                                picking = false
                            },
                            Modifier.testTag("wallet-pick-${it.number}"),
                        ) {
                            Text("${it.title} · ${it.number}")
                        }
                    }
                }
            },
            confirmButton = { TextButton({ picking = false }) { Text("بستن") } },
        )
    }
}

@Composable
private fun WalletQr(
    wallet: PreviewWallet,
    onWallet: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf<String?>(null) }
    var entering by rememberSaveable { mutableStateOf(false) }
    var copied by rememberSaveable { mutableStateOf(false) }
    WalletPicker(wallet, onWallet)
    TransferValue("صاحب", wallet.owner)
    TransferValue("نام کیف", wallet.title)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("شناسه: ${wallet.number}", Modifier.weight(1f))
        TextButton(
            {
                copyPreview(context, wallet.number)
                copied = true
            },
            Modifier.testTag("wallet-copy"),
        ) {
            Text("کپی")
        }
    }
    amount?.let { TransferValue("مبلغ (ریال)", persianNumber(it.toLong())) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("محتوای محلی بارکد", style = MaterialTheme.typography.labelMedium)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    WalletRules.qrContent(wallet.number, amount),
                    Modifier.fillMaxWidth().testTag("wallet-qr-content"),
                    // The payload keeps Latin digits; the fa locale would show Persian ones.
                    style = TextStyle(fontSize = 12.sp, localeList = LocaleList("en")),
                )
            }
            Text(
                "تصویر بارکد در این پیش‌نمایش ساخته نمی‌شود و این محتوا هیچ پرداختی آغاز نمی‌کند.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (amount == null)
        OutlinedButton({ entering = true }, Modifier.fillMaxWidth().testTag("wallet-add-amount")) {
            Text("افزودن مبلغ به بارکد")
        }
    else
        OutlinedButton({ amount = null }, Modifier.fillMaxWidth().testTag("wallet-remove-amount")) {
            Text("حذف مبلغ از بارکد")
        }
    Button({ onUnavailable("اشتراک‌گذاری") }, Modifier.fillMaxWidth().testTag("wallet-share")) {
        Text("اشتراک‌گذاری")
    }
    if (entering) {
        var value by rememberSaveable { mutableStateOf("") }
        val digits = latinDigits(value)
        AlertDialog(
            onDismissRequest = { entering = false },
            title = { Text("افزودن مبلغ به بارکد") },
            text = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value,
                        { value = it },
                        Modifier.testTag("wallet-amount"),
                        label = { Text("مبلغ (ریال)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        amount = digits
                        entering = false
                    },
                    Modifier.testTag("wallet-amount-accept"),
                    enabled = digits.isNotEmpty() && digits.all { it in '0'..'9' },
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ entering = false }) { Text("انصراف") } },
        )
    }
    if (copied) {
        AlertDialog(
            onDismissRequest = { copied = false },
            text = { Text("شناسه با موفقیت کپی شد.") },
            confirmButton = { TextButton({ copied = false }) { Text("بستن") } },
        )
    }
}

@Composable
private fun WalletStatementList(
    wallet: PreviewWallet,
    onWallet: (String) -> Unit,
    onRow: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var start by rememberSaveable { mutableStateOf(dateDaysAgo(29)) }
    var end by rememberSaveable { mutableStateOf(todayPersian()) }
    var appliedStart by rememberSaveable { mutableStateOf(start) }
    var appliedEnd by rememberSaveable { mutableStateOf(end) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var pages by rememberSaveable { mutableIntStateOf(1) }
    val range =
        checkNotNull(persianDateMillis(appliedStart))..checkNotNull(persianDateMillis(appliedEnd))
    val rows =
        remember(wallet.number) { WalletFixture.statement(wallet) }
            .filter { checkNotNull(persianDateMillis(it.date)) in range }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WalletPicker(wallet) {
            onWallet(it)
            pages = 1
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersianDateField(start, { start = it }, "از تاریخ", "wallet-start", Modifier.weight(1f))
            Text("تا", Modifier.padding(horizontal = 4.dp))
            PersianDateField(end, { end = it }, "تا تاریخ", "wallet-end", Modifier.weight(1f))
        }
        Button(
            {
                error = bankStatementDateError(start, end)
                if (error == null) {
                    appliedStart = start
                    appliedEnd = end
                    pages = 1
                }
            },
            Modifier.fillMaxWidth().testTag("wallet-apply"),
        ) {
            Text("اعمال بازه زمانی")
        }
        error?.let {
            Text(
                it,
                Modifier.testTag("wallet-date-error"),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
        OutlinedButton(
            { onUnavailable("دریافت فایل صورتحساب") },
            Modifier.fillMaxWidth().testTag("wallet-export"),
        ) {
            Text("دریافت فایل")
        }
        if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.")
        rows.take(pages * WalletRules.PAGE_SIZE).forEach { row ->
            WalletStatementCard(
                row,
                Modifier.testTag(row.key).clickable(role = Role.Button) { onRow(row.key) },
            )
        }
        if (pages * WalletRules.PAGE_SIZE < rows.size)
            TextButton({ pages++ }, Modifier.testTag("wallet-more")) { Text("نمایش بیشتر") }
    }
}

@Composable
private fun WalletStatementCard(row: WalletStatementRow, modifier: Modifier = Modifier) {
    val color = if (row.credit) ServiceGreen else Color(0xFFCB3434)
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.5.dp,
    ) {
        Row {
            Box(Modifier.width(4.dp).height(120.dp).background(color))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "${if (row.credit) "واریز" else "برداشت"}  ${persianNumber(row.amount)} ریال",
                    color = color,
                )
                Text("نوع تراکنش: ${row.typeDescription}", fontSize = 13.sp)
                Text("زمان: ${row.time} - ${row.date}", fontSize = 13.sp)
                Text("کد پیگیری: ${row.traceId}", fontSize = 13.sp)
                Text("مانده: ${persianNumber(row.balance)} ریال", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WalletReceipt(
    funding: FundingDraft?,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Text(
        "رسید ساختگی مستقل برای بازبینی؛ حاصل تراکنش نیست.",
        modifier = Modifier.testTag("wallet-receipt-disclosure"),
    )
    if (funding != null) {
        // FN-RECOVERY: the standalone wallet top-up hands its result to this receipt.
        val status =
            when (funding.outcome) {
                FundingOutcome.FAILED -> "ناموفق"
                FundingOutcome.UNKNOWN -> "نامعلوم"
                else -> "موفق"
            }
        TransferValue("نوع تراکنش", "افزایش موجودی | $status")
        TransferValue("مبلغ (ریال)", persianNumber(FundingRules.amount(funding.amount)))
        TransferValue("کیف مقصد", funding.target)
    } else {
        TransferValue("نوع تراکنش", "انتقال کیف به کیف | موفق")
        TransferValue("مبلغ (ریال)", persianNumber(400_000L))
        TransferValue("کیف مبدا", "0000000000101")
        TransferValue("کیف مقصد", "0000000000201")
        TransferValue("نام مقصد", "طرف نمایشی")
    }
    TransferValue("زمان", "۱۴۰۵/۰۸/۰۱ - ۱۰:۰۰")
    TransferValue("شماره پیگیری ساختگی", "DEMO-WALLET-0001")
    Button(
        { onUnavailable("اشتراک‌گذاری رسید") },
        Modifier.fillMaxWidth().testTag("wallet-receipt-share"),
    ) {
        Text("اشتراک‌گذاری")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
}
