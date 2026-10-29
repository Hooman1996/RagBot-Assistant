package org.ragbot.replica

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PreviewScenarios(scenario: String, onSelect: (String) -> Unit) {
    Text("سناریوی محلی — طراحی برای ماک", fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("populated" to "داده", "empty" to "خالی", "loading" to "بارگذاری", "error" to "خطا")
            .forEach { (key, title) ->
                FilterChip(
                    scenario == key,
                    { onSelect(key) },
                    label = { Text(title, fontSize = 11.sp) },
                    modifier = Modifier.testTag("scenario-$key"),
                )
            }
    }
}

@Composable
internal fun AccountTab(
    account: PreviewAccount?,
    count: Int,
    balanceVisible: Boolean,
    scenario: String,
    onList: () -> Unit,
    onBalance: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    if (scenario != "populated") {
        Surface(
            Modifier.fillMaxSize(),
            color = Color(0xFFFBFBFB),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            PreviewDataState(scenario, onRetry)
        }
        return
    }
    Spacer(Modifier.height(34.dp))
    val context = LocalContext.current
    var copied by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxWidth().height(220.dp).testTag("account-header")) {
        if (BuildConfig.PRIVATE_RESOURCES) {
            val id =
                context.resources.getIdentifier(
                    "replica_account_background",
                    "drawable",
                    context.packageName,
                )
            check(id != 0)
            Image(
                painterResource(id),
                null,
                Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            )
        }
        Column(
            Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ReplicaIcon(
                    if (account?.key == "account-1") "replica_account_default"
                    else "replica_account_not_default",
                    "☆",
                    Modifier.size(24.dp),
                    Color.White,
                )
                Spacer(Modifier.width(12.dp))
                IconButton(
                    { onUnavailable("اشتراک‌گذاری حساب") },
                    Modifier.size(24.dp).semantics { contentDescription = "اشتراک‌گذاری حساب" },
                ) {
                    ReplicaIcon("replica_account_share", "↥", Modifier.size(24.dp), Color.White)
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    { onUnavailable("سایر خدمات حساب") },
                    Modifier.testTag("account-more").size(24.dp).semantics {
                        contentDescription = "سایر خدمات حساب"
                    },
                ) {
                    ReplicaIcon("replica_account_more", "⋯", Modifier.size(24.dp), Color.White)
                }
            }
            AccountValue("صاحب حساب", account?.owner ?: "—")
            AccountValue("شماره مشتری", "۰۰۰۰۰۱")
            AccountValue("شماره حساب", account?.number ?: "—") {
                copied = "شماره حساب"
                copyPreview(context, account?.number.orEmpty())
            }
            AccountValue("شماره شبا", account?.iban ?: "—") {
                copied = "شماره شبا"
                copyPreview(context, account?.iban.orEmpty())
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onBalance,
                Modifier.align(Alignment.CenterHorizontally)
                    .heightIn(min = 36.dp)
                    .testTag("account-balance"),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    if (balanceVisible)
                        "${account?.balance?.let(::persianNumber) ?: "نامشخص"} ریال · عدم نمایش"
                    else "مشاهده موجودی",
                    color = Color(0xFF052D15),
                )
            }
        }
    }
    Column(
        Modifier.fillMaxSize()
            .background(Color(0xFFFBFBFB), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag("account-tab")
    ) {
        if (count > 1)
            AccountAction("لیست و تغییر حساب", "replica_account_list", "account-list", onList)
        // AI-MENU: deterministic preview configuration; original server ordering is unknown.
        AccountAction("صورتحساب", "replica_quick_statement", "account-statement") {
            onOpen(AccountDestinations.STATEMENT_LIST)
        }
        AccountAction("مدیریت حساب", "replica_account_list", "account-management") {
            onOpen(AccountDestinations.MANAGEMENT)
        }
    }
    copied?.let { label ->
        AlertDialog(
            onDismissRequest = { copied = null },
            text = { Text("$label کپی شد") },
            confirmButton = { TextButton({ copied = null }) { Text("بستن") } },
        )
    }
}

private fun copyPreview(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
        ClipData.newPlainText("حساب نمایشی", text)
    )
}

@Composable
private fun AccountValue(
    label: String,
    value: String,
    fontSize: TextUnit = 12.sp,
    onCopy: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = fontSize)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = fontSize)
        if (onCopy != null)
            IconButton(
                onCopy,
                Modifier.size(20.dp).semantics { contentDescription = "کپی $label" },
            ) {
                ReplicaIcon("replica_account_copy", "⧉", Modifier.size(16.dp), Color.White)
            }
    }
}

@Composable
private fun AccountAction(title: String, icon: String, tag: String, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag(tag)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ReplicaIcon(icon, "▤", Modifier.size(20.dp), ServiceGreen)
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontSize = 14.sp)
            ReplicaIcon("replica_account_next", "‹", Modifier.size(20.dp), Color.Gray)
        }
    }
}

@Composable
internal fun PreviewDataState(scenario: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag("data-$scenario"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (scenario) {
            "loading" -> {
                CircularProgressIndicator()
                Text("در حال دریافت اطلاعات")
            }
            "error" -> {
                Text("دریافت اطلاعات ممکن نشد.")
                TextButton(onRetry) { Text("تلاش مجدد") }
            }
            else -> Text("موردی برای نمایش وجود ندارد.")
        }
    }
}

@Composable
internal fun AccountPage(
    page: String,
    account: PreviewAccount?,
    selectedKey: String,
    visibleKeys: List<String>,
    scenario: String,
    transactionKey: String,
    statementKey: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSelect: (String) -> Unit,
    onApply: (List<String>) -> Unit,
    onTransaction: (String) -> Unit,
    onStatement: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            AccountDestinations.LIST -> "لیست و تغییر حساب"
            AccountDestinations.MANAGEMENT -> "مدیریت حساب"
            AccountDestinations.FETCH -> "فراخوانی حساب های جدید"
            AccountDestinations.STATEMENT -> "درخواست صورتحساب"
            AccountDestinations.STATEMENT_LIST -> "صورتحساب"
            AccountDestinations.STATEMENT_DETAIL -> "جزئیات صورتحساب"
            AccountDestinations.HISTORY -> "سوابق تراکنش‌ها"
            else -> "جزئیات تراکنش"
        }
    Column(
        Modifier.fillMaxSize()
            .background(Color(0xFFFBFBFB))
            .safeDrawingPadding()
            .imePadding()
            .testTag(page)
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF145D32)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onBack,
                Modifier.testTag("page-back").semantics { contentDescription = "بازگشت" },
            ) {
                ReplicaIcon(
                    "replica_account_next",
                    "›",
                    Modifier.size(24.dp).rotate(180f),
                    Color.White,
                )
            }
            Text(title, color = Color.White, fontSize = 18.sp)
            if (page == AccountDestinations.STATEMENT) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    { onUnavailable("درخواست‌های دریافت فایل") },
                    Modifier.testTag("export-downloads").semantics {
                        contentDescription = "درخواست‌های دریافت فایل"
                    },
                ) {
                    ReplicaIcon("replica_export_download", "⇩", Modifier.size(28.dp), Color.White)
                }
            }
        }
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
            return
        }
        when (page) {
            AccountDestinations.LIST,
            AccountDestinations.MANAGEMENT,
            AccountDestinations.FETCH ->
                AccountList(
                    page,
                    selectedKey,
                    visibleKeys,
                    onBack,
                    onOpen,
                    onSelect,
                    onApply,
                    onUnavailable,
                )
            AccountDestinations.STATEMENT_LIST -> StatementListScreen(account, onStatement)
            AccountDestinations.STATEMENT_DETAIL -> StatementDetailScreen(account, statementKey)
            AccountDestinations.HISTORY -> HistoryScreen(onTransaction)
            AccountDestinations.TRANSACTION ->
                TransactionDetail(transactionKey, onBack, onUnavailable)
            AccountDestinations.STATEMENT -> StatementScreen(account, onBack, onUnavailable)
        }
    }
}

@Composable
private fun AccountList(
    page: String,
    selectedKey: String,
    visibleKeys: List<String>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSelect: (String) -> Unit,
    onApply: (List<String>) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(visibleKeys.toList()) }
    var ownership by rememberSaveable { mutableStateOf("انفرادی") }
    val fetching = page == AccountDestinations.FETCH
    val managing = page == AccountDestinations.MANAGEMENT
    val candidates =
        if (fetching) AccountFixture.accounts
        else AccountFixture.accounts.filter { it.key in visibleKeys }
    val grouped = managing || fetching || candidates.map { it.ownership }.distinct().size > 1
    val filtered =
        candidates.filter { accountMatches(it, query) && (!grouped || it.ownership == ownership) }
    Column(Modifier.fillMaxSize()) {
        if (grouped)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("انفرادی", "مشترک", "شرکتی").forEach { label ->
                    FilterChip(
                        ownership == label,
                        { ownership = label },
                        label = { Text(label) },
                        modifier = Modifier.testTag("ownership-$label"),
                    )
                }
            }
        if (fetching)
            Text(
                "لطفا یک یا چند حساب دلخواه خود را برای مشاهده در سامانه انتخاب کنید:",
                Modifier.padding(16.dp),
                fontSize = 14.sp,
            )
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth().padding(16.dp).testTag("account-search"),
            label = { Text(if (fetching) "جستجوی شماره حساب" else "جستجوی شماره یا صاحب حساب") },
            singleLine = true,
        )
        if (!fetching && !managing)
            Text(
                "به منظور تغییر، حساب مورد نظر خود را انتخاب کنید.",
                Modifier.padding(horizontal = 16.dp),
                fontSize = 12.sp,
                color = Color.Gray,
            )
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("accounts-scroll")
        ) {
            if (filtered.isEmpty()) Text("موردی برای نمایش وجود ندارد.")
            filtered.forEach { item ->
                val selected =
                    if (fetching) item.key in checked
                    else if (managing) item.key == "account-1" else item.key == selectedKey
                Surface(
                    Modifier.fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("choose-${item.key}")
                        .selectable(
                            selected,
                            role = if (fetching) Role.Checkbox else Role.RadioButton,
                        ) {
                            if (fetching)
                                checked = if (selected) checked - item.key else checked + item.key
                            else if (managing) onUnavailable("انتخاب حساب پیش‌فرض")
                            else onSelect(item.key)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF237641),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (fetching) {
                                    if (selected) "☑" else "☐"
                                } else if (managing) {
                                    if (selected) "★" else "☆"
                                } else "",
                                color = Color.White,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(item.number, color = Color.White)
                        }
                        if (!fetching && !managing)
                            AccountValue(
                                "موجودی",
                                "${item.balance?.let(::persianNumber) ?: "نامشخص"} ریال",
                                14.sp,
                            )
                        AccountValue("صاحب حساب", item.owner, 14.sp)
                        if (!fetching && !managing) AccountValue("شماره مشتری", "۰۰۰۰۰۱", 14.sp)
                        AccountValue("دسته بندی", "سپرده نمایشی", 14.sp)
                        AccountValue("نوع حساب", item.type, 14.sp)
                        if (!fetching && !managing) AccountValue("شماره شبا", item.iban, 14.sp)
                        if (managing)
                            Row {
                                TextButton({ onUnavailable("حذف حساب") }) {
                                    Text("حذف", color = Color.White)
                                }
                                TextButton({ onUnavailable("انتخاب حساب پیش‌فرض") }) {
                                    Text("حساب پیش‌فرض", color = Color.White)
                                }
                            }
                    }
                }
            }
        }
        if (managing)
            Button(
                { onOpen(AccountDestinations.FETCH) },
                Modifier.fillMaxWidth().padding(16.dp).testTag("fetch-accounts"),
            ) {
                Text("فراخوانی حساب های جدید")
            }
        if (fetching)
            Text(
                "انتخاب‌ها فقط در این نسخه نمایشی ذخیره می‌شوند.",
                Modifier.padding(horizontal = 16.dp),
                fontSize = 12.sp,
            )
        if (fetching)
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    { onApply(checked) },
                    Modifier.weight(1f).testTag("apply-accounts"),
                    enabled = checked.isNotEmpty(),
                ) {
                    Text("تأیید")
                }
                OutlinedButton(onBack) { Text("انصراف") }
            }
    }
}
