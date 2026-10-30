package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Destinations hosted by the wallet-card issue container S-138, plus wallet PIN S-141. */
object WalletDestinations {
    const val SEND_TYPE = "S-140"
    const val ADDRESS = "S-137"
    const val CONFIRM = "S-139"
    const val PIN = "S-141"
    val implemented = setOf(SEND_TYPE, ADDRESS, CONFIRM, PIN)
}

/**
 * WC-FIXTURE: fee is exact integer IRR; in-person delivery is shown disabled. Designed for mock.
 */
internal const val WALLET_CARD_FEE = 120_000L

/** Draft carried from send type to confirm; the card design is the first available one. */
data class WalletRequest(val walletKey: String, val addressKey: String? = null) :
    java.io.Serializable

@Composable
internal fun WalletPage(
    page: String,
    request: WalletRequest?,
    addresses: List<PreviewAddress>,
    scenario: String,
    onChange: (WalletRequest) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val title =
        when (page) {
            WalletDestinations.ADDRESS -> "انتخاب آدرس"
            WalletDestinations.CONFIRM -> "تایید اطلاعات"
            WalletDestinations.PIN -> "دریافت و تغییر رمز"
            else -> "اولین کیف کارت"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (page == WalletDestinations.PIN)
                    PinSections(CardFixture.of(CardKind.WALLET), onBack)
                else {
                    val draft = checkNotNull(request)
                    when (page) {
                        WalletDestinations.ADDRESS ->
                            WalletAddress(draft, addresses, onChange, onOpen, onCancel)
                        WalletDestinations.CONFIRM -> WalletConfirm(draft, addresses, onCancel)
                        else -> SendType(draft, onChange, onOpen, onCancel)
                    }
                }
            }
    }
}

/** S-140: wallet, notice, fee and delivery options; continuing always goes to the address page. */
@Composable
private fun SendType(
    request: WalletRequest,
    onChange: (WalletRequest) -> Unit,
    onOpen: (String) -> Unit,
    onCancel: () -> Unit,
) {
    CardFixture.of(CardKind.WALLET).forEach { wallet ->
        Choice(
            wallet.key == request.walletKey,
            "wallet-${wallet.key}",
            { onChange(request.copy(walletKey = wallet.key)) },
        ) {
            Text("کیف پول ${wallet.primaryAccount}", fontSize = 14.sp)
        }
    }
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFFFF8E1), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("نکته", fontSize = 14.sp)
        Text("کارمزد صدور کیف کارت از کیف پول انتخابی برداشت می‌شود.", fontSize = 12.sp)
    }
    Text("هزینه کارمزد: ${persianNumber(WALLET_CARD_FEE)} ریال", Modifier.testTag("wallet-fee"))
    Text("نوع ارسال خود را انتخاب کنید", fontSize = 14.sp)
    Choice(false, "wallet-branch", {}) {
        Text("تحویل حضوری (در حال حاضر فعال نیست)", fontSize = 14.sp, color = Color.Gray)
    }
    Choice(true, "wallet-post", {}) { Text("ارسال پستی", fontSize = 14.sp) }
    Buttons(true, "wallet-type-ok", { onOpen(WalletDestinations.ADDRESS) }, onCancel)
}

/** S-137: required address; adding one belongs to FR-05. */
@Composable
private fun WalletAddress(
    request: WalletRequest,
    addresses: List<PreviewAddress>,
    onChange: (WalletRequest) -> Unit,
    onOpen: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Text("آدرس محل مورد نظر را انتخاب نمایید", fontSize = 14.sp)
    addresses.forEach { item ->
        Choice(
            item.key == request.addressKey,
            "wallet-address-${item.key}",
            { onChange(request.copy(addressKey = item.key)) },
        ) {
            Column {
                Text(item.title, fontSize = 14.sp)
                Text(item.address, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
    TextButton(
        { onOpen(ProfileDestinations.ADD_ADDRESS) },
        Modifier.testTag("wallet-add-address"),
    ) {
        Text("افزودن آدرس")
    }
    Buttons(
        request.addressKey != null,
        "wallet-address-ok",
        { onOpen(WalletDestinations.CONFIRM) },
        onCancel,
    )
}

/** S-139: review and register; the result is a labelled local outcome. */
@Composable
private fun WalletConfirm(
    request: WalletRequest,
    addresses: List<PreviewAddress>,
    onClose: () -> Unit,
) {
    var done by rememberSaveable { mutableStateOf(false) }
    val wallet = CardFixture.cards.first { it.key == request.walletKey }
    val address = addresses.firstOrNull { it.key == request.addressKey }
    Text("اطلاعات نهایی را جهت تایید بررسی نمایید", fontSize = 14.sp)
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .testTag("wallet-summary")
    ) {
        InfoRow("شماره حساب:", wallet.primaryAccount)
        InfoRow("به نام:", wallet.owner)
        InfoRow("کارمزد:", "${persianNumber(WALLET_CARD_FEE)} ریال")
        InfoRow("کد پستی:", address?.postalCode ?: "—")
        InfoRow("آدرس:", address?.address ?: "—")
    }
    Buttons(true, "wallet-submit", { done = true }, onClose, "ثبت درخواست")
    if (done) MockResult("درخواست صدور کیف کارت ثبت شد. شماره پیگیری: ۰۰۰۰۰۲", onClose)
}
