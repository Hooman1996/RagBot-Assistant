package org.ragbot.replica

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** AI-STATEMENT: independent synthetic ledger rows; signed exact integers, explicit unit. */
internal data class PreviewStatement(
    val key: String,
    val amount: Long,
    val balance: Long?,
    val date: String,
    val description: String,
    val unit: String = "IRR",
)

internal fun dateDaysAgo(days: Int): String {
    val calendar = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
    calendar.add(Calendar.DAY_OF_MONTH, -days)
    return SimpleDateFormat("yyyy/MM/dd", ULocale("fa_IR@calendar=persian;numbers=latn"))
        .format(calendar)
}

internal fun statementFixtures(account: PreviewAccount?): List<PreviewStatement> =
    listOf(
        PreviewStatement(
            "statement-1",
            2_000_000L,
            account?.balance,
            dateDaysAgo(1),
            "واریز نمایشی",
        ),
        PreviewStatement(
            "statement-2",
            -750_000L,
            account?.balance?.minus(2_000_000L),
            dateDaysAgo(2),
            "برداشت نمایشی",
        ),
        PreviewStatement(
            "statement-3",
            1_000_000L,
            account?.balance?.minus(1_250_000L),
            dateDaysAgo(5),
            "واریز نمایشی دوم",
        ),
    )

internal fun bankStatementDateError(
    start: String,
    end: String,
    today: String = todayPersian(),
): String? {
    statementDateError(start, end, today)?.let {
        return it
    }
    val from = checkNotNull(persianDateMillis(start))
    val to = checkNotNull(persianDateMillis(end))
    val a = latinDigits(start).split('/').map(String::toInt)
    val b = latinDigits(today).split('/').map(String::toInt)
    // AI-STATEMENT: source uses this approximate distance despite its 366-day label.
    val approximateAge = (b[0] - a[0]) * 365 + (b[1] - a[1]) * 30 + b[2] - a[2]
    return when {
        approximateAge > 365 -> "تاریخ شروع نمی‌تواند بیش از ۳۶۶ روز قبل باشد."
        (to - from) / 86_400_000L >= 90 -> "بازه زمانی باید کمتر از ۹۰ روز باشد."
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatementListScreen(account: PreviewAccount?, onStatement: (String) -> Unit) {
    var start by rememberSaveable { mutableStateOf(dateDaysAgo(7)) }
    var end by rememberSaveable { mutableStateOf(todayPersian()) }
    var appliedStart by rememberSaveable { mutableStateOf(start) }
    var appliedEnd by rememberSaveable { mutableStateOf(end) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var shown by rememberSaveable { mutableIntStateOf(2) }
    val rows =
        remember(account?.key) { statementFixtures(account) }
            .filter {
                checkNotNull(persianDateMillis(it.date)) in
                    checkNotNull(persianDateMillis(appliedStart))..checkNotNull(
                            persianDateMillis(appliedEnd)
                        )
            }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersianDateField(start, { start = it }, "از تاریخ", "ledger-start", Modifier.weight(1f))
            Text("تا", Modifier.padding(horizontal = 4.dp))
            PersianDateField(end, { end = it }, "تا تاریخ", "ledger-end", Modifier.weight(1f))
            IconButton(
                {
                    error = bankStatementDateError(start, end)
                    if (error == null) {
                        appliedStart = start
                        appliedEnd = end
                        shown = 2
                    }
                },
                Modifier.testTag("ledger-apply").semantics {
                    contentDescription = "اعمال بازه زمانی"
                },
            ) {
                Text("✓", color = ServiceGreen)
            }
        }
        error?.let {
            Text(
                it,
                Modifier.padding(horizontal = 16.dp).testTag("ledger-error"),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { shown = 2 },
            modifier = Modifier.weight(1f).testTag("ledger-refresh"),
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
                    .testTag("ledger-scroll")
            ) {
                if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.")
                rows.take(shown).forEach { row ->
                    StatementCard(
                        row,
                        Modifier.testTag(row.key).clickable(role = Role.Button) {
                            onStatement(row.key)
                        },
                    )
                }
                if (shown < rows.size)
                    TextButton({ shown += 2 }, Modifier.testTag("ledger-more")) {
                        Text("نمایش بیشتر")
                    }
            }
        }
    }
}

@Composable
private fun StatementCard(row: PreviewStatement, modifier: Modifier = Modifier) {
    val color = if (row.amount > 0) ServiceGreen else Color(0xFFCB3434)
    Surface(
        modifier.fillMaxWidth().padding(6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.5.dp,
    ) {
        Row {
            Box(Modifier.width(4.dp).height(128.dp).background(color))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "${if (row.amount > 0) "واریز" else "برداشت"}  ${persianNumber(row.amount)} ریال",
                    color = color,
                )
                Text(
                    "مانده: ${row.balance?.let(::persianNumber) ?: "نامشخص"} ریال",
                    Modifier.padding(top = 8.dp),
                )
                Text("زمان: ۱۰:۱۵ - ${row.date}", Modifier.padding(top = 4.dp), fontSize = 13.sp)
                Text(row.description, Modifier.padding(top = 4.dp), fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun StatementDetailScreen(account: PreviewAccount?, key: String) {
    val row = remember(key, account?.key) { statementFixtures(account).first { it.key == key } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Text(
            "حساب ${account?.number ?: "—"}",
            Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
        )
        StatementCard(row)
        Text("جزئیات", Modifier.padding(16.dp))
        val fields =
            listOf(
                "شعبه" to "شعبه نمایشی (۰۰۰)",
                "شماره مرجع" to "DEMO-${row.key}",
                (if (row.amount > 0) "شناسه واریز" else "شناسه برداشت") to "DEMO-001",
                (if (row.amount > 0) "نام فرستنده" else "نام گیرنده") to "طرف نمایشی",
                "شماره سند" to "DEMO-002",
                "کد پیگیری" to "DEMO-003",
            )
        fields.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(label, Modifier.weight(1f))
                Text(value)
            }
        }
    }
}

/** Picker calendar is a replica presentation; keyboard entry is an accessible mock alternative. */
@Composable
internal fun PersianDateField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value,
        onValue,
        modifier.testTag(tag),
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        trailingIcon = {
            IconButton(
                { picking = true },
                Modifier.testTag("$tag-calendar").semantics { contentDescription = "تقویم $label" },
            ) {
                Text("▦")
            }
        },
    )
    if (picking) {
        val calendar =
            remember(value) {
                Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply {
                    timeInMillis = persianDateMillis(value) ?: System.currentTimeMillis()
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        var displayedMonth by rememberSaveable { mutableLongStateOf(calendar.timeInMillis) }
        calendar.timeInMillis = displayedMonth
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val offset = (calendar.get(Calendar.DAY_OF_WEEK) + 0) % 7
        val days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({
                        calendar.add(Calendar.MONTH, -1)
                        displayedMonth = calendar.timeInMillis
                    }) {
                        Text("ماه قبل")
                    }
                    Text("${persianNumber(year.toLong())}/$month", fontSize = 14.sp)
                    TextButton({
                        calendar.add(Calendar.MONTH, 1)
                        displayedMonth = calendar.timeInMillis
                    }) {
                        Text("ماه بعد")
                    }
                }
            },
            text = {
                Column {
                    Row {
                        listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
                            Text(it, Modifier.weight(1f))
                        }
                    }
                    (List(offset) { 0 } + (1..days).toList()).chunked(7).forEach { week ->
                        Row {
                            (week + List(7 - week.size) { 0 }).forEach { day ->
                                Box(
                                    Modifier.weight(1f).height(40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (day != 0)
                                        TextButton(
                                            {
                                                onValue(
                                                    "%04d/%02d/%02d"
                                                        .format(
                                                            java.util.Locale.US,
                                                            year,
                                                            month,
                                                            day,
                                                        )
                                                )
                                                picking = false
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier =
                                                Modifier.fillMaxSize().testTag("calendar-day-$day"),
                                        ) {
                                            Text(persianNumber(day.toLong()))
                                        }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ picking = false }) { Text("انصراف") } },
        )
    }
}
