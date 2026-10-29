package org.ragbot.replica

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.text.ParsePosition

@Composable
internal fun HistoryScreen(onTransaction: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var shown by rememberSaveable { mutableIntStateOf(1) }
    val rows = HomeFixture.transactions.filter { historyMatches(it, query) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                query,
                {
                    query = it
                    shown = 1
                },
                Modifier.weight(1f).testTag("history-search"),
                label = { Text("جستجوی مبلغ یا نام مقصد") },
                singleLine = true,
            )
            IconButton({}, Modifier.testTag("history-filter"), enabled = false) {
                ReplicaIcon("replica_account_filter", "≡", Modifier.size(24.dp), Color.Gray)
            }
        }
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("history-scroll")
        ) {
            if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.")
            rows.take(shown).forEach { row ->
                Surface(
                    Modifier.fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("history-${row.key}")
                        .clickable(role = Role.Button) { onTransaction(row.key) },
                    color = Color.White,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("موفق - ${row.title}", color = ServiceGreen)
                        Text("${persianNumber(row.amount)} ریال")
                        Text("به ${row.destination}")
                        Text(row.time, color = Color.Gray)
                    }
                }
            }
            if (shown < rows.size)
                TextButton({ shown += 1 }, Modifier.testTag("history-more")) { Text("نمایش بیشتر") }
        }
    }
}

internal fun historyMatches(row: PreviewTransaction, query: String): Boolean {
    val normalized = latinDigits(query).replace("٬", "").replace(",", "").trim()
    return (normalized.isEmpty() ||
        row.amount.toString().contains(normalized) ||
        latinDigits(row.destination).contains(normalized))
}

@Composable
internal fun TransactionDetail(key: String, onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    val transaction = HomeFixture.transactions.first { it.key == key }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("موفق - ${transaction.title}", color = ServiceGreen)
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("جزئیات")
            listOf(
                    "مبلغ" to "${persianNumber(transaction.amount)} ریال",
                    "از" to transaction.source,
                    "به" to transaction.destination,
                    "زمان" to transaction.time,
                    "شماره پیگیری" to "DEMO-${transaction.key}",
                )
                .forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(label, Modifier.weight(1f))
                        Text(value)
                    }
                }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton({ onUnavailable("رسید تراکنش") }, Modifier.weight(1f)) { Text("رسید") }
            OutlinedButton({ onUnavailable("پیگیری تراکنش") }, Modifier.weight(1f)) {
                Text("پیگیری")
            }
            Button(onBack) { Text("بازگشت") }
        }
    }
}

/** Strict Persian calendar parsing through the API-24 platform ICU, no new dependency. */
internal fun persianDateMillis(text: String): Long? {
    val normalized = latinDigits(text)
    if (!Regex("[0-9]{4}/[0-9]{2}/[0-9]{2}").matches(normalized)) return null
    val formatter = SimpleDateFormat("yyyy/MM/dd", ULocale("fa_IR@calendar=persian;numbers=latn"))
    formatter.isLenient = false
    val position = ParsePosition(0)
    val date = formatter.parse(normalized, position) ?: return null
    return if (position.index == normalized.length) date.time else null
}

internal fun todayPersian(): String =
    SimpleDateFormat("yyyy/MM/dd", ULocale("fa_IR@calendar=persian;numbers=latn"))
        .format(java.util.Date())

internal fun statementDateError(
    start: String,
    end: String,
    today: String = todayPersian(),
): String? {
    val from = persianDateMillis(start) ?: return "تاریخ ابتدایی را به صورت سال/ماه/روز وارد کنید."
    val to = persianDateMillis(end) ?: return "تاریخ انتهایی را به صورت سال/ماه/روز وارد کنید."
    val now = checkNotNull(persianDateMillis(today))
    return when {
        from > to -> "تاریخ ابتدایی باید قبل از تاریخ انتهایی بازه زمانی باشد."
        from > now || to > now -> "تاریخ انتخابی نباید تاریخ آینده باشد."
        else -> null
    }
}

@Composable
internal fun StatementScreen(
    account: PreviewAccount?,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var accountKey by rememberSaveable { mutableStateOf(account?.key ?: "") }
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var format by rememberSaveable { mutableStateOf("") }
    var choosingFormat by rememberSaveable { mutableStateOf(false) }
    var choosingAccount by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val error =
        if (submitted)
            when {
                accountKey.isEmpty() -> "حساب را انتخاب کنید."
                statementDateError(start, end) != null -> statementDateError(start, end)
                format.isEmpty() -> "نوع فایل را انتخاب کنید."
                else -> null
            }
        else null
    Column(Modifier.fillMaxSize().padding(16.dp).testTag("statement-form")) {
        Box {
            OutlinedButton({ choosingAccount = true }, Modifier.testTag("export-account")) {
                Text(
                    "حساب ${AccountFixture.accounts.firstOrNull { it.key == accountKey }?.number ?: "انتخاب نشده"}"
                )
            }
            DropdownMenu(choosingAccount, { choosingAccount = false }) {
                AccountFixture.accounts.forEach { item ->
                    DropdownMenuItem(
                        { Text(item.number) },
                        {
                            accountKey = item.key
                            choosingAccount = false
                        },
                    )
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box {
                OutlinedButton(
                    { choosingFormat = true },
                    Modifier.fillMaxWidth().testTag("export-format"),
                ) {
                    Text(format.ifEmpty { "نوع فایل" })
                }
                DropdownMenu(choosingFormat, { choosingFormat = false }) {
                    listOf("اکسل صورتحساب", "CSV صورتحساب", "PDF صورتحساب").forEach { label ->
                        DropdownMenuItem(
                            { Text(label) },
                            {
                                format = label
                                choosingFormat = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersianDateField(
                    start,
                    { start = it },
                    "از تاریخ",
                    "statement-start",
                    Modifier.weight(1f),
                )
                PersianDateField(
                    end,
                    { end = it },
                    "تا تاریخ",
                    "statement-end",
                    Modifier.weight(1f),
                )
            }
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("statement-error"),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                {
                    submitted = true
                    if (
                        accountKey.isNotEmpty() &&
                            statementDateError(start, end) == null &&
                            format.isNotEmpty()
                    )
                        onUnavailable("درخواست و دریافت فایل صورتحساب")
                },
                Modifier.weight(1f).testTag("statement-request"),
            ) {
                Text("ارسال درخواست")
            }
            OutlinedButton(onBack) { Text("انصراف") }
        }
    }
}
