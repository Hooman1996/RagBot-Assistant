package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CardTransferPage(
    page: String,
    scenario: String,
    draft: CardTransferDraft,
    onChange: (CardTransferDraft) -> Unit,
    onBack: () -> Unit,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    PageFrame(
        page,
        if (page == CardTransferDestinations.RECEIPT) "رسید کارت به کارت" else "کارت به کارت",
        onBack,
    ) {
        Text(
            "پیش‌نمایش محلی • اطلاعات ساختگی؛ هیچ انتقال یا استعلامی انجام نمی‌شود.",
            Modifier.fillMaxWidth().background(Color(0xFFEAF3EE)).padding(12.dp),
            color = ServiceGreen,
            fontSize = 12.sp,
        )
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else
            when (page) {
                CardTransferDestinations.ANONYMOUS,
                CardTransferDestinations.WITHOUT_SOURCE ->
                    CardTransferForm(page, draft, onChange, onBack, onReview, onUnavailable)
                CardTransferDestinations.CONFIRM ->
                    CardTransferConfirmation(draft, onChange, onBack, onUnavailable)
                CardTransferDestinations.RECEIPT -> CardTransferReceipt(onUnavailable)
            }
    }
}

@Composable
private fun CardTransferForm(
    page: String,
    draft: CardTransferDraft,
    onChange: (CardTransferDraft) -> Unit,
    onBack: () -> Unit,
    onReview: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var attempted by rememberSaveable { mutableStateOf(false) }
    val errors = if (attempted) CardTransferRules.errors(draft) else emptyMap()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (page == CardTransferDestinations.ANONYMOUS) {
            Text("کارت مبدا • انتخاب نمونه محلی")
            OutlinedButton(
                { onChange(draft.copy(source = CardTransferFixture.source)) },
                Modifier.fillMaxWidth().testTag("card-transfer-source-picker"),
            ) {
                Text(draft.source.ifEmpty { "انتخاب کارت نمونه" })
            }
            errors["source"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            TransferCardField("کارت مبدا", draft.source, "card-transfer-source", errors["source"]) {
                onChange(draft.copy(source = it))
            }
            TextButton(
                { onUnavailable("اسکن کارت در پیش‌نمایش در دسترس نیست.") },
                Modifier.testTag("card-transfer-scan-source"),
            ) {
                Text("اسکن کارت مبدا")
            }
        }
        TransferCardField(
            "کارت مقصد",
            draft.destination,
            "card-transfer-destination",
            errors["destination"],
        ) {
            onChange(draft.copy(destination = it))
        }
        Row {
            TextButton(
                { onUnavailable("اسکن کارت در پیش‌نمایش در دسترس نیست.") },
                Modifier.testTag("card-transfer-scan"),
            ) {
                Text("اسکن کارت مقصد")
            }
            if (page == CardTransferDestinations.ANONYMOUS)
                TextButton(
                    { onUnavailable("انتخاب مقصد کارت ذخیره‌شده در پیش‌نمایش در دسترس نیست.") },
                    Modifier.testTag("card-transfer-favorites"),
                ) {
                    Text("مقصدهای ذخیره‌شده")
                }
        }
        if (page == CardTransferDestinations.ANONYMOUS)
            TextButton(
                { onUnavailable("دسترسی به مخاطبین در پیش‌نمایش در دسترس نیست.") },
                Modifier.testTag("card-transfer-contacts"),
            ) {
                Text("مخاطبین")
            }
        TransferCardField("مبلغ (ریال)", draft.amount, "card-transfer-amount", errors["amount"]) {
            onChange(draft.copy(amount = it))
        }
        Text(
            "اعتبارسنجی محلی فقط شکل ورودی را بررسی می‌کند؛ پاسخ استعلام نمونه است.",
            fontSize = 12.sp,
        )
        Button(
            {
                attempted = true
                if (CardTransferRules.errors(draft).isEmpty()) onReview()
            },
            Modifier.fillMaxWidth().testTag("card-transfer-review"),
        ) {
            Text("بررسی محلی اطلاعات")
        }
        OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("card-transfer-cancel")) {
            Text("انصراف")
        }
    }
}

@Composable
private fun TransferCardField(
    label: String,
    value: String,
    tag: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) },
        singleLine = true,
        textStyle = TextStyle(textDirection = TextDirection.Ltr),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
    )
}

@Composable
private fun CardTransferSummary(draft: CardTransferDraft) {
    Text("کارت مبدا: ${draft.source}", Modifier.testTag("card-transfer-review-source"))
    Text("کارت مقصد: ${draft.destination}", Modifier.testTag("card-transfer-review-destination"))
    Text("دارنده مقصد: گیرنده نمونه • پاسخ ساختگی")
    Text("مبلغ: ${draft.amount} ریال", Modifier.testTag("card-transfer-review-amount"))
}

@Composable
private fun CardTransferConfirmation(
    draft: CardTransferDraft,
    onChange: (CardTransferDraft) -> Unit,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    // Credentials reset through recreation and Back. They never enter SavedState or a fixture.
    var cvv2 by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var dynamic by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardTransferSummary(draft)
        Row {
            RadioButton(
                dynamic,
                { dynamic = true },
                Modifier.testTag("card-transfer-dynamic").semantics {
                    contentDescription = "رمز پویا"
                },
            )
            Text("رمز پویا")
            RadioButton(
                !dynamic,
                { dynamic = false },
                Modifier.testTag("card-transfer-static").semantics {
                    contentDescription = "رمز ثابت"
                },
            )
            Text("رمز ثابت")
        }
        CardTransferSecret("CVV2", cvv2, "card-transfer-cvv2") { cvv2 = it }
        CardTransferSecret("تاریخ انقضا", expiry, "card-transfer-expiry") { expiry = it }
        CardTransferSecret("رمز دوم", password, "card-transfer-password") { password = it }
        if (dynamic)
            OutlinedButton(
                { onUnavailable("دریافت رمز پویا در دسترس نیست؛ هیچ پیامکی ارسال نمی‌شود.") },
                Modifier.testTag("card-transfer-otp"),
            ) {
                Text("درخواست رمز پویا — غیرفعال")
            }
        OutlinedTextField(
            draft.note,
            { onChange(draft.copy(note = it)) },
            Modifier.fillMaxWidth().testTag("card-transfer-note"),
            label = { Text("یادداشت محلی") },
        )
        TextButton(
            { onUnavailable("انتخاب و بارگذاری پیوست در دسترس نیست.") },
            Modifier.testTag("card-transfer-attachment"),
        ) {
            Text("افزودن پیوست")
        }
        Row {
            Checkbox(
                draft.addFavorite,
                { onChange(draft.copy(addFavorite = it)) },
                Modifier.testTag("card-transfer-save-destination").semantics {
                    contentDescription = "افزودن به مقصدهای ذخیره‌شده فقط در پیش‌نمایش"
                },
            )
            Text("افزودن به مقصدهای ذخیره‌شده • فقط انتخاب محلی")
        }
        Button(
            { onUnavailable("پرداخت و ارسال در پیش‌نمایش در دسترس نیست؛ هیچ انتقالی انجام نشد.") },
            Modifier.fillMaxWidth().testTag("card-transfer-submit"),
        ) {
            Text("تایید و پرداخت — در دسترس نیست")
        }
        OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("card-transfer-edit")) {
            Text("بازگشت و ویرایش")
        }
    }
}

@Composable
private fun CardTransferSecret(
    label: String,
    value: String,
    tag: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}

@Composable
private fun CardTransferReceipt(onUnavailable: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "رسید نمونه مستقل • نتیجه تراکنش نیست",
            Modifier.testTag("card-transfer-receipt-synthetic"),
            color = ServiceGreen,
        )
        CardTransferSummary(CardTransferFixture.receipt)
        Text("وضعیت: نمونه نمایشی؛ هیچ پرداختی انجام نشده است")
        Text("شناسه نمونه: DEMO-CARD-008")
        TextButton(
            { onUnavailable("پیگیری تراکنش در پیش‌نمایش در دسترس نیست.") },
            Modifier.testTag("card-transfer-track"),
        ) {
            Text("پیگیری")
        }
        TextButton(
            { onUnavailable("اشتراک‌گذاری و ذخیره رسید در دسترس نیست.") },
            Modifier.testTag("card-transfer-share"),
        ) {
            Text("اشتراک‌گذاری رسید")
        }
    }
}
