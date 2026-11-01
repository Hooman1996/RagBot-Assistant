package org.ragbot.replica

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
import androidx.compose.ui.unit.sp

/**
 * CQ-RECOVERY: cheque menu, books, tracking, report, chequebook request and card board previews.
 */
@Composable
internal fun ChequePage(
    page: String,
    scenario: String,
    accountKey: String,
    bookRef: String,
    issue: ChequeIssueDraft,
    report: ChequeReportFilter,
    taskId: String,
    onAccount: (String) -> Unit,
    onBook: (String) -> Unit,
    onIssue: (ChequeIssueDraft) -> Unit,
    onReport: (ChequeReportFilter) -> Unit,
    onTask: (String) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            ChequeDestinations.MENU -> "چک"
            ChequeDestinations.BOOKS -> "دسته چک‌ها"
            ChequeDestinations.CHEQUES -> "برگه‌های دسته چک"
            ChequeDestinations.TRACK_MY -> "پیگیری چک‌های من"
            ChequeDestinations.TRACK_TRANSFERRED -> "پیگیری چک‌های واگذار شده"
            ChequeDestinations.REPORT_FILTER,
            ChequeDestinations.REPORT -> "گزارش چک‌های واگذار شده"
            ChequeDestinations.ISSUE,
            ChequeDestinations.ISSUE_CONFIRM -> issue.media.request
            ChequeDestinations.ISSUANCE -> "پیگیری درخواست دسته چک"
            ChequeDestinations.CARDBOARD -> "کارتابل"
            else -> "جزئیات درخواست"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(ChequeFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    ChequeDestinations.MENU ->
                        ChequeFixture.menu.forEach { item ->
                            OutlinedCard(
                                Modifier.fillMaxWidth()
                                    .testTag("cheque-menu-${item.key}")
                                    .clickable(role = Role.Button) {
                                        val target = item.destination
                                        if (target == null) onUnavailable(item.title)
                                        else {
                                            // Recovered: the issuance keys pass their cheque media.
                                            if (target == ChequeDestinations.ISSUE)
                                                onIssue(
                                                    ChequeIssueDraft(
                                                        if ("DIGITAL" in item.key)
                                                            ChequeMedia.DIGITAL
                                                        else ChequeMedia.PAPER
                                                    )
                                                )
                                            // Recovered: each report key passes its cheque type.
                                            if (target == ChequeDestinations.REPORT_FILTER)
                                                onReport(
                                                    ChequeReportFilter(
                                                        if ("KARAFARIN" in item.key) "INTERNAL"
                                                        else "OTHER",
                                                        // Designed for mock default range.
                                                        from = dateDaysAgo(29),
                                                        to = todayPersian(),
                                                    )
                                                )
                                            onOpen(target)
                                        }
                                    }
                            ) {
                                Text(item.title, Modifier.padding(16.dp))
                            }
                        }
                    ChequeDestinations.BOOKS -> {
                        ChequeAccountPicker(accountKey, onAccount)
                        val books = ChequeFixture.books.filter { it.accountKey == accountKey }
                        if (books.isEmpty())
                            Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("cheque-empty"))
                        books.forEach { book ->
                            OutlinedCard(
                                Modifier.fillMaxWidth()
                                    .testTag("cheque-book-${book.reference}")
                                    .clickable(role = Role.Button) {
                                        onBook(book.reference)
                                        onOpen(ChequeDestinations.CHEQUES)
                                    }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "${book.docType} · ${persianNumber(book.pageCount.toLong())} برگی"
                                    )
                                    Text("${book.startNo} تا ${book.endNo}", fontSize = 13.sp)
                                    Text("صدور: ${book.issueDate}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    ChequeDestinations.CHEQUES ->
                        ChequeFixture.books.find { it.reference == bookRef }?.let { ChequeList(it) }
                    ChequeDestinations.TRACK_MY,
                    ChequeDestinations.TRACK_TRANSFERRED ->
                        ChequeTrack(accountKey, onAccount, onUnavailable)
                    ChequeDestinations.REPORT_FILTER ->
                        ChequeReportForm(accountKey, report, onAccount, onReport, onOpen)
                    ChequeDestinations.REPORT -> ChequeReport(accountKey, report)
                    ChequeDestinations.ISSUE -> ChequeIssue(issue, onIssue, onOpen, onBack)
                    ChequeDestinations.ISSUE_CONFIRM -> {
                        TransferValue("شماره حساب:", chequeAccount(issue.accountKey))
                        TransferValue("تعداد برگ:", "${issue.pageCount} برگ")
                        TransferValue("رسانه چک:", issue.media.title)
                        TransferValue("کارمزد درخواست:", "${persianNumber(ChequeFixture.FEE)} ریال")
                        if (ChequeRules.cardBoard(issue.accountKey))
                            Text(
                                "این حساب مشترک است؛ درخواست برای تایید به کارتابل اعضا فرستاده می‌شود.",
                                Modifier.testTag("cheque-cardboard-note"),
                            )
                        // Recovered: confirming asks for an OTP; it stays with the user.
                        PaymentActions(
                            "تایید تراکنش",
                            "تایید تراکنش با رمز پویا",
                            onBack,
                            onUnavailable,
                        )
                    }
                    ChequeDestinations.ISSUANCE ->
                        ChequeFixture.issuance.forEach {
                            OutlinedCard(
                                Modifier.fillMaxWidth().testTag("cheque-issuance-${it.trace}")
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    PaymentRow("تاریخ درخواست", it.date)
                                    PaymentRow("شماره حساب", chequeAccount(it.accountKey))
                                    PaymentRow("رسانه چک", it.media.title)
                                    PaymentRow("تعداد برگ", it.pageCount)
                                    PaymentRow("کد رهگیری", it.trace)
                                    PaymentRow("وضعیت", it.status)
                                    it.rejectReason?.let { reason -> PaymentRow("دلیل رد", reason) }
                                }
                            }
                        }
                    ChequeDestinations.CARDBOARD -> ChequeCardBoard(onTask, onOpen)
                    else ->
                        ChequeFixture.tasks
                            .find { it.id == taskId }
                            ?.let { ChequeTaskDetail(page, it, onBack, onUnavailable) }
                }
            }
        }
    }
}

private fun chequeAccount(key: String) =
    AccountFixture.accounts.find { it.key == key }?.let { "${it.type} · ${it.number}" } ?: ""

@Composable
private fun ChequeAccountPicker(selected: String, onSelect: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Text("انتخاب حساب")
    OutlinedButton(
        {
            focus.clearFocus()
            open = true
        },
        Modifier.fillMaxWidth().testTag("cheque-account"),
    ) {
        Text(chequeAccount(selected).ifEmpty { "انتخاب حساب" })
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("انتخاب حساب") },
            text = {
                Column {
                    AccountFixture.accounts.forEach {
                        TextButton(
                            {
                                onSelect(it.key)
                                open = false
                            },
                            Modifier.testTag("cheque-account-${it.key}"),
                        ) {
                            Text(chequeAccount(it.key))
                        }
                    }
                }
            },
            confirmButton = { TextButton({ open = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun ChequeList(book: ChequeBook) {
    var query by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf(false) }
    Text("${book.docType} · ${book.startNo} تا ${book.endNo}")
    OutlinedButton({ summary = true }, Modifier.fillMaxWidth().testTag("cheque-summary")) {
        Text("خلاصه وضعیت دسته چک")
    }
    PaymentInput("cheque-search", "جستجوی شماره چک", query, null) { query = it }
    val rows = ChequeRules.search(ChequeFixture.cheques(book), query)
    if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("cheque-empty"))
    rows.forEach { ChequeCard(it) }
    if (summary) {
        AlertDialog(
            onDismissRequest = { summary = false },
            title = { Text("خلاصه وضعیت دسته چک") },
            text = {
                Column(Modifier.testTag("cheque-summary-sheet")) {
                    book.summary.forEach { (label, count) ->
                        PaymentRow(label, persianNumber(count.toLong()))
                    }
                }
            },
            confirmButton = { TextButton({ summary = false }) { Text("بستن") } },
        )
    }
}

@Composable
private fun ChequeCard(cheque: ChequePaper) {
    OutlinedCard(Modifier.fillMaxWidth().testTag("cheque-${cheque.no}")) {
        Column(Modifier.padding(12.dp)) {
            PaymentRow("شماره چک:", cheque.no)
            PaymentRow("مبلغ چک:", cheque.amount?.let { "${persianNumber(it)} ریال" } ?: "-")
            PaymentRow("شناسه صیاد:", cheque.sayadId)
            PaymentRow("شعبه عامل:", cheque.branch)
            PaymentRow("تاریخ چک:", cheque.date)
            Text(cheque.statusDesc, color = ServiceGreen)
        }
    }
}

@Composable
private fun ChequeTrack(
    accountKey: String,
    onAccount: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var chequeNo by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var resultNo by rememberSaveable { mutableStateOf<String?>(null) }
    var missing by rememberSaveable { mutableStateOf(false) }
    ChequeAccountPicker(accountKey, onAccount)
    PaymentInput("cheque-no", "شماره چک", chequeNo, error, "534001/229900") {
        chequeNo = ChequeRules.chequeNoInput(it)
        error = null
    }
    Button(
        {
            error = ChequeRules.chequeNoError(chequeNo)
            if (error == null) {
                val cheque = ChequeRules.track(accountKey, chequeNo)
                resultNo = cheque?.no
                missing = cheque == null
            }
        },
        Modifier.fillMaxWidth().testTag("cheque-track"),
    ) {
        Text("مشاهده نتیجه")
    }
    val cheque = resultNo?.let { ChequeRules.track(accountKey, it) }
    if (cheque != null) {
        AlertDialog(
            onDismissRequest = { resultNo = null },
            title = { Text("شماره چک ${cheque.no}") },
            text = {
                Column(Modifier.testTag("cheque-track-result")) {
                    PaymentRow("وضعیت چک", cheque.statusDesc)
                    PaymentRow("مبلغ چک", cheque.amount?.let { "${persianNumber(it)} ریال" } ?: "-")
                    PaymentRow("حساب", chequeAccount(accountKey))
                    PaymentRow("شعبه عامل", cheque.branch)
                    PaymentRow("تاریخ آخرین وضعیت", cheque.lastStatusDate)
                    PaymentRow("بانک", "بانک نمایشی")
                }
            },
            confirmButton = { TextButton({ resultNo = null }) { Text("بازگشت") } },
            dismissButton = {
                TextButton({ onUnavailable("اشتراک‌گذاری") }, Modifier.testTag("cheque-share")) {
                    Text("اشتراک‌گذاری")
                }
            },
        )
    }
    if (missing) {
        AlertDialog(
            onDismissRequest = { missing = false },
            text = {
                Text(
                    "چکی با این شماره در دادهٔ نمایشی این حساب یافت نشد.",
                    Modifier.testTag("cheque-not-found"),
                )
            },
            confirmButton = { TextButton({ missing = false }) { Text("بستن") } },
        )
    }
}

@Composable
private fun ChequeReportForm(
    accountKey: String,
    filter: ChequeReportFilter,
    onAccount: (String) -> Unit,
    onChange: (ChequeReportFilter) -> Unit,
    onOpen: (String) -> Unit,
) {
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    Text(if (filter.type == "OTHER") "چک‌های سایر بانک‌ها" else "چک‌های داخلی")
    ChequeAccountPicker(accountKey, onAccount)
    Text("وضعیت")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chequeReportSubtypes.forEach { (key, label) ->
            FilterChip(
                filter.subtype == key,
                {
                    errors = emptyMap()
                    onChange(filter.copy(subtype = key))
                },
                { Text(label) },
                Modifier.testTag("cheque-status-$key"),
            )
        }
    }
    errors["status"]?.let { ChequeError("cheque-status-error", it) }
    if (filter.dated) {
        Row {
            PersianDateField(
                filter.from,
                { onChange(filter.copy(from = it)) },
                "از تاریخ",
                "cheque-from",
                Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PersianDateField(
                filter.to,
                { onChange(filter.copy(to = it)) },
                "تا تاریخ",
                "cheque-to",
                Modifier.weight(1f),
            )
        }
        listOf("from", "to", "range").forEach { key ->
            errors[key]?.let { ChequeError("cheque-$key-error", it) }
        }
    }
    Button(
        {
            errors = ChequeRules.reportErrors(filter)
            if (errors.isEmpty()) onOpen(ChequeDestinations.REPORT)
        },
        Modifier.fillMaxWidth().testTag("cheque-report-apply"),
    ) {
        Text("مشاهده نتیجه")
    }
}

@Composable
private fun ChequeError(tag: String, text: String) {
    Text(text, Modifier.testTag(tag), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

@Composable
private fun ChequeReport(accountKey: String, filter: ChequeReportFilter) {
    var query by rememberSaveable { mutableStateOf("") }
    PaymentRow("حساب", chequeAccount(accountKey))
    PaymentRow("وضعیت", chequeReportSubtypes.find { it.first == filter.subtype }?.second ?: "")
    if (filter.dated) PaymentRow("بازه", "${filter.from} تا ${filter.to}")
    PaymentInput("cheque-report-search", "جستجو بر اساس مبلغ یا شماره چک", query, null) {
        query = it
    }
    val rows =
        ChequeRules.searchReport(
            ChequeFixture.report.filter { it.type == filter.type && it.subtype == filter.subtype },
            query,
        )
    if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("cheque-empty"))
    rows.forEach {
        OutlinedCard(Modifier.fillMaxWidth().testTag("cheque-report-${it.no}")) {
            Column(Modifier.padding(12.dp)) {
                PaymentRow("شماره چک:", it.no)
                PaymentRow("مبلغ چک:", "${persianNumber(it.amount)} ریال")
                PaymentRow("تاریخ چک:", it.date)
            }
        }
    }
}

@Composable
private fun ChequeIssue(
    draft: ChequeIssueDraft,
    onChange: (ChequeIssueDraft) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    ChequeAccountPicker(draft.accountKey) {
        errors = errors - "account"
        onChange(draft.copy(accountKey = it))
    }
    errors["account"]?.let {
        Text(it, Modifier.testTag("cheque-account-error"), color = MaterialTheme.colorScheme.error)
    }
    Text("تعداد برگ")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChequeFixture.pageCounts.getValue(draft.media).forEach {
            FilterChip(
                draft.pageCount == it,
                {
                    errors = errors - "page"
                    onChange(draft.copy(pageCount = it))
                },
                { Text("$it برگ") },
                Modifier.testTag("cheque-page-$it"),
            )
        }
    }
    errors["page"]?.let {
        Text(it, Modifier.testTag("cheque-page-error"), color = MaterialTheme.colorScheme.error)
    }
    Button(
        {
            errors = ChequeRules.issueErrors(draft)
            if (errors.isEmpty()) onOpen(ChequeDestinations.ISSUE_CONFIRM)
        },
        Modifier.fillMaxWidth().testTag("cheque-issue-continue"),
    ) {
        Text("ادامه")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
}

@Composable
private fun ChequeCardBoard(onTask: (String) -> Unit, onOpen: (String) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("WAITING") }
    var query by rememberSaveable { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChequeFixture.tabs.forEach { (key, label) ->
            FilterChip(
                tab == key,
                { tab = key },
                { Text(label) },
                Modifier.testTag("cardboard-tab-$key"),
            )
        }
    }
    PaymentInput("cardboard-search", "جستجو حساب، شماره کارتابل", query, null) { query = it }
    val rows =
        ChequeFixture.tasks.filter { it.tab == tab && (query.isBlank() || query in it.processName) }
    if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("cheque-empty"))
    rows.forEach { task ->
        OutlinedCard(
            Modifier.fillMaxWidth().testTag("cardboard-${task.id}").clickable(role = Role.Button) {
                onTask(task.id)
                // Recovered: paper-cheque processes open the cheque details, others the request.
                onOpen(
                    if (task.pichak) ChequeDestinations.PAY_DETAIL
                    else ChequeDestinations.REQUEST_DETAIL
                )
            }
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(task.processName)
                Text("${task.issuer} · ${task.date}", fontSize = 13.sp)
                Text(task.statusDesc, fontSize = 13.sp, color = ServiceGreen)
            }
        }
    }
}

@Composable
private fun ChequeTaskDetail(
    page: String,
    task: WorkflowTask,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    TransferValue("عنوان", task.processName)
    TransferValue("درخواست‌دهنده", task.issuer)
    TransferValue("تاریخ", task.date)
    TransferValue("وضعیت", task.statusDesc)
    if (page == ChequeDestinations.PAY_DETAIL) {
        TransferValue("مبلغ چک", "${persianNumber(task.amount)} ریال")
        TransferValue("گیرنده:", "گیرندهٔ نمایشی")
        TransferValue("بابت:", "بابت نمایشی")
        TransferValue("امضاداران", "کاربر نمایشی، عضو نمایشی")
    } else {
        TransferValue("کارمزد درخواست:", "${persianNumber(task.amount)} ریال")
        TransferValue("امضاداران", "کاربر نمایشی، عضو نمایشی")
    }
    // Recovered actions (approve, reject, cancel) need the user's OTP; none is performed here.
    if (task.tab == "WAITING") {
        Button(
            { onUnavailable("تایید درخواست کارتابل") },
            Modifier.fillMaxWidth().testTag("cardboard-act"),
        ) {
            Text("تایید — در دسترس نیست")
        }
        OutlinedButton(
            { onUnavailable("رد درخواست کارتابل") },
            Modifier.fillMaxWidth().testTag("cardboard-reject"),
        ) {
            Text("رد — در دسترس نیست")
        }
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("cardboard-back")) { Text("بازگشت") }
}
