package org.ragbot.replica

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** CR-FLOW: recovered request kinds. Issue starts from an account; the others from a debit card. */
enum class CardOperation(val title: String, val fee: Long) {
    ISSUE("درخواست کارت", 250_000L),
    EXTEND("تمدید کارت", 150_000L),
    REPLICATE("المثنی کارت", 200_000L),
    RECOLOR("تغییر رنگ کارت", 180_000L),
}

/**
 * The draft carried between request pages; nothing here is sent or stored outside saved UI state.
 */
data class CardRequest(
    val operation: CardOperation,
    val sourceKey: String,
    val inPerson: Boolean = true,
    val addressKey: String? = null,
    val colorKey: String? = null,
) : java.io.Serializable

data class PreviewAddress(
    val key: String,
    val title: String,
    val address: String,
    val postalCode: String,
) : java.io.Serializable

data class PreviewCardColor(val key: String, val title: String, val color: Long)

/**
 * CR-FIXTURE: invented branch, addresses, colours and fees (exact integer IRR); Designed for mock.
 */
object CardRequestFixture {
    const val NOTICE = "کارمزد صدور یا تغییر کارت از حساب انتخابی برداشت می‌شود."
    const val BRANCH = "شعبه نمایشی مرکزی، خیابان نمونه، پلاک ۱"
    const val PHONE = "۰۰۰۰۰۰۰۰۰۰۰"
    val addresses =
        listOf(
            PreviewAddress("address-1", "خانه", "خیابان نمونه، کوچه یک، پلاک ۲", "۰۰۰۰۰۰۰۰۰۱"),
            PreviewAddress("address-2", "محل کار", "میدان نمونه، ساختمان دو", "۰۰۰۰۰۰۰۰۰۲"),
        )
    val colors =
        listOf(
            PreviewCardColor("color-1", "سبز", 0xFF237641),
            PreviewCardColor("color-2", "سرمه‌ای", 0xFF1D2B53),
            PreviewCardColor("color-3", "طلایی", 0xFFB08D57),
        )
}

object CardRequestDestinations {
    const val ISSUE = "S-124"
    const val MODIFY = "S-127"
    const val ADDRESS = "S-129"
    const val COLOR = "S-123"
    const val CONFIRM = "S-128"
    val implemented = setOf(ISSUE, MODIFY, ADDRESS, COLOR, CONFIRM)
}

/** Recovered branch: in-person delivery skips the address page. */
internal fun nextAfterFirstPage(request: CardRequest): String =
    if (request.inPerson) CardRequestDestinations.COLOR else CardRequestDestinations.ADDRESS

@Composable
internal fun Choice(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth()
            .testTag(tag)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFE3F4E9) else Color.White,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, null)
            Spacer(Modifier.width(8.dp))
            content()
        }
    }
}

@Composable
internal fun Buttons(
    enabled: Boolean,
    tag: String,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    apply: String = "تایید و ادامه",
) {
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onApply, Modifier.weight(1f).testTag(tag), enabled = enabled) { Text(apply) }
        OutlinedButton(onCancel, Modifier.weight(1f).testTag("request-cancel")) { Text("انصراف") }
    }
}

@Composable
internal fun CardRequestPage(
    page: String,
    request: CardRequest,
    addresses: List<PreviewAddress>,
    scenario: String,
    onChange: (CardRequest) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            CardRequestDestinations.ADDRESS -> "انتخاب آدرس"
            CardRequestDestinations.COLOR -> "انتخاب رنگ کارت"
            CardRequestDestinations.CONFIRM -> "تایید اطلاعات"
            else -> request.operation.title
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    CardRequestDestinations.ADDRESS -> {
                        Text("آدرس محل مورد نظر را انتخاب نمایید", fontSize = 14.sp)
                        addresses.forEach { item ->
                            Choice(
                                item.key == request.addressKey,
                                "address-${item.key}",
                                { onChange(request.copy(addressKey = item.key)) },
                            ) {
                                Column {
                                    Text(item.title, fontSize = 14.sp)
                                    Text(item.address, fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "کد پستی: ${item.postalCode}",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }
                        TextButton(
                            { onOpen(ProfileDestinations.ADD_ADDRESS) },
                            Modifier.testTag("add-address"),
                        ) {
                            Text("افزودن آدرس")
                        }
                        Buttons(
                            request.addressKey != null,
                            "address-ok",
                            { onOpen(CardRequestDestinations.COLOR) },
                            onCancel,
                        )
                    }
                    CardRequestDestinations.COLOR -> {
                        Text("رنگ کارت بانکی خود را انتخاب کنید", fontSize = 14.sp)
                        CardRequestFixture.colors.forEach { item ->
                            Choice(
                                item.key == request.colorKey,
                                "color-${item.key}",
                                { onChange(request.copy(colorKey = item.key)) },
                            ) {
                                Box(
                                    Modifier.size(64.dp, 40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(item.color))
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(item.title, fontSize = 14.sp)
                            }
                        }
                        Buttons(
                            request.colorKey != null,
                            "color-ok",
                            { onOpen(CardRequestDestinations.CONFIRM) },
                            onCancel,
                        )
                    }
                    CardRequestDestinations.CONFIRM -> RequestConfirm(request, addresses, onCancel)
                    else -> FirstPage(page, request, onChange, onOpen, onCancel)
                }
            }
    }
}

/** S-124 picks a source account; S-127 picks a debit card. Both choose delivery. */
@Composable
private fun FirstPage(
    page: String,
    request: CardRequest,
    onChange: (CardRequest) -> Unit,
    onOpen: (String) -> Unit,
    onCancel: () -> Unit,
) {
    if (page == CardRequestDestinations.ISSUE)
        AccountFixture.accounts.forEach { account ->
            Choice(
                account.key == request.sourceKey,
                "source-${account.key}",
                { onChange(request.copy(sourceKey = account.key)) },
            ) {
                Text("حساب ${account.number}", fontSize = 14.sp)
            }
        }
    else
        CardFixture.of(CardKind.DEBIT).forEach { card ->
            Choice(
                card.key == request.sourceKey,
                "source-${card.key}",
                { onChange(request.copy(sourceKey = card.key)) },
            ) {
                CardNumber(card.number, Color.Black, 16)
            }
        }
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFFFF8E1), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("نکته", fontSize = 14.sp)
        Text(CardRequestFixture.NOTICE, fontSize = 12.sp)
    }
    Text("نوع ارسال خود را انتخاب کنید", fontSize = 14.sp)
    Choice(request.inPerson, "delivery-branch", { onChange(request.copy(inPerson = true)) }) {
        Column {
            Text("تحویل حضوری", fontSize = 14.sp)
            Text(CardRequestFixture.BRANCH, fontSize = 12.sp, color = Color.Gray)
        }
    }
    Choice(!request.inPerson, "delivery-post", { onChange(request.copy(inPerson = false)) }) {
        Text("ارسال پستی", fontSize = 14.sp)
    }
    Buttons(true, "first-ok", { onOpen(nextAfterFirstPage(request)) }, onCancel)
}

@Composable
private fun RequestConfirm(
    request: CardRequest,
    addresses: List<PreviewAddress>,
    onClose: () -> Unit,
) {
    var tracking by rememberSaveable { mutableStateOf<String?>(null) }
    val account = AccountFixture.accounts.firstOrNull { it.key == request.sourceKey }
    val card = CardFixture.cards.firstOrNull { it.key == request.sourceKey }
    val address = addresses.firstOrNull { it.key == request.addressKey }
    val color = CardRequestFixture.colors.firstOrNull { it.key == request.colorKey }
    Text("اطلاعات نهایی را جهت تایید بررسی نمایید", fontSize = 14.sp)
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .testTag("request-summary")
    ) {
        InfoRow("نوع درخواست:", request.operation.title)
        if (account != null) InfoRow("شماره حساب:", account.number)
        if (card != null) InfoRow("شماره کارت:", cardNumberGroups(card.number), ltr = true)
        InfoRow("به نام:", account?.owner ?: card?.owner ?: "—")
        if (request.inPerson) InfoRow("شعبه:", CardRequestFixture.BRANCH)
        InfoRow("کارمزد:", "${persianNumber(request.operation.fee)} ریال")
        if (!request.inPerson && address != null) {
            InfoRow("کد پستی:", address.postalCode)
            InfoRow("آدرس:", address.address)
            InfoRow("شماره تماس:", CardRequestFixture.PHONE)
        }
        InfoRow("رنگ کارت:", color?.title ?: "—")
    }
    Buttons(true, "request-submit", { tracking = "۰۰۰۰۰۱" }, onClose, "ثبت درخواست")
    tracking?.let { number ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("نتیجه نمایشی") },
            text = {
                Column(Modifier.semantics(mergeDescendants = true) {}.testTag("request-result")) {
                    Text(
                        "درخواست در این نسخه نمایشی ثبت شد. هیچ کارتی صادر یا تغییر نکرد و کارمزدی برداشت نشد."
                    )
                    Text("شماره پیگیری: $number")
                    Text("طراحی برای ماک", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClose, Modifier.testTag("request-done")) { Text("بازگشت به کارت‌ها") }
            },
        )
    }
}
