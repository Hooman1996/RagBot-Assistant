package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Shows a bank result: a receipt page, or a dialog when no page is given. */
internal class BankActions(val finish: (BankResult, String?) -> Unit) {
    /** For a page with its own result: records the outcome and says whether it succeeded. */
    fun accepted(result: BankResult): Boolean {
        if (result is BankResult.Failed) finish(result, null)
        return result is BankResult.Done
    }
}

internal val LocalBank = compositionLocalOf<BankActions> { error("No bank") }

/** The user's confirm. It asks for the OTP first when [otp] is true; only the user can tap it. */
@Composable
internal fun BankButton(
    label: String,
    tag: String,
    receipt: String?,
    otp: Boolean = false,
    action: () -> BankResult,
) {
    val bank = LocalBank.current
    var asking by remember { mutableStateOf(false) }
    Button(
        { if (otp) asking = true else bank.finish(action(), receipt) },
        Modifier.fillMaxWidth().testTag(tag),
    ) {
        Text(label)
    }
    if (asking)
        OtpStep(
            {
                asking = false
                bank.finish(action(), receipt)
            },
            { asking = false },
        )
}

/** The receipt of the last confirmed service. */
@Composable
internal fun BankReceipt(onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    val receipt = MockBank.receipt
    Column(
        Modifier.testTag("bank-receipt"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (receipt == null) Text("رسیدی برای نمایش وجود ندارد.")
        else {
            Text("عملیات با موفقیت انجام شد.", color = ServiceGreen)
            TransferValue("نوع تراکنش", receipt.title)
            if (receipt.amount > 0)
                TransferValue("مبلغ (ریال)", persianNumber(receipt.amount))
            TransferValue("مبدا", receipt.source)
            TransferValue("مقصد", receipt.destination)
            receipt.rows.forEach { (label, value) -> TransferValue(label, value) }
            TransferValue("زمان", receipt.time)
            TransferValue("شماره پیگیری", receipt.trace)
        }
        Button(
            { onUnavailable("ذخیره و اشتراک‌گذاری رسید") },
            Modifier.fillMaxWidth().testTag("bank-receipt-share"),
        ) {
            Text("ذخیره و اشتراک‌گذاری")
        }
        OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("bank-receipt-done")) {
            Text("بازگشت به خانه")
        }
    }
}

/** A bad mock file: the app stops here and names the file instead of crashing. */
@Composable
internal fun MockDataErrorPage(error: MockDataError) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp).testTag("mock-data-error"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("دادهٔ نمایشی قابل خواندن نیست", style = MaterialTheme.typography.titleLarge)
        Text(error.file, Modifier.testTag("mock-data-error-file"))
        Text(error.message.orEmpty(), Modifier.testTag("mock-data-error-message"))
        Text("فایل را اصلاح کنید و برنامه را دوباره باز کنید.")
    }
}
