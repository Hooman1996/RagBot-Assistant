package org.ragbot.replica

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CardSecurityDestinations {
    const val BLOCK = "S-121"
    const val LEGACY_BLOCK = "S-118"
    const val ANONYMOUS_BLOCK = "S-122"
    const val PIN = "S-135"
    const val ACTIVATION = "S-136"
    val implemented = setOf(BLOCK, LEGACY_BLOCK, ANONYMOUS_BLOCK, PIN, ACTIVATION)
}

/** CS-FIXTURE: the OTP is a fixed local code and the phone is synthetic; Designed for mock. */
internal const val PREVIEW_OTP = "12345"
private const val PREVIEW_PHONE = "۰۹۰۰۰۰۰۰۰۰۰"

/** CS-PIN: one section per recovered PIN operation. */
enum class PinOperation(val title: String, val second: Boolean, val change: Boolean) {
    ISSUE_FIRST("تخصیص رمز اول کارت", false, false),
    ISSUE_SECOND("تخصیص رمز دوم کارت", true, false),
    CHANGE_FIRST("تغییر رمز اول کارت", false, true),
    CHANGE_SECOND("تغییر رمز دوم کارت", true, true),
}

/** Recovered: first PIN is 4 digits, second PIN 5–12, retype must match, all fields required. */
internal fun pinErrors(
    operation: PinOperation,
    current: String,
    new: String,
    retype: String,
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    val required = "تکمیل این فیلد اجباری است."
    if (operation.change && current.isEmpty()) errors["current"] = required
    val valid = if (operation.second) new.length in 5..12 else new.length == 4
    if (new.isEmpty()) errors["new"] = required
    else if (!valid || latinDigits(new).any { it !in '0'..'9' })
        errors["new"] =
            if (operation.second) "طول رمز باید بین ۵ تا ۱۲ رقم باشد."
            else "طول رمز باید ۴ رقم باشد."
    if (retype.isEmpty()) errors["retype"] = required
    else if (retype != new)
        errors["retype"] =
            if (operation.second) "مقدار وارد شده با رمز دوم جدید یکسان نیست."
            else "مقدار وارد شده با رمز اول جدید یکسان نیست."
    return errors
}

/** Labelled local outcome: nothing is blocked, assigned or activated. */
@Composable
private fun MockResult(text: String, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("نتیجه نمایشی") },
        text = {
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag("security-result")) {
                Text(text)
                Text("در این نسخه نمایشی هیچ تغییری روی کارت انجام نشد.")
                Text("طراحی برای ماک", fontSize = 11.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onDone, Modifier.testTag("security-done")) { Text("متوجه شدم") }
        },
    )
}

/** Recovered OTP sheet; the entered code is never saved. Cancel leaves the form unchanged. */
@Composable
private fun OtpStep(onVerified: () -> Unit, onCancel: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column {
                Text("کد ارسال شده به شماره $PREVIEW_PHONE را وارد کنید.")
                Text("کد نمایشی: ۱۲۳۴۵", fontSize = 11.sp, color = Color.Gray)
                OutlinedTextField(
                    code,
                    {
                        code = it.take(6)
                        wrong = false
                    },
                    Modifier.testTag("otp-code"),
                    singleLine = true,
                    isError = wrong,
                    supportingText = if (wrong) ({ Text("کد وارد شده صحیح نیست.") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                TextButton({ code = "" }, Modifier.testTag("otp-resend")) { Text("ارسال مجدد") }
            }
        },
        confirmButton = {
            TextButton(
                { if (latinDigits(code) == PREVIEW_OTP) onVerified() else wrong = true },
                Modifier.testTag("otp-ok"),
            ) {
                Text("تایید")
            }
        },
        dismissButton = { TextButton(onCancel, Modifier.testTag("otp-cancel")) { Text("انصراف") } },
    )
}

@Composable
internal fun CardSecurityPage(
    page: String,
    kind: CardKind,
    scenario: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            CardSecurityDestinations.PIN -> "رمز کارت"
            CardSecurityDestinations.ACTIVATION -> "فعال‌سازی رمز پویا"
            else -> "مسدودی کارت"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    CardSecurityDestinations.BLOCK -> BlockCard(kind, onBack)
                    CardSecurityDestinations.PIN -> PinSections(kind, onBack)
                    CardSecurityDestinations.ACTIVATION -> Activation(kind, onBack)
                    else -> NoLoginBlock(page, onBack, onUnavailable)
                }
            }
    }
}

/** S-121: pick a card of the incoming group, confirm the recovered warning; no OTP. */
@Composable
private fun BlockCard(kind: CardKind, onDone: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var done by rememberSaveable { mutableStateOf(false) }
    Text("یک کارت را جهت مسدود کردن، انتخاب کنید:", fontSize = 14.sp)
    CardFixture.of(kind).forEach { card ->
        Surface(
            Modifier.fillMaxWidth().testTag("block-${card.key}").clickable(role = Role.Button) {
                selected = card.key
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
        ) {
            Column(Modifier.padding(12.dp)) {
                CardNumber(card.number, Color.Black, 16)
                Text(card.owner, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
    val card = CardFixture.cards.firstOrNull { it.key == selected }
    if (card != null && !done)
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("مسدودی کارت") },
            text = {
                Column {
                    Text(
                        "با مسدودی کارت،‌ از انجام هرگونه تراکنش یا استفاده از آن جلوگیری خواهد شد. برای فعال‌سازی کارت ضروریست به شعب بانک مراجعه کنید."
                    )
                    Text("آیا مایل به مسدود کردن کارت هستید؟")
                }
            },
            confirmButton = {
                TextButton({ done = true }, Modifier.testTag("block-ok")) { Text("تایید") }
            },
            dismissButton = { TextButton({ selected = null }) { Text("انصراف") } },
        )
    if (card != null && done)
        MockResult(
            "کارت به شماره \u2066${cardNumberGroups(card.number)}\u2069 مسدود گردید.",
            onDone,
        )
}

/** S-135: card picker and four expandable sections, then confirm, OTP and result. */
@Composable
private fun PinSections(kind: CardKind, onDone: () -> Unit) {
    val cards = CardFixture.of(kind)
    var card by rememberSaveable { mutableStateOf(cards.firstOrNull()?.key.orEmpty()) }
    var open by rememberSaveable { mutableStateOf<PinOperation?>(null) }
    var stage by rememberSaveable { mutableStateOf("form") }
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var retype by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    CardChooser(cards, card) { card = it }
    PinOperation.entries.forEach { operation ->
        Surface(
            Modifier.fillMaxWidth().testTag("pin-${operation.name}").clickable(role = Role.Button) {
                open = if (open == operation) null else operation
                current = ""
                new = ""
                retype = ""
                errors = emptyMap()
            },
            shape = RoundedCornerShape(12.dp),
            color = if (open == operation) Color(0xFFE3F4E9) else Color.White,
        ) {
            Text(operation.title, Modifier.padding(16.dp), fontSize = 14.sp)
        }
        if (open == operation) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    if (operation.second) "طول رمز باید بین ۵ تا ۱۲ رقم باشد."
                    else "طول رمز باید ۴ رقم باشد.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                val name = if (operation.second) "رمز دوم" else "رمز اول"
                if (operation.change)
                    SecretField("current", "$name فعلی", current, errors) { current = it }
                SecretField("new", "$name جدید", new, errors) { new = it }
                SecretField("retype", "تکرار $name جدید", retype, errors) { retype = it }
                Button(
                    {
                        errors = pinErrors(operation, current, new, retype)
                        if (errors.isEmpty()) stage = "confirm"
                    },
                    Modifier.fillMaxWidth().testTag("pin-ok"),
                ) {
                    Text("تایید")
                }
            }
        }
    }
    val operation = open
    when {
        operation == null -> {}
        stage == "confirm" ->
            AlertDialog(
                onDismissRequest = { stage = "form" },
                title = { Text(operation.title) },
                text = { Text("آیا از انجام این عملیات اطمینان دارید؟") },
                confirmButton = {
                    TextButton({ stage = "otp" }, Modifier.testTag("pin-confirm")) { Text("تایید") }
                },
                dismissButton = { TextButton({ stage = "form" }) { Text("انصراف") } },
            )
        stage == "otp" -> OtpStep({ stage = "done" }, { stage = "form" })
        stage == "done" -> MockResult("عملیات با موفقیت انجام شد.", onDone)
    }
}

@Composable
private fun SecretField(
    key: String,
    label: String,
    value: String,
    errors: Map<String, String>,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        { onChange(it.take(12)) },
        Modifier.fillMaxWidth().testTag("secret-$key"),
        label = { Text(label) },
        singleLine = true,
        isError = key in errors,
        supportingText = errors[key]?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
    )
}

/** S-136: pick a card, read the warning, send the code, verify the OTP. */
@Composable
private fun Activation(kind: CardKind, onDone: () -> Unit) {
    val cards = CardFixture.of(kind)
    var card by rememberSaveable { mutableStateOf(cards.firstOrNull()?.key.orEmpty()) }
    var stage by rememberSaveable { mutableStateOf("ready") }
    CardChooser(cards, card) { card = it }
    Text(
        "جهت فعال‌سازی رمز پویا، یک کد تایید به شماره همراه متصل به کارت انتخاب شده ارسال می‌گردد.",
        fontSize = 14.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button({ stage = "otp" }, Modifier.weight(1f).testTag("activation-send")) {
            Text("ارسال کد")
        }
        OutlinedButton(onDone, Modifier.weight(1f)) { Text("انصراف") }
    }
    when (stage) {
        "otp" -> OtpStep({ stage = "done" }, { stage = "ready" })
        "done" -> MockResult("رمز پویای کارت فعال شد.", onDone)
    }
}

/**
 * S-118/S-122 pre-login block: recovered fields, required checks, dynamic second-password request.
 */
@Composable
private fun NoLoginBlock(page: String, onDone: () -> Unit, onUnavailable: (String) -> Unit) {
    var number by rememberSaveable { mutableStateOf("") }
    var cvv2 by remember { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var done by rememberSaveable { mutableStateOf(false) }
    Text(
        "با مسدودی کارت،‌ از انجام هرگونه تراکنش با استفاده از این کارت جلوگیری می‌شود و برای فعالسازی مجدد ضروریست به شعب بانک مراجعه کنید.",
        Modifier.background(Color(0xFFFFF8E1), RoundedCornerShape(12.dp)).padding(12.dp),
        fontSize = 12.sp,
    )
    OutlinedTextField(
        number,
        { number = it.take(16) },
        Modifier.fillMaxWidth().testTag("noLogin-number"),
        label = { Text("شماره کارت") },
        singleLine = true,
        isError = "number" in errors,
        supportingText = errors["number"]?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    SecretField("cvv2", "CVV2", cvv2, errors) { cvv2 = it.take(4) }
    OutlinedTextField(
        expiry,
        { expiry = it.take(5) },
        Modifier.fillMaxWidth().testTag("noLogin-expiry"),
        label = { Text("تاریخ انقضا (ماه/سال)") },
        singleLine = true,
        isError = "expiry" in errors,
        supportingText = errors["expiry"]?.let { { Text(it) } },
    )
    SecretField(
        "password",
        if (page == CardSecurityDestinations.ANONYMOUS_BLOCK) "رمز دوم ثابت یا پویا"
        else "رمز پویا",
        password,
        errors,
    ) {
        password = it
    }
    TextButton({ onUnavailable("ارسال رمز پویا") }, Modifier.testTag("noLogin-dynamic")) {
        Text("دریافت رمز")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            {
                errors = secureFormErrors(number, cvv2, expiry, password)
                done = errors.isEmpty()
            },
            Modifier.weight(1f).testTag("noLogin-ok"),
        ) {
            Text("تایید")
        }
        OutlinedButton(onDone, Modifier.weight(1f)) { Text("انصراف") }
    }
    if (done)
        MockResult(
            "کارت به شماره \u2066${cardNumberGroups(latinDigits(number))}\u2069 مسدود گردید.",
            onDone,
        )
}
