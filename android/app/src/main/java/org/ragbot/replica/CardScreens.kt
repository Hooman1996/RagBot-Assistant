package org.ragbot.replica

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val COPIED = "با موفقیت کپی شد."
private const val EMPTY = "موردی برای نمایش وجود ندارد."

/** CI-TAB: hosted Home card content: card pager, group selector, then the group's action rows. */
@Composable
internal fun CardTab(
    scenario: String,
    kind: CardKind,
    indices: List<Int>,
    shownBalances: List<String>,
    onKind: (CardKind) -> Unit,
    onIndex: (CardKind, Int) -> Unit,
    onBalance: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val context = LocalContext.current
    var copied by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize()
            .background(Color(0xFFFBFBFB), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("card-tab")
    ) {
        if (scenario == "loading" || scenario == "error") {
            PreviewDataState(scenario, onRetry)
            return@Column
        }
        Spacer(Modifier.height(16.dp))
        val cards = if (scenario == "empty") emptyList() else CardFixture.of(kind)
        if (cards.isEmpty()) CardEmptyState()
        else
            key(kind) {
                val pager =
                    rememberPagerState(indices[kind.ordinal].coerceAtMost(cards.lastIndex)) {
                        cards.size
                    }
                LaunchedEffect(pager.settledPage) { onIndex(kind, pager.settledPage) }
                HorizontalPager(pager, Modifier.fillMaxWidth().testTag("card-pager")) { page ->
                    CardFace(
                        cards[page],
                        cards[page].key in shownBalances,
                        { onBalance(cards[page].key) },
                        {
                            copyPreview(context, cards[page].number)
                            copied = true
                        },
                        onUnavailable,
                    )
                }
                if (cards.size > 1)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        cards.indices.forEach {
                            Box(
                                Modifier.padding(2.dp)
                                    .size(6.dp)
                                    .background(
                                        if (it == pager.currentPage) ServiceGreen
                                        else Color(0xFFD9D9D9),
                                        CircleShape,
                                    )
                            )
                        }
                    }
            }
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp)
                .testTag("card-actions")
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CardKind.entries.forEach {
                    FilterChip(
                        it == kind,
                        { onKind(it) },
                        label = { Text(it.title) },
                        modifier = Modifier.testTag("card-kind-${it.name}"),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 10.dp)) {
                CardFixture.actions.getValue(kind).forEach { action ->
                    AccountAction(
                        action.title,
                        "replica_account_list",
                        "card-action-${action.key}",
                    ) {
                        action.destination?.let(onOpen) ?: onUnavailable(action.title)
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }
    if (copied)
        AlertDialog(
            onDismissRequest = { copied = false },
            text = { Text(COPIED) },
            confirmButton = { TextButton({ copied = false }) { Text("بستن") } },
        )
}

/** The recovered pattern overlays a server card colour; this fixed colour is Designed for mock. */
@Composable
private fun CardBackground(credit: Boolean) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(if (credit) Color(0xFF1F4E3A) else Color(0xFF237641)))
    if (BuildConfig.PRIVATE_RESOURCES) {
        val key = if (credit) "replica_card_credit_bg" else "replica_card_normal_bg"
        val id = context.resources.getIdentifier(key, "drawable", context.packageName)
        check(id != 0) { "Missing private card background: $key" }
        Image(
            painterResource(id),
            null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun CardEmptyState() {
    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(18.dp))
            .testTag("card-empty"),
        contentAlignment = Alignment.Center,
    ) {
        CardBackground(false)
        Text(
            "برای دریافت کارت جدید، به منوی «درخواست کارت» و برای افزودن کارت، به منوی «پروفایل > مدیریت کارت‌ها» مراجعه نمایید.",
            Modifier.padding(16.dp),
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun CardFace(
    card: PreviewCard,
    shown: Boolean,
    onBalance: () -> Unit,
    onCopy: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(18.dp))
            .testTag("card-${card.key}")
    ) {
        CardBackground(card.kind == CardKind.CREDIT)
        Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.kind == CardKind.DEBIT) {
                    ReplicaIcon(
                        if (card.isDefault) "replica_account_default"
                        else "replica_account_not_default",
                        if (card.isDefault) "★" else "☆",
                        Modifier.size(24.dp).semantics {
                            contentDescription = if (card.isDefault) "کارت پیش‌فرض" else "کارت عادی"
                        },
                        Color.White,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                IconButton(
                    { onUnavailable("اشتراک‌گذاری کارت") },
                    Modifier.size(24.dp).semantics { contentDescription = "اشتراک‌گذاری کارت" },
                ) {
                    ReplicaIcon("replica_account_share", "↥", Modifier.size(24.dp), Color.White)
                }
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onCopy,
                    Modifier.size(24.dp).testTag("card-copy").semantics {
                        contentDescription = "کپی شماره کارت"
                    },
                ) {
                    ReplicaIcon("replica_card_copy", "⧉", Modifier.size(24.dp), Color.White)
                }
                Spacer(Modifier.weight(1f))
                ReplicaIcon(
                    if (card.altLogo) "replica_card_logo_alt" else "replica_card_logo",
                    "▣",
                    Modifier.height(26.dp).widthIn(max = 90.dp),
                    Color.White,
                )
            }
            Spacer(Modifier.height(8.dp))
            CardNumber(card.number, Color.White, 20)
            Spacer(Modifier.height(4.dp))
            Text(card.owner, color = Color.White, fontSize = 14.sp)
            if (card.kind == CardKind.DEBIT) {
                Spacer(Modifier.height(8.dp))
                CardBalance(card, shown, onBalance, onUnavailable)
            }
        }
    }
}

@Composable
internal fun CardNumber(number: String, color: Color, size: Int, modifier: Modifier = Modifier) {
    Text(
        cardNumberGroups(number),
        modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.Medium,
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
    )
}

/** Recovered states: hidden, shown amount (zero included), and retry when no amount is returned. */
@Composable
private fun CardBalance(
    card: PreviewCard,
    shown: Boolean,
    onBalance: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("card-balance"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            !shown -> {
                ReplicaIcon("replica_card_visible", "◉", Modifier.size(24.dp), Color.White)
                Spacer(Modifier.weight(1f))
                Text(
                    "مشاهده موجودی",
                    Modifier.clickable(role = Role.Button, onClick = onBalance),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            card.cardBalance == null -> {
                Spacer(Modifier.weight(1f))
                TextButton({ onUnavailable("دریافت موجودی این کارت") }) {
                    Text("تلاش مجدد", color = Color.White)
                }
            }
            else -> {
                IconButton(
                    onBalance,
                    Modifier.size(24.dp).semantics { contentDescription = "عدم نمایش موجودی" },
                ) {
                    ReplicaIcon("replica_card_invisible", "◌", Modifier.size(24.dp), Color.White)
                }
                Spacer(Modifier.width(8.dp))
                // The amount is unchanged local data; refreshing re-reads the same fixture.
                ReplicaIcon(
                    "replica_card_refresh",
                    "↻",
                    Modifier.size(24.dp).semantics { contentDescription = "به‌روزرسانی موجودی" },
                    Color.White,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${persianNumber(card.cardBalance)} ریال",
                    color = Color.White,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
internal fun CardPage(
    page: String,
    kind: CardKind,
    scenario: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            CardDestinations.INFO -> "اطلاعات کارت"
            CardDestinations.BON_BALANCE -> "موجودی بن کارت"
            CardDestinations.ANONYMOUS_BALANCE -> "موجودی کارت"
            CardDestinations.LIST -> "لیست کارت‌ها"
            CardDestinations.MANAGEMENT -> "مدیریت کارت‌ها"
            CardDestinations.FETCH -> "فراخوانی کارت های جدید"
            CardDestinations.VISIBILITY -> "وضعیت نمایش کارت"
            CardDestinations.CREDIT -> "جزئیات و بازپرداخت"
            else -> "خلاصه پرونده تسهیلات"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    CardDestinations.INFO -> CardInfo(kind, onUnavailable)
                    CardDestinations.LIST -> LegacyCardList()
                    CardDestinations.MANAGEMENT -> CardManagement(onOpen, onUnavailable)
                    CardDestinations.FETCH -> FetchCards(onBack, onUnavailable)
                    CardDestinations.VISIBILITY -> CardVisibility(onUnavailable)
                    CardDestinations.CREDIT -> CreditDetail(onBack, onUnavailable)
                    CardDestinations.FILE_LOAN -> FileLoans(onUnavailable)
                    else -> SecureBalanceForm(page, onBack, onUnavailable)
                }
            }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    ltr: Boolean = false,
    onCopy: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 14.sp,
            style =
                if (ltr) LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                else LocalTextStyle.current,
        )
        if (onCopy != null)
            IconButton(
                onCopy,
                Modifier.size(32.dp).semantics { contentDescription = "کپی $label" },
            ) {
                ReplicaIcon("replica_card_copy", "⧉", Modifier.size(20.dp), ServiceGreen)
            }
    }
}

@Composable
private fun CardChooser(cards: List<PreviewCard>, selected: String, onSelect: (String) -> Unit) {
    cards.forEach { card ->
        Surface(
            Modifier.fillMaxWidth().testTag("pick-${card.key}").selectable(
                card.key == selected,
                role = Role.RadioButton,
            ) {
                onSelect(card.key)
            },
            shape = RoundedCornerShape(12.dp),
            color = if (card.key == selected) Color(0xFFE3F4E9) else Color.White,
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(card.key == selected, null)
                Spacer(Modifier.weight(1f))
                CardNumber(card.number, Color.Black, 16)
            }
        }
    }
}

/** S-012: own card picker for the incoming card group, then account/IBAN/status rows. */
@Composable
private fun CardInfo(kind: CardKind, onUnavailable: (String) -> Unit) {
    val context = LocalContext.current
    val cards = CardFixture.of(kind)
    var selected by rememberSaveable { mutableStateOf(cards.firstOrNull()?.key.orEmpty()) }
    var copied by rememberSaveable { mutableStateOf(false) }
    val card = cards.firstOrNull { it.key == selected }
    if (card == null) {
        Text(EMPTY)
        return
    }
    CardChooser(cards, selected) { selected = it }
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .testTag("card-info")
    ) {
        InfoRow("شماره حساب اصلی", card.primaryAccount) {
            copyPreview(context, card.primaryAccount)
            copied = true
        }
        card.otherAccounts.forEach { InfoRow("شماره حساب فرعی", it) }
        InfoRow("شماره شبا", card.iban) {
            copyPreview(context, card.iban)
            copied = true
        }
        InfoRow("وضعیت", card.status)
    }
    Button({ onUnavailable("اشتراک‌گذاری اطلاعات کارت") }, Modifier.fillMaxWidth()) {
        Text("اشتراک‌گذاری")
    }
    if (copied)
        AlertDialog(
            onDismissRequest = { copied = false },
            text = { Text(COPIED) },
            confirmButton = { TextButton({ copied = false }) { Text("بستن") } },
        )
}

@Composable
private fun CardRow(card: PreviewCard, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("row-${card.key}"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF237641),
    ) {
        Column(Modifier.padding(16.dp)) {
            CardNumber(card.number, Color.White, 18, Modifier.align(Alignment.CenterHorizontally))
            if (card.locked) Text("قفل شده", color = Color(0xFFFFD54F), fontSize = 12.sp)
            ProvideTextStyle(LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp)) {
                content()
            }
        }
    }
}

@Composable
private fun WhiteRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Text(value)
    }
}

/**
 * S-125: legacy searchable list; recovered row taps have no action. Balance is the record's own
 * field.
 */
@Composable
private fun LegacyCardList() {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        query,
        { query = it },
        Modifier.fillMaxWidth().testTag("card-search"),
        label = { Text("جستجوی شماره کارت") },
        singleLine = true,
    )
    val rows = CardFixture.of(CardKind.DEBIT).filter { cardMatches(it, query) }
    if (rows.isEmpty()) Text(EMPTY)
    rows.forEach { card ->
        CardRow(card) {
            WhiteRow("صاحب کارت", card.owner)
            WhiteRow("شماره مشتری", "۰۰۰۰۰۱")
            WhiteRow("شماره حساب", card.primaryAccount)
            WhiteRow("موجودی", card.accountBalance?.let { "${persianNumber(it)} ریال" } ?: "نامشخص")
            WhiteRow("شماره شبا", card.iban)
        }
    }
}

/**
 * S-126: grouped, searchable management rows. Default/delete/lock are remote changes: unavailable.
 */
@Composable
private fun CardManagement(onOpen: (String) -> Unit, onUnavailable: (String) -> Unit) {
    var kind by rememberSaveable { mutableStateOf(CardKind.DEBIT) }
    var query by rememberSaveable { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CardKind.entries.forEach {
            FilterChip(
                it == kind,
                { kind = it },
                label = { Text(it.title, fontSize = 12.sp) },
                modifier = Modifier.testTag("manage-${it.name}"),
            )
        }
    }
    OutlinedTextField(
        query,
        { query = it },
        Modifier.fillMaxWidth().testTag("manage-search"),
        label = { Text("جستجوی شماره یا صاحب حساب") },
        singleLine = true,
    )
    val rows = CardFixture.of(kind).filter { cardMatches(it, query) }
    if (rows.isEmpty()) {
        Text(EMPTY)
        Text(
            "برای مشاهده و فراخوانی کارت‌های خود دکمه «فراخوانی کارت‌های جدید» را کلیک کنید.",
            fontSize = 12.sp,
        )
    }
    rows.forEach { card ->
        CardRow(card) {
            WhiteRow("صاحب حساب", card.owner)
            WhiteRow("شماره حساب اصلی", card.primaryAccount)
            Row {
                TextButton({ onUnavailable("انتخاب کارت پیش‌فرض") }) {
                    Text(if (card.isDefault) "★ پیش‌فرض" else "☆ پیش‌فرض", color = Color.White)
                }
                TextButton({ onUnavailable(if (card.locked) "رفع قفل کارت" else "قفل کارت") }) {
                    Text(if (card.locked) "رفع قفل" else "قفل", color = Color.White)
                }
                TextButton({ onUnavailable("حذف کارت") }) { Text("حذف", color = Color.White) }
            }
        }
    }
    Button({ onOpen(CardDestinations.FETCH) }, Modifier.fillMaxWidth().testTag("fetch-cards")) {
        Text("فراخوانی کارت های جدید")
    }
}

/**
 * S-068: local checkbox draft. Adding cards to a profile is a remote change and stays unavailable.
 */
@Composable
private fun FetchCards(onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    var checked by rememberSaveable { mutableStateOf(listOf<String>()) }
    Text("لطفا یک یا چند کارت دلخواه خود را برای مشاهده در سامانه انتخاب کنید:", fontSize = 14.sp)
    TextButton(
        { checked = CardFixture.candidates.map { it.key } },
        Modifier.testTag("select-all"),
    ) {
        Text("انتخاب همه")
    }
    CardFixture.candidates.forEach { card ->
        Row(
            Modifier.fillMaxWidth()
                .testTag("candidate-${card.key}")
                .toggleable(card.key in checked, role = Role.Checkbox) {
                    checked = if (it) checked + card.key else checked - card.key
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(card.key in checked, null)
            Spacer(Modifier.weight(1f))
            CardNumber(card.number, Color.Black, 16)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            { onUnavailable("افزودن کارت") },
            Modifier.weight(1f).testTag("fetch-ok"),
            enabled = checked.isNotEmpty(),
        ) {
            Text("تایید")
        }
        OutlinedButton(onBack, Modifier.weight(1f)) { Text("انصراف") }
    }
}

/** S-130: the switch is a remote card preference; it explains the limit and keeps its state. */
@Composable
private fun CardVisibility(onUnavailable: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("نمایش کارت‌ها", Modifier.weight(1f))
        Switch(true, { onUnavailable("تغییر وضعیت نمایش کارت") }, Modifier.testTag("card-visible"))
    }
    Text(
        "در وضعیت عدم نمایش کارت، برای انجام عملیات مربوط به کارت، شماره کارت مبدا توسط شما وارد می‌گردد.",
        fontSize = 12.sp,
        color = Color.Gray,
    )
}

private fun firstCredit() = CardFixture.of(CardKind.CREDIT).firstOrNull()

/** S-131: recovered behavior reads the first credit card, not the tab's current card. */
@Composable
private fun CreditDetail(onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    val card = firstCredit()
    val summary = card?.let { CardFixture.credit[it.key] }
    if (card == null || summary == null) {
        Text(EMPTY)
        return
    }
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .testTag("credit-summary")
    ) {
        InfoRow("کارت", cardNumberGroups(card.number), ltr = true)
        InfoRow("صاحب", card.owner)
        InfoRow("وضعیت کارت", summary.state)
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = HomeGreen)
        InfoRow("سقف اعتبار", "${persianNumber(summary.creditLimit)} ریال")
        InfoRow("مانده اعتبار", "${persianNumber(summary.remainingCredit)} ریال")
        InfoRow("مبلغ مصرف شده", "${persianNumber(summary.usedAmount)} ریال")
        InfoRow("بدهی اقساط معوق", "${persianNumber(summary.overdueDebt)} ریال")
        InfoRow("مبلغ تسویه کل", "${persianNumber(summary.totalPayable)} ریال")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            { onUnavailable("پرداخت بدهی کارت اعتباری") },
            Modifier.weight(1f).testTag("credit-pay"),
            enabled = summary.totalPayable > 0,
        ) {
            Text("پرداخت")
        }
        OutlinedButton(onBack, Modifier.weight(1f)) { Text("انصراف") }
    }
}

/**
 * S-134: file summaries for the first credit card; opening a file belongs to a later loan batch.
 */
@Composable
private fun FileLoans(onUnavailable: (String) -> Unit) {
    val loans = firstCredit()?.let { CardFixture.fileLoans[it.key] }.orEmpty()
    if (loans.isEmpty()) Text(EMPTY)
    loans.forEach { loan ->
        Surface(
            Modifier.fillMaxWidth().testTag("loan-${loan.fileNumber}").clickable(
                role = Role.Button
            ) {
                onUnavailable("جزئیات پرونده تسهیلات")
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "پرونده ${loan.fileNumber}",
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                    )
                    ReplicaIcon("replica_card_arrow", "‹", Modifier.size(20.dp), ServiceGreen)
                }
                InfoRow("مانده بدهی", "${persianNumber(loan.remaining)} ریال")
                InfoRow("تاریخ بازپرداخت", loan.dueDate)
            }
        }
    }
}

/** S-119/S-120: recovered fields and checks only. No PIN is processed and no balance is shown. */
@Composable
private fun SecureBalanceForm(page: String, onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    val anonymous = page == CardDestinations.ANONYMOUS_BALANCE
    var number by rememberSaveable { mutableStateOf("") }
    var cvv2 by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var save by rememberSaveable { mutableStateOf(false) }
    var errors by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val digits = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    @Composable
    fun Field(
        key: String,
        label: String,
        value: String,
        secret: Boolean,
        onChange: (String) -> Unit,
    ) {
        OutlinedTextField(
            value,
            onChange,
            Modifier.fillMaxWidth().testTag("secure-$key"),
            label = { Text(label) },
            singleLine = true,
            isError = key in errors,
            supportingText = errors[key]?.let { { Text(it) } },
            keyboardOptions =
                if (key == "expiry") KeyboardOptions(keyboardType = KeyboardType.Number)
                else digits,
            visualTransformation =
                if (secret) PasswordVisualTransformation()
                else androidx.compose.ui.text.input.VisualTransformation.None,
        )
    }
    if (anonymous) Field("number", "شماره کارت", number, false) { number = it.take(16) }
    else CardFixture.of(CardKind.BON).firstOrNull()?.let { CardNumber(it.number, Color.Black, 18) }
    Field("cvv2", "CVV2", cvv2, true) { cvv2 = it.take(4) }
    Field("expiry", "تاریخ انقضا (ماه/سال)", expiry, false) { expiry = it.take(5) }
    Field("password", if (anonymous) "رمز پویا" else "رمز دوم ثابت", password, true) {
        password = it.take(12)
    }
    if (!anonymous)
        Row(
            Modifier.toggleable(save, role = Role.Checkbox) { save = it },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(save, null)
            Text("ذخیره اطلاعات کارت")
        }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            {
                errors = secureFormErrors(if (anonymous) number else null, cvv2, expiry, password)
                if (errors.isEmpty()) onUnavailable("استعلام امن موجودی کارت")
            },
            Modifier.weight(1f).testTag("secure-ok"),
        ) {
            Text("تایید")
        }
        OutlinedButton(onBack, Modifier.weight(1f)) { Text("انصراف") }
    }
}
