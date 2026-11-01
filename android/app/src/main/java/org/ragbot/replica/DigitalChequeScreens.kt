package org.ragbot.replica

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** DC-RECOVERY: digital-cheque activation, cartable, detail and transfer previews. */
@Composable
internal fun DigitalChequePage(
    page: String,
    scenario: String,
    active: Boolean,
    selected: String,
    transfer: DigitalTransferDraft,
    onSelect: (String) -> Unit,
    onTransfer: (DigitalTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            DigitalChequeDestinations.ACTIVATION -> "فعالسازی با هامون"
            DigitalChequeDestinations.CARTABLE -> "کارتابل چک دیجیتال"
            DigitalChequeDestinations.DETAIL -> "جزئیات"
            DigitalChequeDestinations.TRANSFER -> "واگذاری چک دیجیتال"
            else -> "تایید اطلاعات"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(DigitalChequeFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    DigitalChequeDestinations.ACTIVATION -> DigitalActivation(active, onUnavailable)
                    DigitalChequeDestinations.CARTABLE -> DigitalCartable(onSelect, onOpen)
                    DigitalChequeDestinations.DETAIL ->
                        DigitalChequeRules.inquiry(selected)?.let {
                            DigitalDetail(it)
                            Button(
                                {
                                    // Recovered: the detail passes its Sayad ID and fixes it.
                                    onTransfer(DigitalTransferDraft(it.sayadId, fixed = true))
                                    onOpen(DigitalChequeDestinations.TRANSFER)
                                },
                                Modifier.fillMaxWidth().testTag("digital-transfer"),
                            ) {
                                Text("واگذاری چک")
                            }
                            OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("بازگشت") }
                        }
                    DigitalChequeDestinations.TRANSFER ->
                        DigitalTransfer(transfer, onTransfer, onOpen, onBack)
                    else ->
                        DigitalChequeRules.inquiry(transfer.sayadId)?.let {
                            PaymentRow("شناسه صیاد:", it.sayadId)
                            Text(
                                "دارنده/ دارندگان چک",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            it.holders.forEach { name ->
                                PaymentRow("نام و نام خانوادگی/نام شرکت:", name)
                            }
                            PaymentRow("شبای سپرده دیجیتال:", it.digitalIban)
                            PaymentRow("حساب", chequeAccount(transfer.accountKey))
                            PaymentRow("برگشت زده شود:", if (transfer.bounce) "بله" else "خیر")
                            PaymentRow(
                                "کارمزد درخواست:",
                                "${persianNumber(DigitalChequeFixture.FEE)} ریال",
                            )
                            // The transfer is signed and sent by the user; nothing is sent here.
                            PaymentActions("تایید", "واگذاری چک دیجیتال", onBack, onUnavailable)
                        }
                }
            }
        }
    }
}

@Composable
private fun DigitalActivation(active: Boolean, onUnavailable: (String) -> Unit) {
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    if (active) {
        Text("امضا چکاد شما فعال است", Modifier.testTag("digital-active"), color = ServiceGreen)
        Button({ confirm = "UPDATE" }, Modifier.fillMaxWidth().testTag("digital-update")) {
            Text("همگام‌سازی با هامون")
        }
        OutlinedButton(
            { confirm = "DE_ACTIVE" },
            Modifier.fillMaxWidth().testTag("digital-deactivate"),
        ) {
            Text("غیرفعال‌سازی")
        }
    } else {
        Text("امضا چکاد", style = MaterialTheme.typography.titleMedium)
        Text(DigitalChequeFixture.ACTIVATION_TEXT, fontSize = 13.sp)
        Button({ confirm = "ACTIVE" }, Modifier.fillMaxWidth().testTag("digital-activate")) {
            Text("درخواست فعالسازی")
        }
    }
    confirm?.let { type ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(
                    when (type) {
                        "ACTIVE" -> "فعال‌سازی خدمت"
                        "UPDATE" -> "همگام‌سازی با هامون"
                        else -> "غیرفعال‌سازی خدمت"
                    }
                )
            },
            text = {
                Column(Modifier.testTag("digital-confirm-sheet")) {
                    Text(
                        when (type) {
                            "ACTIVE" -> DigitalChequeFixture.ACTIVATION_TEXT
                            "UPDATE" -> DigitalChequeFixture.UPDATE_TEXT
                            else -> "آیا مایل به غیرفعال‌سازی خدمت هستید؟"
                        }
                    )
                    if (type != "DE_ACTIVE")
                        Text(
                            "هنگام دانلود اپلیکیشن هامون، به نماد زیر توجه کنید.",
                            fontSize = 12.sp,
                        )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        confirm = null
                        // The external Hamun app and the service request are not used here.
                        onUnavailable(
                            if (type == "DE_ACTIVE") "غیرفعال‌سازی خدمت"
                            else "امضا با برنامهٔ هامون"
                        )
                    },
                    Modifier.testTag("digital-confirm"),
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ confirm = null }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun DigitalCartable(onSelect: (String) -> Unit, onOpen: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    PaymentInput("digital-search", "جستجو بر اساس شناسه صیاد", query, null) { query = it }
    val rows = DigitalChequeRules.search(query)
    if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("digital-empty"))
    rows.forEach {
        OutlinedCard(
            Modifier.fillMaxWidth().testTag("digital-${it.sayadId}").clickable(role = Role.Button) {
                onSelect(it.sayadId)
                onOpen(DigitalChequeDestinations.DETAIL)
            }
        ) {
            Column(Modifier.padding(12.dp)) {
                PaymentRow("شناسه صیاد:", it.sayadId)
                PaymentRow("مبلغ چک:", "${persianNumber(it.amount)} ریال")
                PaymentRow("وضعیت چک:", it.status)
            }
        }
    }
}

@Composable
private fun DigitalDetail(cheque: DigitalCheque) {
    TransferValue("شناسه صیاد:", cheque.sayadId)
    TransferValue("مبلغ چک:", "${persianNumber(cheque.amount)} ریال")
    TransferValue("تاریخ سررسید:", cheque.dueDate)
    TransferValue("بانک عهده چک:", cheque.bank)
    TransferValue("شعبه عهده چک:", cheque.branch)
    TransferValue("وضعیت چک:", cheque.status)
    TransferValue("وضعیت ضمانت چک:", cheque.guarantee)
    TransferValue("وضعیت مسدودی چک:", cheque.block)
    TransferValue("وضعیت نقد کردن چک:", cheque.lock)
    TransferValue("وضعیت ذینفع:", cheque.holderStatus)
    TransferValue("بابت:", cheque.reason)
    if (cheque.description.isNotEmpty()) TransferValue("شرح چک:", cheque.description)
}

@Composable
private fun DigitalTransfer(
    draft: DigitalTransferDraft,
    onChange: (DigitalTransferDraft) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    Text("شناسه صیاد")
    OutlinedButton(
        { picking = true },
        Modifier.fillMaxWidth().testTag("digital-sayad"),
        enabled = !draft.fixed,
    ) {
        Text(draft.sayadId.ifEmpty { "جستجوی چک" })
    }
    error?.let {
        Text(it, Modifier.testTag("digital-sayad-error"), color = MaterialTheme.colorScheme.error)
    }
    ChequeAccountPicker(draft.accountKey) { onChange(draft.copy(accountKey = it)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            draft.bounce,
            { onChange(draft.copy(bounce = it)) },
            Modifier.testTag("digital-bounce"),
        )
        Text("برگشت زده شود.")
    }
    Button(
        {
            error = DigitalChequeRules.sayadError(draft.sayadId)
            if (error == null) onOpen(DigitalChequeDestinations.CONFIRM)
        },
        Modifier.fillMaxWidth().testTag("digital-continue"),
    ) {
        Text("تایید اطلاعات")
    }
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("digital-cancel")) { Text("انصراف") }
    if (picking) {
        var query by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("جستجوی چک") },
            text = {
                Column {
                    PaymentInput("digital-sayad-search", "شناسه صیاد", query, null) { query = it }
                    val rows = DigitalChequeRules.assigned(query)
                    if (rows.isEmpty())
                        Text("شناسه صیادی یافت نشد", Modifier.testTag("digital-sayad-none"))
                    rows.forEach {
                        TextButton(
                            {
                                error = null
                                onChange(draft.copy(sayadId = it))
                                picking = false
                            },
                            Modifier.testTag("digital-sayad-$it"),
                        ) {
                            Text(it)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ picking = false }) { Text("بستن") } },
        )
    }
}
