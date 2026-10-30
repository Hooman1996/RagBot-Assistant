package org.ragbot.replica

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SupportPage(
    page: String,
    scenario: String,
    selectedTicket: String,
    drafts: Map<String, String>,
    onDraft: (String, String) -> Unit,
    onSelectTicket: (String) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            SupportDestinations.CHATBOT -> "پرسش و پاسخ"
            SupportDestinations.FAQ,
            SupportDestinations.FAQ_NO_LOGIN -> "سوالات متداول"
            SupportDestinations.SUGGESTION -> "بازخورد/ایده"
            SupportDestinations.TICKETS -> "صندوق پیام‌ها"
            SupportDestinations.TICKET -> "مشاهده پیام"
            else -> "ارسال/مشاهده پیام"
        }
    PageFrame(page, title, onBack) {
        Column(Modifier.fillMaxSize().testTag("support-page-$page")) {
            Text(
                "پیش‌نمایش محلی • همه اطلاعات ساختگی است",
                Modifier.fillMaxWidth().background(Color(0xFFEAF3EE)).padding(12.dp),
                color = ServiceGreen,
                fontSize = 12.sp,
            )
            when {
                scenario == "loading" ->
                    Box(Modifier.fillMaxSize().testTag("support-loading"), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                scenario == "error" ->
                    Column(Modifier.padding(24.dp).testTag("support-error")) {
                        Text("دریافت اطلاعات نمونه با خطا مواجه شد.")
                        Button(onRetry, Modifier.testTag("support-retry")) { Text("تلاش مجدد") }
                    }
                page == SupportDestinations.FAQ || page == SupportDestinations.FAQ_NO_LOGIN ->
                    FaqPage(page, scenario, onOpen)
                page == SupportDestinations.SUGGESTION ->
                    SuggestionPage(
                        drafts["suggestion"].orEmpty(),
                        { onDraft("suggestion", it) },
                        drafts["suggestion-rating"] ?: "NONE",
                        { onDraft("suggestion-rating", it) },
                        onBack,
                    )
                page == SupportDestinations.CHATBOT ->
                    ChatbotPage(
                        drafts["chatbot"].orEmpty(),
                        { onDraft("chatbot", it) },
                        onUnavailable,
                    )
                page == SupportDestinations.TICKETS ->
                    SupportTicketList(scenario) {
                        onSelectTicket(it)
                        onOpen(SupportDestinations.TICKET)
                    }
                page == SupportDestinations.TICKET || page == SupportDestinations.MESSAGE ->
                    SupportMessages(
                        page,
                        scenario,
                        selectedTicket,
                        drafts,
                        onDraft,
                        onSelectTicket,
                        onUnavailable,
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqPage(page: String, scenario: String, onOpen: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var identityStep by rememberSaveable { mutableIntStateOf(0) }
    var mobile by rememberSaveable { mutableStateOf("") }
    // OTP deliberately stays outside saveable state and all persistent drafts.
    var otp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    BackHandler(identityStep > 0) {
        identityStep = 0
        otp = ""
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            if (page == SupportDestinations.FAQ) "سوالات متداول • کاربر واردشده"
            else "سوالات متداول • بدون ورود",
            Modifier.padding(16.dp).testTag("faq-variant"),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            if (scenario == "empty")
                Text("سوالی برای نمایش وجود ندارد.", Modifier.testTag("support-empty"))
            else
                SupportFixture.faqs.forEachIndexed { index, faq ->
                    Surface(
                        Modifier.fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("faq-item-$index")
                            .semantics {
                                stateDescription = if (index in expanded) "باز" else "بسته"
                            }
                            .clickable(role = Role.Button) {
                                expanded =
                                    if (index in expanded) expanded - index else expanded + index
                            },
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(faq.question, Modifier.weight(1f))
                                Text(if (index in expanded) "−" else "+", color = ServiceGreen)
                            }
                            if (index in expanded) {
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                Text(
                                    faq.answer,
                                    Modifier.testTag("faq-answer-$index"),
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
        }
        if (page == SupportDestinations.FAQ_NO_LOGIN) {
            Button(
                {
                    identityStep = 1
                    error = ""
                },
                Modifier.fillMaxWidth().padding(16.dp).testTag("faq-support"),
            ) {
                Text("ارسال/مشاهده پیام")
            }
        }
    }
    if (identityStep > 0) {
        ModalBottomSheet(
            onDismissRequest = {
                identityStep = 0
                otp = ""
            }
        ) {
            Column(Modifier.fillMaxWidth().imePadding().padding(20.dp)) {
                Text("احراز هویت نمایشی؛ هیچ پیامکی ارسال نمی‌شود.")
                if (identityStep == 1) {
                    Text("برای ارسال/مشاهده پیام شماره همراه نمونه را وارد کنید.")
                    OutlinedTextField(
                        mobile,
                        {
                            mobile = it
                            error = ""
                        },
                        Modifier.fillMaxWidth().testTag("support-mobile"),
                        label = { Text("شماره همراه") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )
                    Button(
                        {
                            if (ProfileRules.mobile(mobile)) {
                                identityStep = 2
                                error = ""
                            } else error = "شماره همراه باید ۱۱ رقم و با ۰۹ آغاز شود."
                        },
                        Modifier.fillMaxWidth().testTag("support-identity-continue"),
                    ) {
                        Text("ادامه نمایشی")
                    }
                } else {
                    Text("کد نمایشی 12345 را وارد کنید. این کد ذخیره نمی‌شود.")
                    OutlinedTextField(
                        otp,
                        {
                            otp = it
                            error = ""
                        },
                        Modifier.fillMaxWidth().testTag("support-otp"),
                        label = { Text("کد نمایشی") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                    Button(
                        {
                            if (latinDigits(otp) == "12345") {
                                otp = ""
                                identityStep = 0
                                onOpen(SupportDestinations.MESSAGE)
                            } else error = "کد نمایشی را بررسی کنید."
                        },
                        Modifier.fillMaxWidth().testTag("support-otp-continue"),
                    ) {
                        Text("ورود به پیش‌نمایش پیام‌ها")
                    }
                    TextButton({
                        otp = ""
                        error = "کد نمونه همان 12345 است؛ پیامکی ارسال نشد."
                    }) {
                        Text("ارسال مجدد نمایشی")
                    }
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(
                    {
                        identityStep = 0
                        otp = ""
                    },
                    Modifier.testTag("support-identity-cancel"),
                ) {
                    Text("انصراف")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionPage(
    draft: String,
    onDraft: (String) -> Unit,
    rating: String,
    onRating: (String) -> Unit,
    onBack: () -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    BackHandler(confirm) { confirm = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("تجربه شما در استفاده از برنامه چطور بود؟", fontSize = 20.sp)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("EXCELLENT" to "عالی", "GOOD" to "خوب", "FAIR" to "متوسط", "POOR" to "ضعیف")
                .forEach { (key, title) ->
                    FilterChip(
                        selected = rating == key,
                        onClick = { onRating(key) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ReplicaIcon(
                                    "replica_support_rating_${key.lowercase()}",
                                    "☺",
                                    Modifier.size(32.dp),
                                    Color.Unspecified,
                                )
                                Text(title, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("suggestion-rating-$key"),
                    )
                }
        }
        OutlinedTextField(
            draft,
            onDraft,
            Modifier.fillMaxWidth().heightIn(min = 160.dp).testTag("suggestion-description"),
            label = { Text("بازخورد/ایده") },
            minLines = 5,
        )
        Text(
            "ثبت فقط در پیش‌نمایش محلی است؛ هیچ نظری به بانک ارسال نمی‌شود.",
            Modifier.padding(vertical = 16.dp),
            fontSize = 13.sp,
        )
        Button(
            { confirm = true },
            Modifier.fillMaxWidth().testTag("suggestion-submit"),
            enabled = SupportRules.suggestion(draft),
        ) {
            Text("ثبت در پیش‌نمایش محلی")
        }
        TextButton(onBack, Modifier.fillMaxWidth()) { Text("انصراف") }
    }
    if (confirm) {
        ModalBottomSheet(onDismissRequest = { confirm = false }) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "بازخورد نمونه فقط در پیش‌نمایش محلی ثبت شد.",
                    Modifier.testTag("support-preview-result"),
                )
                Text("هیچ پیامی به بانک ارسال نشد. متن پیش‌نویس باقی می‌ماند.")
                Button(
                    {
                        confirm = false
                        onBack()
                    },
                    Modifier.testTag("support-preview-confirm"),
                ) {
                    Text("متوجه شدم")
                }
                TextButton({ confirm = false }, Modifier.testTag("support-preview-cancel")) {
                    Text("انصراف")
                }
            }
        }
    }
}

@Composable
private fun ChatbotPage(draft: String, onDraft: (String) -> Unit, onUnavailable: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("پرسش و پاسخ", fontSize = 26.sp, color = ServiceGreen)
            Surface(
                Modifier.padding(top = 24.dp),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("کاربر گرامی، سلام. چطور می‌تونم کمکتون کنم؟", Modifier.padding(20.dp))
            }
            Text(
                "هرچی درباره‌ی برنامه می‌خوای بدونی، از من بپرس.",
                Modifier.padding(vertical = 16.dp),
            )
            Text(
                "تولید پاسخ در این پیش‌نمایش در دسترس نیست؛ متن شما ارسال نمی‌شود.",
                fontSize = 13.sp,
                color = Color.Gray,
            )
        }
        OutlinedTextField(
            draft,
            { if (it.length <= 250) onDraft(it) },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("chatbot-draft"),
            label = { Text("پرسش شما") },
            supportingText = {
                Text("${persianNumber(draft.length.toLong())}/۲۵۰ • حداقل ۴ نویسه")
            },
            maxLines = 5,
        )
        Button(
            { onUnavailable("پاسخ‌گویی در دسترس نیست؛ پرسش ارسال نشد و پاسخ تولید نمی‌شود.") },
            Modifier.fillMaxWidth().padding(16.dp).testTag("chatbot-send"),
            enabled = SupportRules.question(draft),
        ) {
            Text("پرسش • پیش‌نمایش محلی")
        }
    }
}

@Composable
private fun SupportTicketList(scenario: String, onTicket: (String) -> Unit) {
    var shown by rememberSaveable { mutableIntStateOf(2) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tickets-scroll")
    ) {
        if (scenario == "empty")
            Text("پیامی برای نمایش وجود ندارد.", Modifier.testTag("support-empty"))
        else {
            SupportFixture.tickets.take(shown).forEach { ticket ->
                Surface(
                    Modifier.fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("ticket-${ticket.key}")
                        .clickable(role = Role.Button) { onTicket(ticket.key) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(ticket.caption, color = ServiceGreen)
                        Text(ticket.question, Modifier.padding(vertical = 8.dp), maxLines = 2)
                        Text(
                            "${supportStatus(ticket.status)} • ${ticket.modified}",
                            fontSize = 12.sp,
                        )
                        Text(ticket.key, fontSize = 12.sp)
                    }
                }
            }
            if (shown < SupportFixture.tickets.size)
                TextButton({ shown += 2 }, Modifier.testTag("tickets-more")) { Text("نمایش بیشتر") }
        }
    }
}

private fun supportStatus(status: String) =
    when (status) {
        "ANSWERED" -> "پاسخ داده شده"
        "CLOSED" -> "پایان یافته"
        else -> "در انتظار بررسی"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportMessages(
    page: String,
    scenario: String,
    selectedTicket: String,
    drafts: Map<String, String>,
    onDraft: (String, String) -> Unit,
    onSelectTicket: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showingTicket by rememberSaveable {
        mutableStateOf(page == SupportDestinations.TICKET && selectedTicket.isNotEmpty())
    }
    var editing by rememberSaveable(selectedTicket) { mutableStateOf(false) }
    val historyPages = rememberSaveableStateHolder()
    val ticket = SupportFixture.tickets.firstOrNull { it.key == selectedTicket }
    if (ticket == null && selectedTicket.isNotEmpty()) {
        Text(
            "پیام انتخاب‌شده در داده‌های نمونه موجود نیست.",
            Modifier.padding(16.dp).testTag("support-unknown-ticket"),
        )
        return
    }
    val key =
        if (showingTicket) "ticket-${checkNotNull(ticket).key}"
        else if (page == SupportDestinations.TICKET) "ticket-new" else "message"
    val draft = drafts[key].orEmpty()
    Column(Modifier.fillMaxSize()) {
        if (page == SupportDestinations.MESSAGE) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    tab == 0,
                    {
                        tab = 0
                        showingTicket = false
                        editing = false
                    },
                    Modifier.testTag("message-tab-compose"),
                    text = { Text("ایجاد پیام") },
                )
                Tab(
                    tab == 1,
                    { tab = 1 },
                    Modifier.testTag("message-tab-history"),
                    text = { Text("لیست پیام‌ها") },
                )
            }
        }
        if (tab == 1 && page == SupportDestinations.MESSAGE) {
            historyPages.SaveableStateProvider("history") {
                SupportTicketList(scenario) {
                    onSelectTicket(it)
                    showingTicket = true
                    tab = 0
                }
            }
        } else {
            Column(
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("support-thread")
            ) {
                if (showingTicket && ticket != null) {
                    Text(
                        ticket.key,
                        Modifier.testTag("support-ticket-${ticket.key}"),
                        color = ServiceGreen,
                    )
                    Text(ticket.caption, fontSize = 20.sp)
                    Text(supportStatus(ticket.status), Modifier.padding(vertical = 8.dp))
                    SupportFixture.messages
                        .filter { it.ticketKey == ticket.key }
                        .forEach { message ->
                            Surface(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = if (message.fromSupport) Color.White else Color(0xFFE1F1E8),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        if (message.fromSupport) "پاسخ نمونه" else "پیام نمونه شما",
                                        color = ServiceGreen,
                                        fontSize = 12.sp,
                                    )
                                    Text(message.text)
                                    Text(
                                        message.time,
                                        Modifier.padding(top = 8.dp),
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }
                } else {
                    Text("کاربر گرامی، سلام. چطور می‌تونم کمکتون کنم؟", color = ServiceGreen)
                    Text(
                        "توضیحات خود را به صورت کامل در یک پیام بنویسید.",
                        Modifier.padding(vertical = 16.dp),
                    )
                }
                if (showingTicket && ticket?.status == "CLOSED") {
                    Text("این پیام بررسی شده و پایان یافته است.")
                }
            }
            if (!showingTicket || ticket?.status != "CLOSED") {
                if (showingTicket && ticket != null && !editing) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        if (ticket.status == "ASKED") {
                            OutlinedButton(
                                {
                                    onDraft(key, drafts[key] ?: ticket.question)
                                    editing = true
                                },
                                Modifier.weight(1f).testTag("support-edit"),
                            ) {
                                Text("ویرایش پیام")
                            }
                        } else if (ticket.status in listOf("ANSWERED", "SEEN_BY_CUSTOMER")) {
                            OutlinedButton(
                                { editing = true },
                                Modifier.weight(1f).testTag("support-continue"),
                            ) {
                                Text("ادامه گفتگو")
                            }
                            TextButton(
                                {
                                    onUnavailable(
                                        "پایان دادن به پیام نیازمند سرویس پشتیبانی است؛ تغییری اعمال نشد."
                                    )
                                },
                                Modifier.testTag("support-finish"),
                            ) {
                                Text("پایان")
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        draft,
                        { if (it.length <= 250) onDraft(key, it) },
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("support-draft"),
                        label = { Text(if (editing) "متن پیام" else "پیام شما") },
                        supportingText = {
                            Text("${persianNumber(draft.length.toLong())}/۲۵۰ • حداقل ۵ نویسه")
                        },
                        maxLines = 5,
                    )
                    Button(
                        {
                            onUnavailable(
                                "پیام ارسال نشد؛ سرویس پشتیبانی در دسترس نیست. پیش‌نویس فقط محلی باقی می‌ماند."
                            )
                        },
                        Modifier.fillMaxWidth().padding(16.dp).testTag("support-send"),
                        enabled = SupportRules.message(draft),
                    ) {
                        Text("ارسال • پیش‌نمایش محلی")
                    }
                    if (showingTicket)
                        TextButton(
                            { editing = false },
                            Modifier.fillMaxWidth().testTag("support-edit-cancel"),
                        ) {
                            Text("انصراف از ویرایش")
                        }
                }
            }
        }
    }
}
