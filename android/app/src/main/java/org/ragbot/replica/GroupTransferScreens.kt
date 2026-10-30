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
import androidx.compose.ui.unit.dp

/** GT-RECOVERY: recovered group-transfer pages with local-only waiting, review and receipts. */
@Composable
internal fun GroupTransferPage(
    page: String,
    scenario: String,
    draft: GroupTransferDraft,
    trace: String,
    destination: Long,
    cancelling: Boolean,
    onChange: (GroupTransferDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onTrace: (String) -> Unit,
    onDestination: (Long) -> Unit,
    onCancel: (Boolean) -> Unit,
    onLeave: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            GroupTransferDestinations.RECEIPT -> "رسید انتقال وجه گروهی"
            GroupTransferDestinations.HISTORY -> "سوابق تراکنش گروهی"
            GroupTransferDestinations.DESTINATIONS -> "جزئیات مقصدها"
            GroupTransferDestinations.DESTINATION -> "جزئیات تراکنش"
            else -> "انتقال وجه گروهی"
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
                val history = GroupTransferFixture.history.find { it.traceId == trace }
                when (page) {
                    GroupTransferDestinations.FIRST ->
                        GroupFirstPage(draft, onChange, onBack, onOpen)
                    GroupTransferDestinations.MANUAL ->
                        GroupManualList(draft, onChange, onBack, onOpen, onUnavailable)
                    GroupTransferDestinations.FILE_PICK ->
                        GroupFilePick(draft, onChange, onOpen, onUnavailable)
                    GroupTransferDestinations.FILE_LIST -> GroupFileList(draft, onChange, onOpen)
                    GroupTransferDestinations.WAITING -> GroupWaiting(draft, onOpen, onCancel)
                    GroupTransferDestinations.VALIDATION ->
                        GroupValidation(draft, onChange, onOpen, onCancel)
                    GroupTransferDestinations.ERRORS ->
                        GroupErrors(draft, onChange, onOpen, onCancel)
                    GroupTransferDestinations.VERIFY -> GroupVerify(draft, onCancel, onUnavailable)
                    GroupTransferDestinations.RECEIPT -> GroupReceipt(onBack, onUnavailable)
                    GroupTransferDestinations.HISTORY -> GroupHistoryList(onTrace, onUnavailable)
                    GroupTransferDestinations.DESTINATIONS ->
                        history?.let { GroupDestinationList(it, onDestination) }
                    else ->
                        history
                            ?.rows
                            ?.find { it.id == destination }
                            ?.let { GroupDestinationDetail(it, onBack) }
                }
            }
        }
    }
    if (cancelling) {
        AlertDialog(
            onDismissRequest = { onCancel(false) },
            title = { Text("ابطال فرآیند") },
            text = {
                Text("فرآیند انتقال گروهی لغو شود؟ در این پیش‌نمایش چیزی حذف یا ارسال نمی‌شود.")
            },
            confirmButton = {
                TextButton(onLeave, Modifier.testTag("group-cancel-confirm")) { Text("ابطال") }
            },
            dismissButton = {
                TextButton({ onCancel(false) }, Modifier.testTag("group-cancel-continue")) {
                    Text("ادامه فرآیند")
                }
            },
        )
    }
}

@Composable
private fun GroupFirstPage(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var sourcePicker by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val source = AccountFixture.accounts.find { it.key == draft.sourceId }
    Text("حساب مبدأ")
    OutlinedButton(
        {
            focus.clearFocus()
            sourcePicker = true
        },
        Modifier.fillMaxWidth().testTag("group-source"),
    ) {
        Text(source?.let { "${it.type} · ${it.number}" } ?: "انتخاب حساب مبدأ")
    }
    TransferInput(
        "group-total",
        "مبلغ کل (ریال)",
        draft.total,
        { onChange(draft.copy(total = it)) },
        numeric = true,
    )
    listOf(false to "ورود دستی مقصدها (حداکثر 30تا)", true to "بارگذاری فایل مقصدها (حداکثر 500تا)")
        .forEach { (byFile, label) ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable {
                        // A different entry mode starts a fresh destination list.
                        if (byFile != draft.byFile)
                            onChange(
                                GroupTransferDraft(
                                    draft.sourceId,
                                    draft.total,
                                    byFile,
                                    draft.withdrawId,
                                    draft.sourceDescription,
                                )
                            )
                    }
                    .testTag(if (byFile) "group-mode-file" else "group-mode-manual")
            ) {
                RadioButton(draft.byFile == byFile, null)
                Text(label)
            }
        }
    TextButton(
        {
            focus.clearFocus()
            extra = !extra
        },
        Modifier.testTag("group-extra"),
    ) {
        Text("افزودن شناسه برداشت و شرح مبدا")
    }
    if (extra) {
        TransferInput(
            "group-withdraw-id",
            "شناسه برداشت (اختیاری)",
            draft.withdrawId,
            {
                if (latinDigits(it).all { ch -> ch in '0'..'9' } && it.length <= 17)
                    onChange(draft.copy(withdrawId = it))
            },
            numeric = true,
        )
        TransferInput(
            "group-source-description",
            "شرح مبدأ (اختیاری)",
            draft.sourceDescription,
            { if (it.length <= 50) onChange(draft.copy(sourceDescription = it)) },
        )
    }
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("group-error"),
        )
    }
    Text(
        "موجودی و تراکنش گروهی در جریان نیازمند سرویس‌اند؛ در این نمونه بررسی نمی‌شوند.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        {
            focus.clearFocus()
            error = GroupTransferRules.firstPageError(draft)
            if (error == null)
                onOpen(
                    if (draft.byFile) GroupTransferDestinations.FILE_PICK
                    else GroupTransferDestinations.MANUAL
                )
        },
        Modifier.fillMaxWidth().testTag("group-continue"),
    ) {
        Text("ادامه")
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
                            Modifier.testTag("group-source-${account.key}"),
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
    BackHandler(sourcePicker) { sourcePicker = false }
}

@Composable
private fun GroupManualList(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(false) }
    draft.rows.forEachIndexed { index, row ->
        // Row-local editor state follows its row when another row is deleted.
        key(row.id) {
            OutlinedCard(Modifier.fillMaxWidth().testTag("group-row-$index")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("مقصد ${index + 1}", Modifier.weight(1f))
                        if (draft.rows.size > 1)
                            TextButton(
                                { onChange(draft.copy(rows = draft.rows - row)) },
                                Modifier.testTag("group-row-$index-delete"),
                            ) {
                                Text("حذف")
                            }
                    }
                    GroupRowFields("group-row-$index", row) { changed ->
                        onChange(
                            draft.copy(
                                rows = draft.rows.map { if (it.id == row.id) changed else it }
                            )
                        )
                    }
                    if (checked)
                        GroupTransferRules.rowError(row)?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("group-row-$index-error"),
                            )
                        }
                }
            }
        }
    }
    Button(
        {
            onChange(
                draft.copy(
                    rows = draft.rows + GroupRow((draft.rows.maxOfOrNull { it.id } ?: 0) + 1)
                )
            )
        },
        Modifier.fillMaxWidth().testTag("group-add"),
        enabled = draft.rows.size < GroupTransferRules.MANUAL_MAX,
    ) {
        Text("افزودن مقصد")
    }
    TextButton({ onUnavailable("انتخاب مقصد ذخیره‌شده") }, Modifier.testTag("group-favorites")) {
        Text("مقصدهای ذخیره‌شده")
    }
    GroupTotal(draft.rows)
    GroupAccept(draft, onChange, onOpen) { checked = true }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
}

@Composable
private fun GroupRowFields(tag: String, row: GroupRow, onChange: (GroupRow) -> Unit) {
    var reasonPicker by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf(false) }
    TransferInput(
        "$tag-iban",
        "شماره شبا بدون IR",
        row.iban,
        { onChange(row.copy(iban = it)) },
        numeric = true,
    )
    TransferInput(
        "$tag-amount",
        "مبلغ (ریال)",
        row.amount,
        { onChange(row.copy(amount = it)) },
        numeric = true,
    )
    OutlinedButton({ reasonPicker = true }, Modifier.fillMaxWidth().testTag("$tag-reason")) {
        Text(row.reason.ifEmpty { "بابت — لطفاً انتخاب کنید" })
    }
    TextButton({ extra = !extra }, Modifier.testTag("$tag-extra")) {
        Text("افزودن شناسه واریز و شرح مقصد")
    }
    if (extra) {
        TransferInput(
            "$tag-payment-id",
            "شناسه واریز (اختیاری) — حداکثر ۳۰ رقم",
            row.paymentId,
            { if (it.length <= 30) onChange(row.copy(paymentId = it)) },
            numeric = true,
        )
        TransferInput(
            "$tag-description",
            "شرح مقصد (اختیاری)",
            row.description,
            { onChange(row.copy(description = it)) },
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
                                onChange(row.copy(reason = reason))
                                reasonPicker = false
                            },
                            Modifier.testTag("$tag-reason-$index"),
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
}

@Composable
private fun GroupTotal(rows: List<GroupRow>) {
    TransferValue(
        "مبلغ کل مقصدها (ریال)",
        GroupTransferRules.total(rows)?.let(::persianNumber) ?: "—",
    )
}

/** Recovered accept checks: valid rows, more than one destination and an equal total. */
@Composable
private fun GroupAccept(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
    onChecked: () -> Unit,
) {
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var conflict by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val sum = GroupTransferRules.total(draft.rows)
    fun proceed(total: String) {
        onChange(draft.copy(total = total, second = false))
        onOpen(GroupTransferDestinations.WAITING)
    }
    Button(
        {
            focus.clearFocus()
            onChecked()
            message = GroupTransferRules.acceptError(draft.rows)
            when {
                message != null -> {}
                sum != TransferRules.amountRial(draft.total) -> conflict = true
                else -> proceed(draft.total)
            }
        },
        Modifier.fillMaxWidth().testTag("group-accept"),
    ) {
        Text("ادامه")
    }
    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it, Modifier.testTag("group-accept-message")) },
            confirmButton = {
                TextButton({ message = null }, Modifier.testTag("group-accept-ok")) {
                    Text("متوجه شدم")
                }
            },
        )
    }
    if (conflict && sum != null) {
        AlertDialog(
            onDismissRequest = { conflict = false },
            title = { Text("مغایرت مبلغ") },
            text = {
                Text(
                    "مبلغ کل واردشده: ${TransferRules.amountRial(draft.total)?.let(::persianNumber) ?: "—"} ریال\n" +
                        "جمع مبالغ مقصدها: ${persianNumber(sum)} ریال",
                    Modifier.testTag("group-conflict"),
                )
            },
            confirmButton = {
                TextButton(
                    {
                        conflict = false
                        proceed(sum.toString())
                    },
                    Modifier.testTag("group-conflict-accept"),
                ) {
                    Text("پذیرش جمع مقصدها")
                }
            },
            dismissButton = { TextButton({ conflict = false }) { Text("اصلاح") } },
        )
    }
}

@Composable
private fun GroupFilePick(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf(false) }
    val file = GroupTransferFixture.files.find { it.key == draft.fileKey }
    val rows = file?.let { GroupTransferRules.parse(it.sheet) }.orEmpty()
    val error = file?.let { GroupTransferRules.fileError(rows) }
    Text(
        "فقط فایل‌های ساختگی داخلی؛ هیچ فایلی از دستگاه یا رایانه خوانده نمی‌شود.",
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButton({ picker = true }, Modifier.fillMaxWidth().testTag("group-file-pick")) {
        Text("انتخاب فایل اکسل")
    }
    if (file != null) {
        OutlinedCard(Modifier.fillMaxWidth().testTag("group-file-state")) {
            Column(Modifier.padding(12.dp)) {
                Text(file.name)
                Text("${persianNumber(rows.size.toLong())} رکورد")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(
                    { onChange(draft.copy(fileKey = null)) },
                    Modifier.testTag("group-file-clear"),
                ) {
                    Text("حذف فایل")
                }
            }
        }
    }
    TextButton({ onUnavailable("دریافت فایل نمونه") }, Modifier.testTag("group-file-example")) {
        Text("دریافت فایل نمونه")
    }
    TextButton({ onUnavailable("دریافت راهنما") }, Modifier.testTag("group-file-guide")) {
        Text("دریافت راهنما")
    }
    Button(
        {
            if (file != null) {
                onChange(draft.copy(rows = rows, rejected = file.rejected))
                onOpen(GroupTransferDestinations.FILE_LIST)
            }
        },
        Modifier.fillMaxWidth().testTag("group-file-apply"),
        enabled = file != null && error == null,
    ) {
        Text("ادامه")
    }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("فایل‌های ساختگی") },
            text = {
                Column {
                    GroupTransferFixture.files.forEach {
                        TextButton(
                            {
                                onChange(draft.copy(fileKey = it.key))
                                picker = false
                            },
                            Modifier.testTag("group-file-${it.key}"),
                        ) {
                            Text(it.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ picker = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun GroupFileList(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
) {
    var onlyInvalid by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<GroupRow?>(null) }
    var removing by rememberSaveable { mutableStateOf(false) }
    val invalid = draft.rows.filter { GroupTransferRules.rowError(it) != null }
    Text("اطلاعات مقصدها - ${persianNumber(draft.rows.size.toLong())} مقصد")
    Row(
        Modifier.fillMaxWidth()
            .clickable { onlyInvalid = !onlyInvalid }
            .testTag("group-only-invalid")
    ) {
        Checkbox(onlyInvalid, null)
        Text("نمایش رکوردهای غیر قابل قبول - ${persianNumber(invalid.size.toLong())} رکورد")
    }
    (if (onlyInvalid) invalid else draft.rows).forEach { row ->
        OutlinedCard(Modifier.fillMaxWidth().testTag("group-file-row-${row.id}")) {
            Column(Modifier.padding(12.dp)) {
                Text("ردیف ${persianNumber(row.id)}")
                Text("IR${row.iban}")
                Text(
                    "${TransferRules.amountRial(row.amount)?.let(::persianNumber) ?: row.amount} ریال"
                )
                Text(row.reason.ifEmpty { "بابت ندارد" })
                GroupTransferRules.rowError(row)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Row {
                    TextButton(
                        { editing = row },
                        Modifier.testTag("group-file-row-${row.id}-edit"),
                    ) {
                        Text("ویرایش")
                    }
                    TextButton(
                        { onChange(draft.copy(rows = draft.rows - row)) },
                        Modifier.testTag("group-file-row-${row.id}-delete"),
                    ) {
                        Text("حذف")
                    }
                }
            }
        }
    }
    if (invalid.isNotEmpty())
        OutlinedButton(
            { removing = true },
            Modifier.fillMaxWidth().testTag("group-remove-invalid"),
        ) {
            Text("حذف رکوردها")
        }
    GroupTotal(draft.rows)
    GroupAccept(draft, onChange, onOpen) {}
    editing?.let { original ->
        var row by remember(original.id) { mutableStateOf(original) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("ویرایش ردیف ${persianNumber(original.id)}") },
            text = { Column { GroupRowFields("group-edit", row) { row = it } } },
            confirmButton = {
                TextButton(
                    {
                        onChange(
                            draft.copy(rows = draft.rows.map { if (it.id == row.id) row else it })
                        )
                        editing = null
                    },
                    Modifier.testTag("group-edit-apply"),
                ) {
                    Text("اعمال")
                }
            },
            dismissButton = { TextButton({ editing = null }) { Text("انصراف") } },
        )
    }
    if (removing) {
        AlertDialog(
            onDismissRequest = { removing = false },
            text = { Text("${persianNumber(invalid.size.toLong())} رکورد غیر قابل قبول حذف شود؟") },
            confirmButton = {
                TextButton(
                    {
                        onChange(draft.copy(rows = draft.rows - invalid.toSet()))
                        removing = false
                    },
                    Modifier.testTag("group-remove-confirm"),
                ) {
                    Text("حذف رکوردها")
                }
            },
            dismissButton = { TextButton({ removing = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun GroupWaiting(
    draft: GroupTransferDraft,
    onOpen: (String) -> Unit,
    onCancel: (Boolean) -> Unit,
) {
    Text(
        if (draft.second) "اطلاعات مقصدها ثبت شده است." else "در حال پردازش اطلاعات مقصدها",
        Modifier.testTag("group-waiting-${if (draft.second) "second" else "first"}"),
    )
    Text("برای مشاهده آخرین وضعیت، به‌روزرسانی کنید.")
    Text(
        "پیش‌نمایش محلی: به‌روزرسانی بلافاصله وضعیت ساختگی «آماده» را نشان می‌دهد؛ هیچ سرویسی بررسی نمی‌شود.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        {
            onOpen(
                if (draft.second) GroupTransferDestinations.VERIFY
                else GroupTransferDestinations.VALIDATION
            )
        },
        Modifier.fillMaxWidth().testTag("group-refresh"),
    ) {
        Text("به‌روزرسانی")
    }
    OutlinedButton({ onCancel(true) }, Modifier.fillMaxWidth().testTag("group-cancel")) {
        Text("ابطال")
    }
}

@Composable
private fun GroupValidation(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
    onCancel: (Boolean) -> Unit,
) {
    Text("اطلاعات مقصدها - ${persianNumber(draft.rows.size.toLong())} مقصد")
    draft.rows.forEach { row ->
        val rejected = row.id in draft.rejected
        OutlinedCard(Modifier.fillMaxWidth().testTag("group-validated-${row.id}")) {
            Column(Modifier.padding(12.dp)) {
                Text("IR${row.iban}")
                Text("${TransferRules.amountRial(row.amount)?.let(::persianNumber)} ریال")
                Text(
                    if (rejected) "غیر قابل قبول — نتیجهٔ ساختگی ثبت" else "آماده — وضعیت ساختگی",
                    color =
                        if (rejected) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    GroupTotal(draft.rows)
    Button(
        {
            if (draft.rejected.isNotEmpty()) onOpen(GroupTransferDestinations.ERRORS)
            else {
                onChange(draft.copy(second = true))
                onOpen(GroupTransferDestinations.WAITING)
            }
        },
        Modifier.fillMaxWidth().testTag("group-validation-continue"),
    ) {
        Text("ادامه")
    }
    OutlinedButton({ onCancel(true) }, Modifier.fillMaxWidth().testTag("group-cancel")) {
        Text("ابطال")
    }
}

@Composable
private fun GroupErrors(
    draft: GroupTransferDraft,
    onChange: (GroupTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
    onCancel: (Boolean) -> Unit,
) {
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val rejected = draft.rows.filter { it.id in draft.rejected }
    Text("اطلاعات مقصدها - ${persianNumber(rejected.size.toLong())} مقصد")
    rejected.forEach { row ->
        OutlinedCard(Modifier.fillMaxWidth().testTag("group-rejected-${row.id}")) {
            Column(Modifier.padding(12.dp)) {
                Text("IR${row.iban}")
                Text("${TransferRules.amountRial(row.amount)?.let(::persianNumber)} ریال")
                Text("خطای نمونهٔ ثبت — ساختگی", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        {
            val kept = draft.rows - rejected.toSet()
            message = GroupTransferRules.acceptError(kept)
            if (message == null) {
                onChange(
                    draft.copy(
                        rows = kept,
                        rejected = emptyList(),
                        total = GroupTransferRules.total(kept).toString(),
                        second = true,
                    )
                )
                onOpen(GroupTransferDestinations.WAITING)
            }
        },
        Modifier.fillMaxWidth().testTag("group-remove-rejected"),
    ) {
        Text("حذف رکوردهای غیر مجاز")
    }
    OutlinedButton({ onCancel(true) }, Modifier.fillMaxWidth().testTag("group-cancel")) {
        Text("ابطال")
    }
}

@Composable
private fun GroupVerify(
    draft: GroupTransferDraft,
    onCancel: (Boolean) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var details by rememberSaveable { mutableStateOf(false) }
    val source = AccountFixture.accounts.find { it.key == draft.sourceId }
    Text("انتقال وجه گروهی — بررسی محلی")
    TransferValue("مبدا", source?.number ?: "نامشخص")
    TransferValue("مقصد", "${persianNumber(draft.rows.size.toLong())} مقصد")
    TransferValue(
        "مبلغ کل (ریال)",
        TransferRules.amountRial(draft.total)?.let(::persianNumber) ?: "—",
    )
    if (draft.withdrawId.isNotEmpty()) TransferValue("شناسه برداشت:", draft.withdrawId)
    if (draft.sourceDescription.isNotEmpty()) TransferValue("شرح مبدا:", draft.sourceDescription)
    TextButton({ details = true }, Modifier.testTag("group-verify-details")) {
        Text("جزئیات مقصدها")
    }
    TextButton(
        { onUnavailable("افزودن یادداشت و پیوست تراکنش") },
        Modifier.testTag("group-verify-attachment"),
    ) {
        Text("افزودن یادداشت و پیوست تراکنش")
    }
    Text(
        "تأیید پرداخت، رمز پویا و ارسال در این پیش‌نمایش در دسترس نیست. هیچ انتقالی انجام نمی‌شود."
    )
    Button(
        { onUnavailable("تایید پرداخت و ارسال انتقال گروهی") },
        Modifier.fillMaxWidth().testTag("group-verify-accept"),
    ) {
        Text("تایید — در دسترس نیست")
    }
    OutlinedButton({ onCancel(true) }, Modifier.fillMaxWidth().testTag("group-cancel")) {
        Text("ابطال")
    }
    if (details) {
        AlertDialog(
            onDismissRequest = { details = false },
            title = { Text("جزئیات مقصدها") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    draft.rows.forEach {
                        Text(
                            "IR${it.iban} · ${TransferRules.amountRial(it.amount)?.let(::persianNumber)} ریال"
                        )
                    }
                }
            },
            confirmButton = { TextButton({ details = false }) { Text("بازگشت") } },
        )
    }
}

@Composable
private fun GroupReceipt(onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    val sample = GroupTransferFixture.history.first()
    Text(
        "رسید ساختگی مستقل برای بازبینی؛ حاصل انتقال وجه نیست.",
        modifier = Modifier.testTag("group-receipt-disclosure"),
    )
    TransferValue("وضعیت", "نمونهٔ نمایشی — بدون تراکنش بانکی")
    TransferValue("زمان", sample.date)
    TransferValue(
        "مجموع مبالغ (ریال)",
        GroupTransferRules.total(sample.rows)?.let(::persianNumber) ?: "—",
    )
    TransferValue("مقصد", "${persianNumber(sample.rows.size.toLong())} مقصد")
    TransferValue("مبدا", sample.source)
    TransferValue("شماره پیگیری ساختگی", sample.traceId)
    Button(
        { onUnavailable("مشاهده سوابق از رسید") },
        Modifier.fillMaxWidth().testTag("group-receipt-history"),
    ) {
        Text("مشاهده سوابق")
    }
    Button(
        { onUnavailable("ذخیره و اشتراک‌گذاری رسید") },
        Modifier.fillMaxWidth().testTag("group-receipt-share"),
    ) {
        Text("ذخیره و اشتراک‌گذاری")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
}

@Composable
private fun GroupHistoryList(onTrace: (String) -> Unit, onUnavailable: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    TransferInput("group-history-search", "جستجوی مبلغ", query, { query = it }, numeric = true)
    val shown =
        GroupTransferFixture.history.filter {
            latinDigits(query).trim().let { q ->
                q.isEmpty() || GroupTransferRules.total(it.rows).toString().contains(q)
            }
        }
    if (shown.isEmpty())
        Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("group-history-empty"))
    shown.forEach { item ->
        OutlinedCard(Modifier.fillMaxWidth().testTag("group-history-${item.traceId}")) {
            Column(Modifier.padding(12.dp)) {
                Text("انتقال وجه گروهی · ${item.date}")
                Text(item.status)
                Text("مبلغ: ${GroupTransferRules.total(item.rows)?.let(::persianNumber)} ریال")
                Text("مبدا: ${item.source}")
                Text("نام مبدا: ${item.sourceName}")
                Text("مقصد: ${persianNumber(item.rows.size.toLong())} مقصد")
                Text("شناسه برداشت: ${item.withdrawId.ifEmpty { "-" }}")
                Text("شرح مبدا: ${item.sourceDescription.ifEmpty { "-" }}")
                Text("شماره پیگیری: ${item.traceId}")
                Text("یادداشت: ${item.note.ifEmpty { "-" }}")
                Row {
                    TextButton(
                        { onTrace(item.traceId) },
                        Modifier.testTag("group-history-${item.traceId}-destinations"),
                    ) {
                        Text("جزئیات مقصدها")
                    }
                    TextButton(
                        { onUnavailable("دریافت پیوست و فایل سوابق") },
                        Modifier.testTag("group-history-${item.traceId}-attachment"),
                    ) {
                        Text("پیوست تراکنش")
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupDestinationList(history: GroupHistory, onDestination: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    TransferInput(
        "group-destination-search",
        "جستجوی شماره مقصد",
        query,
        { query = it },
        numeric = true,
    )
    val shown =
        history.rows.filter { it.iban.contains(latinDigits(query).trim().removePrefix("IR")) }
    if (shown.isEmpty())
        Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("group-destination-empty"))
    shown.forEach { row ->
        OutlinedCard(
            Modifier.fillMaxWidth()
                .clickable { onDestination(row.id) }
                .testTag("group-destination-${row.id}")
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("IR${row.iban}")
                Text("${TransferRules.amountRial(row.amount)?.let(::persianNumber)} ریال")
                Text(row.description)
            }
        }
    }
}

@Composable
private fun GroupDestinationDetail(row: GroupRow, onBack: () -> Unit) {
    TransferValue("نوع تراکنش:", "انتقال وجه گروهی — نمونه")
    TransferValue("وضعیت:", "نمونهٔ نمایشی — بدون تراکنش بانکی")
    TransferValue("شرح وضعیت:", "رکورد ثابت ساختگی")
    TransferValue("مقصد", "IR${row.iban}")
    TransferValue("مبلغ (ریال)", TransferRules.amountRial(row.amount)?.let(::persianNumber) ?: "—")
    TransferValue("شناسه واریز:", row.paymentId.ifEmpty { "-" })
    TransferValue("بابت:", row.reason)
    TransferValue("شرح مقصد:", row.description.ifEmpty { "-" })
    TransferValue("کد مرجع:", "DEMO-REF-${row.id}")
    TransferValue("شماره پیگیری:", "DEMO-TRACE-${row.id}")
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("group-destination-back")) {
        Text("بازگشت")
    }
}
