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
import androidx.compose.ui.unit.dp

/** RT-RECOVERY: standing-order and repetitive-transaction pages; nothing is scheduled or sent. */
@Composable
internal fun RecurringTransferPage(
    page: String,
    scenario: String,
    draft: ScheduleDraft,
    records: List<ContinuousRecord>,
    repetitive: List<RepetitiveRecord>,
    selected: String,
    onChange: (ScheduleDraft) -> Unit,
    onRecords: (List<ContinuousRecord>) -> Unit,
    onRepetitive: (List<RepetitiveRecord>) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    onRepeat: (RepetitiveRecord) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            RecurringTransferDestinations.REQUEST -> "درخواست انتقال وجه خودکار"
            RecurringTransferDestinations.EXECUTION -> "تایید تاریخ انتقال وجه خودکار"
            RecurringTransferDestinations.HISTORY -> "سوابق انتقال وجه خودکار"
            RecurringTransferDestinations.DETAIL -> "جزئیات انتقال وجه خودکار"
            RecurringTransferDestinations.REPETITIVE -> "تراکنش‌های پرتکرار"
            else -> "انتقال وجه خودکار"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(RecurringTransferFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    RecurringTransferDestinations.MENU -> {
                        Text("فهرست نمونهٔ خدمات؛ ترتیب و دسترسی محلی است.")
                        Button(
                            { onOpen(RecurringTransferDestinations.REQUEST) },
                            Modifier.fillMaxWidth().testTag("recurring-new"),
                        ) {
                            Text("درخواست انتقال وجه خودکار")
                        }
                        Button(
                            { onOpen(RecurringTransferDestinations.HISTORY) },
                            Modifier.fillMaxWidth().testTag("recurring-history"),
                        ) {
                            Text("سوابق انتقال وجه خودکار")
                        }
                    }
                    RecurringTransferDestinations.REQUEST ->
                        ScheduleRequest(draft, onChange, onBack, onOpen)
                    RecurringTransferDestinations.EXECUTION ->
                        ScheduleExecutionPreview(draft, onBack, onContinue)
                    RecurringTransferDestinations.HISTORY ->
                        ContinuousHistory(records, onOpen, onSelect)
                    RecurringTransferDestinations.DETAIL ->
                        records
                            .find { it.traceId == selected }
                            ?.let {
                                ContinuousDetail(it, records, onRecords, onBack, onUnavailable)
                            }
                    else -> RepetitiveList(repetitive, onRepetitive, onRepeat)
                }
            }
        }
    }
}

@Composable
private fun ScheduleRequest(
    draft: ScheduleDraft,
    onChange: (ScheduleDraft) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var dayPicker by rememberSaveable { mutableStateOf(false) }
    var typePicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    TransferInput(
        "schedule-title",
        "عنوان درخواست",
        draft.title,
        { onChange(draft.copy(title = it)) },
    )
    PersianDateField(
        draft.start,
        { onChange(draft.copy(start = it)) },
        "تاریخ شروع",
        "schedule-start",
        Modifier.fillMaxWidth(),
    )
    PersianDateField(
        draft.end,
        { onChange(draft.copy(end = it)) },
        "تاریخ پایان",
        "schedule-end",
        Modifier.fillMaxWidth(),
    )
    Text("نوع تکرار انتقال وجه")
    RecurringTransferRules.frequencies.forEach { (key, label) ->
        Row(
            Modifier.fillMaxWidth()
                .clickable {
                    focus.clearFocus()
                    // Changing the frequency resets its day, as the recovered sheets do.
                    onChange(draft.copy(frequency = key, day = 0))
                    if (key != RecurringTransferRules.DAILY) dayPicker = true
                }
                .testTag("schedule-frequency-$key")
        ) {
            RadioButton(draft.frequency == key, null)
            Text(
                if (draft.frequency == key) RecurringTransferRules.frequencyTitle(key, draft.day)
                else label
            )
        }
    }
    if (RecurringTransferRules.monthWarning(draft))
        Text(
            RecurringTransferRules.MONTH_WARNING,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("schedule-month-warning"),
        )
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("schedule-error"),
        )
    }
    Button(
        {
            focus.clearFocus()
            error =
                RecurringTransferRules.requestError(
                    draft,
                    persianDateMillis(draft.start),
                    persianDateMillis(draft.end),
                )
            if (error == null) typePicker = true
        },
        Modifier.fillMaxWidth().testTag("schedule-continue"),
    ) {
        Text("ادامه")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
    if (dayPicker) {
        AlertDialog(
            onDismissRequest = { dayPicker = false },
            title = {
                Text(
                    if (draft.frequency == RecurringTransferRules.WEEKLY) "انتخاب روز هفته"
                    else "انتخاب روز ماه"
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val days =
                        if (draft.frequency == RecurringTransferRules.WEEKLY)
                            RecurringTransferRules.weekDays
                        else (1..31).map { persianNumber(it.toLong()) to it }
                    days.forEach { (label, value) ->
                        TextButton(
                            {
                                onChange(draft.copy(day = value))
                                dayPicker = false
                            },
                            Modifier.testTag("schedule-day-$value"),
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ dayPicker = false }) { Text("انصراف") } },
        )
    }
    if (typePicker) {
        AlertDialog(
            onDismissRequest = { typePicker = false },
            title = { Text("نوع انتقال وجه") },
            text = {
                Column {
                    listOf(
                            AccountTransferDestinations.INTERNAL to "انتقال داخلی",
                            AccountTransferDestinations.INTERBANK to "انتقال بین بانکی",
                        )
                        .forEach { (type, label) ->
                            TextButton(
                                {
                                    onChange(draft.copy(transferType = type))
                                    typePicker = false
                                    onOpen(RecurringTransferDestinations.EXECUTION)
                                },
                                Modifier.testTag("schedule-type-$type"),
                            ) {
                                Text(label)
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ typePicker = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun ScheduleExecutionPreview(
    draft: ScheduleDraft,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    TransferValue("عنوان درخواست", draft.title)
    TransferValue(
        "نوع انتقال وجه",
        if (draft.transferType == AccountTransferDestinations.INTERNAL) "انتقال داخلی"
        else "انتقال بین بانکی",
    )
    TransferValue(
        "نوع تکرار انتقال وجه:",
        RecurringTransferRules.frequencyTitle(draft.frequency, draft.day),
    )
    TransferValue("تاریخ شروع:", draft.start)
    TransferValue("تاریخ پایان:", draft.end)
    Text(
        "تاریخ‌های اجرا را سرویس محاسبه می‌کند؛ این پیش‌نمایش فهرست اجرا نمی‌سازد و چیزی زمان‌بندی نمی‌کند.",
        Modifier.testTag("schedule-execution-unknown"),
    )
    if (RecurringTransferRules.monthWarning(draft))
        Text(RecurringTransferRules.MONTH_WARNING, style = MaterialTheme.typography.bodySmall)
    Button(onContinue, Modifier.fillMaxWidth().testTag("schedule-execution-continue")) {
        Text("ادامه")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
}

@Composable
private fun ContinuousHistory(
    records: List<ContinuousRecord>,
    onOpen: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    TransferInput("continuous-search", "جستجوی نام یا مقصد", query, { query = it })
    val shown =
        records.filter {
            val q = latinDigits(query).trim()
            q.isEmpty() || it.name.contains(q) || it.destination.contains(q)
        }
    if (shown.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("continuous-empty"))
    shown.forEach { record ->
        OutlinedCard(
            Modifier.fillMaxWidth()
                .clickable { onSelect(record.traceId) }
                .testTag("continuous-${record.traceId}")
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(record.name)
                Text(
                    if (record.cancelled) "لغو شده — وضعیت ساختگی" else "فعال — وضعیت ساختگی",
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("مبدا: ${record.source}")
                Text("مقصد: ${record.destination}")
                Text("مبلغ: ${persianNumber(record.amount)} ریال")
                Text("تاریخ ایجاد: ${record.created}")
                Text("تعداد‌ تراکنش‌ها: ${persianNumber(record.executions.size.toLong())}")
                Text(
                    "نوع تکرار انتقال وجه: ${RecurringTransferRules.frequencyTitle(record.frequency, 0)}"
                )
            }
        }
    }
    Button(
        { onOpen(RecurringTransferDestinations.REQUEST) },
        Modifier.fillMaxWidth().testTag("continuous-new"),
    ) {
        Text("درخواست جدید")
    }
}

@Composable
private fun ContinuousDetail(
    record: ContinuousRecord,
    records: List<ContinuousRecord>,
    onRecords: (List<ContinuousRecord>) -> Unit,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    val action = RecurringTransferRules.detailAction(record)
    TransferValue("عنوان", record.name)
    TransferValue(
        "نوع انتقال وجه",
        if (record.type == AccountTransferDestinations.INTERNAL) "انتقال داخلی"
        else "انتقال بین بانکی",
    )
    TransferValue("مبدا", record.source)
    TransferValue("مقصد", record.destination)
    TransferValue("مبلغ (ریال)", persianNumber(record.amount))
    TransferValue("تاریخ شروع:", record.start)
    TransferValue("تاریخ پایان:", record.end)
    TransferValue(
        "نوع تکرار انتقال وجه:",
        RecurringTransferRules.frequencyTitle(record.frequency, 0),
    )
    TransferValue("وضعیت", if (record.cancelled) "لغو شده — ساختگی" else "فعال — ساختگی")
    Text("اجراها")
    record.executions.forEachIndexed { index, execution ->
        OutlinedCard(Modifier.fillMaxWidth().testTag("continuous-execution-$index")) {
            Row(Modifier.padding(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(execution.date)
                    Text(RecurringTransferRules.orderStatuses[execution.status] ?: "نامشخص")
                }
                if (
                    execution.status != RecurringTransferRules.NOT_STARTED &&
                        execution.status != RecurringTransferRules.CANCELLED
                )
                    TextButton(
                        { onUnavailable("رسید اجرای انتقال وجه خودکار") },
                        Modifier.testTag("continuous-execution-$index-receipt"),
                    ) {
                        Text("رسید")
                    }
            }
        }
    }
    TextButton(
        { onUnavailable("دریافت پیوست تراکنش") },
        Modifier.testTag("continuous-attachment"),
    ) {
        Text("پیوست تراکنش")
    }
    Button(
        { if (action == "BACK") onBack() else confirm = true },
        Modifier.fillMaxWidth().testTag("continuous-action-$action"),
    ) {
        Text(
            when (action) {
                "DELETE" -> "حذف درخواست انتقال وجه"
                "CANCEL" -> "لغو درخواست انتقال وجه"
                else -> "بازگشت"
            }
        )
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = {
                Text(
                    if (action == "DELETE") "آیا مایل به حذف این انتقال وجه خودکار هستید؟"
                    else "آیا مایل به لغو این انتقال وجه خودکار هستید؟"
                )
            },
            text = {
                Text(
                    (if (action == "DELETE") "فقط تا قبل از شروع تراکنش ها امکان حذف وجود دارد."
                    else "فقط تراکنش های در انتظار لغو می شوند.") +
                        "\nتغییر فقط در همین پیش‌نمایش محلی اعمال می‌شود."
                )
            },
            confirmButton = {
                TextButton(
                    {
                        confirm = false
                        if (action == "DELETE") {
                            onRecords(records - record)
                            onBack()
                        } else {
                            onRecords(
                                records.map {
                                    if (it == record) RecurringTransferRules.cancel(it) else it
                                }
                            )
                        }
                    },
                    Modifier.testTag("continuous-confirm"),
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun RepetitiveList(
    repetitive: List<RepetitiveRecord>,
    onRepetitive: (List<RepetitiveRecord>) -> Unit,
    onRepeat: (RepetitiveRecord) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    TransferInput("repetitive-search", "جستجوی عنوان", query, { query = it })
    Text(
        "تکرار، پیش‌بررسی سرویس را انجام نمی‌دهد و فقط بازبینی محلی همان نوع را باز می‌کند.",
        style = MaterialTheme.typography.bodySmall,
    )
    val shown = repetitive.filter { query.isBlank() || it.title.contains(query.trim()) }
    if (shown.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("repetitive-empty"))
    shown.forEach { record ->
        OutlinedCard(Modifier.fillMaxWidth().testTag("repetitive-${record.id}")) {
            Column(Modifier.padding(12.dp)) {
                Text(record.title)
                Text("تراکنش: ${RecurringTransferFixture.typeTitles[record.type]}")
                Text(
                    "مبدا: ${AccountFixture.accounts.find { it.key == record.source }?.number ?: record.source}"
                )
                Text("نام مقصد: ${record.destinationName}")
                Text("بانک: ${record.bank}")
                Text("مبلغ: ${persianNumber(record.amount)} ریال")
                Text(record.created)
                Row {
                    TextButton(
                        { onRepeat(record) },
                        Modifier.testTag("repetitive-${record.id}-repeat"),
                    ) {
                        Text("تکرار تراکنش")
                    }
                    TextButton(
                        { deleting = record.id },
                        Modifier.testTag("repetitive-${record.id}-delete"),
                    ) {
                        Text("حذف")
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text("این تراکنش از فهرست محلی حذف شود؟") },
            confirmButton = {
                TextButton(
                    {
                        onRepetitive(repetitive.filter { it.id != id })
                        deleting = null
                    },
                    Modifier.testTag("repetitive-delete-confirm"),
                ) {
                    Text("حذف")
                }
            },
            dismissButton = { TextButton({ deleting = null }) { Text("انصراف") } },
        )
    }
}
