package org.ragbot.replica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // No incoming Intent data, extras, nested intents or deep links are interpreted.
        setContent { ReplicaApp() }
    }
}

@Composable
fun ReplicaApp() {
    var entered by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var balanceVisible by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var serviceOrder by rememberSaveable {
        mutableStateOf<List<String>>(HomeFixture.services.map { it.key })
    }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val navigation = remember { HomeNavigation() }
    val savedPages = rememberSaveableStateHolder()
    val homeScroll = rememberScrollState()
    var stack by rememberSaveable { mutableStateOf(listOf(HomeDestinations.HOME)) }
    var tab by rememberSaveable { mutableStateOf(HomeControl.HOME) }
    var cardKind by rememberSaveable { mutableStateOf(CardKind.DEBIT) }
    var cardIndices by rememberSaveable { mutableStateOf(List(CardKind.entries.size) { 0 }) }
    var shownBalances by rememberSaveable { mutableStateOf(listOf<String>()) }
    var cardRequest by rememberSaveable { mutableStateOf<CardRequest?>(null) }
    var accountKey by rememberSaveable { mutableStateOf("account-1") }
    var visibleAccounts by rememberSaveable {
        mutableStateOf<List<String>>(listOf("account-1", "account-2"))
    }
    var scenario by rememberSaveable { mutableStateOf("populated") }
    var transactionKey by rememberSaveable { mutableStateOf("preview-1") }
    var statementKey by rememberSaveable { mutableStateOf("statement-1") }
    val account = AccountFixture.accounts.firstOrNull { it.key == accountKey }
    val page = stack.last()
    fun open(id: String) {
        check(id in HomeDestinations.implemented)
        stack = stack + id
    }
    fun goBack() {
        savedPages.removeState(stack.last())
        stack = stack.dropLast(1)
    }
    // Cancel leaves the whole request flow, as the recovered pages return to Home.
    fun leaveRequest() {
        stack.filter { it in CardRequestDestinations.implemented }.forEach(savedPages::removeState)
        stack = stack.filter { it !in CardRequestDestinations.implemented }
        cardRequest = null
    }
    fun unavailable(title: String) {
        notice = "$title در این پیش‌نمایش آماده نیست. در همین صفحه می‌مانید."
    }
    val activity = checkNotNull(LocalActivity.current)
    val homeVisible = entered && page == HomeDestinations.HOME
    SideEffect {
        val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        bars.isAppearanceLightStatusBars = !homeVisible
        bars.isAppearanceLightNavigationBars = !homeVisible
    }
    val regular = replicaFont("replica_regular")
    val medium = replicaFont("replica_medium")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme =
                lightColorScheme(
                    primary = Color(0xFF308A48),
                    secondaryContainer = Color(0xFFE3F4E9),
                    onSecondaryContainer = Color(0xFF145D32),
                )
        ) {
            ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = regular)) {
                BackHandler(entered || notice != null) {
                    when {
                        notice != null -> notice = null
                        stack.size > 1 -> goBack()
                        tab != HomeControl.HOME -> tab = HomeControl.HOME
                        editing -> editing = false
                        expanded -> expanded = false
                        else -> entered = false
                    }
                }
                if (!entered) {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("نسخه نمایشی", fontSize = 24.sp)
                        Text("داده‌ها ساختگی و محلی هستند. ورود بانکی لازم نیست.")
                        Text(
                            if (BuildConfig.PRIVATE_RESOURCES) "دارایی‌های خصوصی محلی"
                            else "قلم، نمادها، بنر و عنوان عمومی؛ تطابق ظاهری تأیید نشده است."
                        )
                        Text("ساخت: ${BuildConfig.SOURCE_REVISION}", Modifier.testTag("source"))
                        PreviewScenarios(scenario) { scenario = it }
                        Text(
                            "ورود مستقل برای بازبینی؛ مسیر اصلی هنوز بازیابی نشده",
                            fontSize = 11.sp,
                        )
                        Row {
                            TextButton(
                                {
                                    entered = true
                                    open(AccountDestinations.HISTORY)
                                },
                                Modifier.testTag("demo-history"),
                            ) {
                                Text("سوابق قدیمی")
                            }
                            TextButton(
                                {
                                    entered = true
                                    open(AccountDestinations.STATEMENT)
                                },
                                Modifier.testTag("demo-statement"),
                            ) {
                                Text("درخواست صورتحساب")
                            }
                        }
                        Row {
                            listOf(
                                    CardDestinations.MANAGEMENT to "مدیریت کارت",
                                    CardDestinations.VISIBILITY to "نمایش کارت",
                                    CardDestinations.ANONYMOUS_BALANCE to "موجودی بدون ورود",
                                )
                                .forEach { (id, title) ->
                                    TextButton(
                                        {
                                            entered = true
                                            open(id)
                                        },
                                        Modifier.testTag("demo-$id"),
                                    ) {
                                        Text(title, fontSize = 12.sp)
                                    }
                                }
                        }

                        Button(
                            onClick = { entered = navigation.navigate(HomeDestinations.HOME) },
                            Modifier.testTag("demo-entry"),
                        ) {
                            Text("ورود به خانه")
                        }
                    }
                } else {
                    savedPages.SaveableStateProvider(page) {
                        if (page == HomeDestinations.HOME) {
                            HomeFrame(
                                expanded,
                                balanceVisible,
                                serviceOrder,
                                medium,
                                { expanded = !expanded },
                                { editing = true },
                                { item ->
                                    notice =
                                        "${item.title} در این پیش‌نمایش آماده نیست. در خانه می‌مانید."
                                },
                                { unavailable("جزئیات تراکنش خانه") },
                                account,
                                tab,
                                homeScroll,
                                {
                                    if (tab == HomeControl.CARD)
                                        CardTab(
                                            scenario,
                                            cardKind,
                                            cardIndices,
                                            shownBalances,
                                            { cardKind = it },
                                            { kind, index ->
                                                cardIndices =
                                                    cardIndices.toMutableList().also {
                                                        it[kind.ordinal] = index
                                                    }
                                            },
                                            { key ->
                                                shownBalances =
                                                    if (key in shownBalances) shownBalances - key
                                                    else shownBalances + key
                                            },
                                            { scenario = "populated" },
                                            { action ->
                                                val id = checkNotNull(action.destination)
                                                if (id in CardRequestDestinations.implemented)
                                                    cardRequest =
                                                        CardRequest(
                                                            CardOperation.valueOf(
                                                                action.key.uppercase()
                                                            ),
                                                            if (id == CardRequestDestinations.ISSUE)
                                                                accountKey
                                                            else
                                                                CardFixture.of(CardKind.DEBIT)[
                                                                        cardIndices[0]]
                                                                    .key,
                                                        )
                                                open(id)
                                            },
                                            ::unavailable,
                                        )
                                    else
                                        AccountTab(
                                            account,
                                            visibleAccounts.size,
                                            balanceVisible,
                                            scenario,
                                            { open(AccountDestinations.LIST) },
                                            { balanceVisible = !balanceVisible },
                                            { scenario = "populated" },
                                            ::unavailable,
                                            ::open,
                                        )
                                },
                            ) { control ->
                                when (control) {
                                    HomeControl.BALANCE -> balanceVisible = !balanceVisible
                                    HomeControl.HOME,
                                    HomeControl.ACCOUNT,
                                    HomeControl.CARD -> tab = control
                                    HomeControl.ACCOUNT_MANAGEMENT ->
                                        open(AccountDestinations.MANAGEMENT)
                                    else -> notice = navigation.unavailable(control)
                                }
                            }
                        } else if (page in CardRequestDestinations.implemented) {
                            CardRequestPage(
                                page,
                                checkNotNull(cardRequest),
                                scenario,
                                { cardRequest = it },
                                ::goBack,
                                ::open,
                                ::leaveRequest,
                                { scenario = "populated" },
                                ::unavailable,
                            )
                        } else if (page in CardDestinations.implemented) {
                            CardPage(
                                page,
                                cardKind,
                                scenario,
                                ::goBack,
                                ::open,
                                { scenario = "populated" },
                                ::unavailable,
                            )
                        } else {
                            AccountPage(
                                page,
                                account,
                                accountKey,
                                visibleAccounts,
                                scenario,
                                transactionKey,
                                statementKey,
                                ::goBack,
                                ::open,
                                { key ->
                                    accountKey = key
                                    balanceVisible = false
                                    goBack()
                                },
                                { keys ->
                                    visibleAccounts = keys.toList()
                                    if (accountKey !in keys) {
                                        accountKey = keys.first()
                                        balanceVisible = false
                                    }
                                    goBack()
                                },
                                { key ->
                                    transactionKey = key
                                    open(AccountDestinations.TRANSACTION)
                                },
                                { key ->
                                    statementKey = key
                                    open(AccountDestinations.STATEMENT_DETAIL)
                                },
                                { scenario = "populated" },
                                ::unavailable,
                            )
                        }
                    }
                    if (editing) {
                        HomeMenuEditor(
                            serviceOrder,
                            {
                                serviceOrder = it.toList()
                                editing = false
                            },
                            { editing = false },
                            { title ->
                                notice = "$title در این پیش‌نمایش آماده نیست. در خانه می‌مانید."
                            },
                        )
                    }
                }
                notice?.let { message ->
                    AlertDialog(
                        onDismissRequest = { notice = null },
                        title = { Text("پیش‌نمایش محدود") },
                        text = { Text(message, Modifier.testTag("unavailable")) },
                        confirmButton = {
                            TextButton(onClick = { notice = null }) {
                                Text(
                                    if (page == HomeDestinations.HOME && tab == HomeControl.HOME)
                                        "بازگشت به خانه"
                                    else "بستن"
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFrame(
    expanded: Boolean,
    balanceVisible: Boolean,
    serviceOrder: List<String>,
    medium: FontFamily,
    toggleHeader: () -> Unit,
    onEdit: () -> Unit,
    onMenu: (HomeMenuItem) -> Unit,
    onTransaction: () -> Unit,
    account: PreviewAccount?,
    tab: HomeControl,
    homeScroll: androidx.compose.foundation.ScrollState,
    tabContent: @Composable () -> Unit,
    onControl: (HomeControl) -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF145D32),
                    .3f to Color(0xFF052D15),
                    1f to Color(0xFF011409),
                )
            )
    ) {
        if (BuildConfig.PRIVATE_RESOURCES) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().alpha(.6f)) {
                PrivatePattern("replica_pattern_start")
                Spacer(Modifier.weight(1f))
                PrivatePattern("replica_pattern_end")
            }
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding().testTag(HomeDestinations.HOME)) {
            Row(
                Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.padding(start = 8.dp)
                        .size(48.dp)
                        .clickable(role = Role.Button) { onControl(HomeControl.PROFILE) }
                        .semantics { contentDescription = HomeControl.PROFILE.title },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(36.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(32.dp).background(Color(0xFFFBFBFB), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            ReplicaIcon(
                                "replica_avatar_person",
                                "○",
                                Modifier.size(24.dp),
                                Color(0xFFA5A5A5),
                            )
                        }
                    }
                }
                ControlIcon(
                    HomeControl.NOTIFICATIONS,
                    "replica_notification",
                    onControl,
                    Color.White,
                )
                Spacer(Modifier.weight(1f))
                ControlIcon(HomeControl.SHORTCUTS, "replica_bookmark", onControl, Color.White)
                ControlIcon(HomeControl.SEARCH, "replica_search", onControl, Color.White)
                ControlIcon(HomeControl.SUPPORT, "replica_message", onControl, Color.White)
                ControlIcon(HomeControl.LOGOUT, "replica_logout", onControl, Color.White)
                Spacer(Modifier.width(4.dp))
            }
            if (tab != HomeControl.HOME) {
                Column(Modifier.weight(1f).fillMaxWidth()) { tabContent() }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.testTag("balance")
                            .clickable(role = Role.Button) { onControl(HomeControl.BALANCE) }
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReplicaIcon("replica_eye", "◉", Modifier.size(24.dp), HomeGreen)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (balanceVisible)
                                "${account?.balance?.let(::persianNumber) ?: "نامشخص"} ریال · عدم نمایش"
                            else HomeControl.BALANCE.title,
                            color = HomeGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = replicaFont("replica_bold"),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier.clickable(role = Role.Button) {
                                onControl(HomeControl.INCREASE)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReplicaIcon("replica_add", "⊕", Modifier.size(24.dp), HomeGreen)
                            Spacer(Modifier.width(2.dp))
                            Text("افزایش موجودی", color = HomeGreen, fontSize = 14.sp)
                        }
                        Text(
                            "شماره حساب ${account?.number ?: "انتخاب نشده"}",
                            Modifier.testTag("account-number"),
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    }
                    if (expanded) {
                        Column(
                            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp)
                        ) {
                            listOf(
                                    "صاحب حساب: ${account?.owner ?: "—"}",
                                    "نوع حساب: ${account?.type ?: "—"}",
                                    "شماره شبا: ${account?.iban ?: "—"}",
                                )
                                .forEach {
                                    Text(
                                        it,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = { onControl(HomeControl.ACCOUNT_MANAGEMENT) }
                                ) {
                                    Text(HomeControl.ACCOUNT_MANAGEMENT.title, color = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                ControlIcon(
                                    HomeControl.SHARE,
                                    "replica_share",
                                    onControl,
                                    Color.White,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier.size(48.dp, 6.dp)
                            .testTag("expand-account")
                            .semantics {
                                contentDescription = "جزئیات حساب"
                                stateDescription = if (expanded) "باز" else "بسته"
                            }
                            .clickable(role = Role.Button, onClick = toggleHeader),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Tapping this chevron is a deliberate preview affordance; see the report.
                        ReplicaIcon(
                            "replica_down_arrow",
                            "⌄",
                            Modifier.size(12.dp, 6.dp).rotate(if (expanded) 180f else 0f),
                            Color.White,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xFFFBFBFB))
                        .verticalScroll(homeScroll)
                        .testTag("home-scroll")
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp)
                ) {
                    HomeContent(
                        serviceOrder.map { key -> HomeFixture.services.first { it.key == key } },
                        onMenu,
                        onEdit,
                        onTransaction,
                    )
                }
            }
            Surface(
                modifier =
                    Modifier.background(
                            Color.Black.copy(alpha = .04f),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        )
                        .padding(top = 1.6.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.White,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BottomTab(
                        HomeControl.HOME,
                        if (tab == HomeControl.HOME) "replica_home_selected"
                        else "replica_home_unselected",
                        tab == HomeControl.HOME,
                        medium,
                        onControl,
                    )
                    BottomTab(
                        HomeControl.ACCOUNT,
                        if (tab == HomeControl.ACCOUNT) "replica_account_selected"
                        else "replica_accounts",
                        tab == HomeControl.ACCOUNT,
                        medium,
                        onControl,
                    )
                    Box(
                        Modifier.size(56.dp)
                            .background(Color.White, CircleShape)
                            .clickable(role = Role.Button) { onControl(HomeControl.QR) }
                            .semantics { contentDescription = HomeControl.QR.title }
                            .testTag("control-QR"),
                        contentAlignment = Alignment.Center,
                    ) {
                        ReplicaIcon("replica_barcode", "▦", Modifier.size(50.dp), Color(0xFF308A48))
                    }
                    BottomTab(
                        HomeControl.CARD,
                        if (tab == HomeControl.CARD) "replica_card_selected" else "replica_cards",
                        tab == HomeControl.CARD,
                        medium,
                        onControl,
                    )
                    BottomTab(HomeControl.PROFILE, "replica_profile", false, medium, onControl)
                }
            }
        }
    }
}

@Composable
private fun BottomTab(
    control: HomeControl,
    icon: String,
    selected: Boolean,
    medium: FontFamily,
    onControl: (HomeControl) -> Unit,
) {
    val color = if (selected) Color(0xFF308A48) else Color(0xFFA5A5A5)
    Column(
        Modifier.width(56.dp)
            .heightIn(min = 56.dp)
            .selectable(selected, role = Role.Tab) { onControl(control) }
            .testTag("control-${control.name}")
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReplicaIcon(
            icon,
            when (control) {
                HomeControl.HOME -> "⌂"
                HomeControl.ACCOUNT -> "▤"
                HomeControl.CARD -> "▭"
                else -> "○"
            },
            Modifier.size(24.dp),
            color,
        )
        Text(
            control.title,
            color = color,
            fontSize = 12.sp,
            fontFamily = if (selected) medium else LocalTextStyle.current.fontFamily,
        )
    }
}

@Composable
private fun ControlIcon(
    control: HomeControl,
    icon: String,
    onControl: (HomeControl) -> Unit,
    tint: Color,
) {
    val iconSize =
        when (control) {
            HomeControl.SHORTCUTS -> 28.dp
            HomeControl.SEARCH,
            HomeControl.NOTIFICATIONS,
            HomeControl.LOGOUT -> 32.dp
            HomeControl.SUPPORT -> 28.dp
            HomeControl.SHARE -> 20.dp
            else -> 24.dp
        }
    IconButton(
        onClick = { onControl(control) },
        modifier =
            Modifier.size(48.dp).testTag("header-${control.name}").semantics {
                contentDescription = control.title
            },
    ) {
        Box {
            ReplicaIcon(
                icon,
                when (control) {
                    HomeControl.SEARCH -> "⌕"
                    HomeControl.SHORTCUTS -> "☆"
                    HomeControl.LOGOUT -> "↪"
                    else -> "○"
                },
                Modifier.size(iconSize),
                tint,
            )
            if (control == HomeControl.NOTIFICATIONS) {
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(Color(0xFFFF8A17), CircleShape)
                )
            }
        }
    }
}

@Composable
internal fun ReplicaIcon(key: String, fallback: String, modifier: Modifier, tint: Color) {
    val context = LocalContext.current
    val id =
        if (BuildConfig.PRIVATE_RESOURCES)
            context.resources.getIdentifier(key, "drawable", context.packageName)
        else 0
    if (BuildConfig.PRIVATE_RESOURCES) check(id != 0) { "Missing private icon: $key" }
    if (id != 0)
        Icon(
            painterResource(id),
            contentDescription = null,
            modifier = modifier,
            tint =
                if (
                    key == "replica_barcode" ||
                        key == "replica_home_selected" ||
                        key == "replica_account_selected" ||
                        key == "replica_card_selected"
                )
                    Color.Unspecified
                else tint,
        )
    else
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(fallback, color = tint, fontSize = 22.sp)
        }
}

@Composable
private fun PrivatePattern(key: String) {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(key, "drawable", context.packageName)
    check(id != 0) { "Missing private pattern: $key" }
    Image(
        painterResource(id),
        contentDescription = null,
        modifier = Modifier.size(if (key == "replica_pattern_start") 215.dp else 105.dp, 254.dp),
    )
}

@Composable
internal fun replicaFont(key: String): FontFamily {
    val context = LocalContext.current
    if (!BuildConfig.PRIVATE_RESOURCES) return FontFamily.SansSerif
    val id = context.resources.getIdentifier(key, "font", context.packageName)
    check(id != 0) { "Missing private font: $key" }
    return remember(id) { FontFamily(Font(id)) }
}

@Composable
internal fun replicaRecentHeading(): String {
    if (!BuildConfig.PRIVATE_RESOURCES) return "آخرین تراکنش‌ها"
    val context = LocalContext.current
    val id =
        context.resources.getIdentifier("replica_recent_heading", "string", context.packageName)
    check(id != 0) { "Missing private recent-history title" }
    return context.getString(id)
}
