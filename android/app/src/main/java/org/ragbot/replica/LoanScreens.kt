package org.ragbot.replica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** LN-RECOVERY: read-only obligations and local repayment review; no payment handler. */
@Composable
internal fun LoanPage(
    page: String,
    scenario: String,
    loanId: String,
    selected: List<Int>,
    source: String,
    paymentId: String,
    groupKey: String,
    onLoan: (String) -> Unit,
    onSelected: (List<Int>) -> Unit,
    onSource: (String) -> Unit,
    onPaymentId: (String) -> Unit,
    onGroup: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            LoanDestinations.MENU -> "تسهیلات"
            LoanDestinations.SUMMARY -> "تسهیلات جاری و پرداخت اقساط"
            LoanDestinations.DETAIL -> "جزئیات تسهیلات"
            LoanDestinations.CONFIRM -> "تایید پرداخت قسط"
            else -> "تسهیلات باشگاه مشتریان"
        }
    PageFrame(page, title, onBack) {
        if (page != LoanDestinations.MENU && scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(PaymentFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    LoanDestinations.MENU -> {
                        Text("فهرست نمونه؛ ترتیب و دسترسی خدمات از دادهٔ ساختگی است.")
                        Button(
                            { onOpen(LoanDestinations.SUMMARY) },
                            Modifier.fillMaxWidth().testTag("loan-current"),
                        ) {
                            Text("تسهیلات جاری و پرداخت اقساط")
                        }
                        OutlinedButton(
                            { onOpen(LoanDestinations.GROUP) },
                            Modifier.fillMaxWidth().testTag("loan-groups"),
                        ) {
                            Text("گروه‌های تسهیلات — فقط مشاهده")
                        }
                    }
                    LoanDestinations.SUMMARY -> LoanSummary(onLoan)
                    LoanDestinations.GROUP,
                    LoanDestinations.GROUP_DETAIL ->
                        LoanGroupContent(page, groupKey, onGroup, onOpen, onUnavailable)
                    else -> {
                        val loan = LoanFixture.loans.first { it.id == loanId }
                        if (page == LoanDestinations.DETAIL) {
                            LoanDetail(
                                loan,
                                selected,
                                source,
                                paymentId,
                                onSelected,
                                onSource,
                                onPaymentId,
                                onOpen,
                            )
                        } else {
                            LoanReview(loan, selected, source, paymentId, onBack, onUnavailable)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanSummary(onLoan: (String) -> Unit) {
    var corporate by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false to "تسهیلات انفرادی", true to "تسهیلات شرکتی").forEach { (value, title) ->
            FilterChip(
                corporate == value,
                { corporate = value },
                { Text(title) },
                Modifier.testTag("loan-category-$value"),
            )
        }
    }
    LoanFixture.loans
        .filter { (it.corporateTitle != null) == corporate }
        .forEach { loan ->
            loan.corporateTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            OutlinedCard(
                Modifier.fillMaxWidth().testTag("loan-${loan.id}").clickable(role = Role.Button) {
                    onLoan(loan.id)
                }
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(loan.title, style = MaterialTheme.typography.titleMedium)
                    TransferValue("شماره تسهیلات", loan.fileNumber)
                    TransferValue("نوع تسهیلات", loan.loanType)
                    TransferValue("مبلغ تسهیلات (ریال)", persianNumber(loan.principalIrr))
                }
            }
        }
}

@Composable
private fun LoanDetail(
    loan: LoanRecord,
    selected: List<Int>,
    source: String,
    paymentId: String,
    onSelected: (List<Int>) -> Unit,
    onSource: (String) -> Unit,
    onPaymentId: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var shared by rememberSaveable { mutableStateOf(false) }
    var paymentIdShown by rememberSaveable { mutableStateOf(false) }
    Text(loan.title, style = MaterialTheme.typography.titleMedium)
    TransferValue("شماره تسهیلات", loan.fileNumber)
    TransferValue("نوع تسهیلات", loan.loanType)
    TransferValue("مبلغ تسهیلات (ریال)", persianNumber(loan.principalIrr))
    TransferValue("مبلغ پرداخت‌شده (ریال)", persianNumber(loan.paidIrr))
    TransferValue("مانده بدهی (ریال)", persianNumber(loan.totalDebtIrr))
    TransferValue("حساب تضمین", loan.guaranteeAccount)
    Text("اقساط", style = MaterialTheme.typography.titleMedium)
    if (loan.installments.isEmpty()) Text("موردی برای نمایش وجود ندارد.")
    loan.installments.forEachIndexed { index, installment ->
        val enabled = installment.status != LoanInstallmentStatus.PAID
        OutlinedCard(
            Modifier.fillMaxWidth().testTag("loan-installment-$index").selectable(
                index in selected,
                enabled,
                Role.Checkbox,
            ) {
                error = LoanFixture.selectionError(loan, selected, index)
                if (error == null)
                    onSelected(if (index in selected) selected - index else selected + index)
            }
        ) {
            Row(Modifier.padding(12.dp)) {
                Checkbox(index in selected, null, enabled = enabled)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("قسط ${persianNumber(installment.number.toLong())}")
                    TransferValue("سررسید", installment.dueDate)
                    Text(
                        when (installment.status) {
                            LoanInstallmentStatus.PAID -> "پرداخت شده"
                            LoanInstallmentStatus.UNPAID -> "پرداخت نشده"
                            LoanInstallmentStatus.OVERDUE -> "معوق"
                            LoanInstallmentStatus.TRACKABLE_OVERDUE -> "دارای بدهی معوق"
                        }
                    )
                    TransferValue("مبلغ قسط (ریال)", persianNumber(installment.amountIrr))
                    TransferValue("جریمه (ریال)", persianNumber(installment.penaltyIrr))
                    TransferValue("پرداخت‌شده (ریال)", persianNumber(installment.paidIrr))
                    TransferValue("مانده قسط (ریال)", persianNumber(installment.remainingIrr))
                }
            }
        }
    }
    error?.let { Text(it, Modifier.testTag("loan-error"), color = MaterialTheme.colorScheme.error) }
    Text(
        "جمع اقساط انتخابی: ${persianNumber(LoanFixture.selectedSumIrr(loan, selected))} ریال",
        Modifier.testTag("loan-selected-sum"),
    )
    if (loan.installments.any { it.status != LoanInstallmentStatus.PAID }) {
        Button(
            {
                error = if (selected.isEmpty()) LoanFixture.SELECT_ERROR else null
                if (error == null) picker = true
            },
            Modifier.fillMaxWidth().testTag("loan-review"),
        ) {
            Text("پرداخت — بازبینی محلی")
        }
    }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("انتخاب حساب مبدا") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("حساب‌های ساختگی")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(false to "انفرادی", true to "مشترک").forEach { (value, label) ->
                            FilterChip(
                                shared == value,
                                { shared = value },
                                { Text(label) },
                                Modifier.testTag("loan-source-shared-$value"),
                            )
                        }
                    }
                    AccountFixture.accounts
                        .filter { (it.ownership == "مشترک") == shared }
                        .forEach { account ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .testTag("loan-source-${account.key}")
                                    .selectable(source == account.key, role = Role.RadioButton) {
                                        error = null
                                        onSource(account.key)
                                    }
                            ) {
                                RadioButton(source == account.key, null)
                                Text("${account.type}\n${account.number}")
                            }
                        }
                    TextButton(
                        { paymentIdShown = !paymentIdShown },
                        Modifier.testTag("loan-payment-id-toggle"),
                    ) {
                        Text("افزودن شناسه برداشت")
                    }
                    if (paymentIdShown)
                        PaymentInput(
                            "loan-payment-id",
                            "شناسه برداشت (اختیاری)",
                            paymentId,
                            null,
                            "حداکثر 17 رقم",
                        ) {
                            // The recovered callback passes text through; server checks are
                            // unknown.
                            onPaymentId(it)
                        }
                    error?.let {
                        Text(
                            it,
                            Modifier.testTag("loan-source-error"),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    {
                        error = LoanFixture.reviewError(loan, selected, source)
                        if (error == null) {
                            picker = false
                            onOpen(LoanDestinations.CONFIRM)
                        }
                    },
                    Modifier.testTag("loan-source-confirm"),
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ picker = false }) { Text("انصراف") } },
        )
    }
    BackHandler(picker) { picker = false }
}

@Composable
private fun LoanReview(
    loan: LoanRecord,
    selected: List<Int>,
    source: String,
    paymentId: String,
    onBack: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Text("پرداخت قسط (${persianNumber(selected.size.toLong())} قسط)")
    PaymentSource(source)
    TransferValue("بابت", loan.fileNumber)
    TransferValue("حساب تضمین", loan.guaranteeAccount)
    TransferValue("شناسه برداشت:", paymentId.ifEmpty { "-" })
    selected.forEach { index ->
        val item = loan.installments[index]
        TransferValue(
            "قسط ${persianNumber(item.number.toLong())} (ریال)",
            persianNumber(item.remainingIrr),
        )
    }
    val sum = LoanFixture.selectedSumIrr(loan, selected)
    if (sum == loan.totalDebtIrr && loan.discountIrr > 0L) {
        TransferValue("مانده بدهی (ریال)", persianNumber(loan.totalDebtIrr))
        TransferValue("تخفیف تسویه زودهنگام (ریال)", persianNumber(loan.discountIrr))
    }
    Text(
        "مبلغ قابل پرداخت: ${persianNumber(LoanFixture.payableIrr(loan, selected))} ریال",
        Modifier.testTag("loan-payable"),
    )
    Text("این بازبینی محلی است؛ هیچ پرداختی انجام نمی‌شود.")
    OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("loan-correct")) {
        Text("اصلاح انتخاب اقساط و مبدا")
    }
    PaymentActions("تایید و پرداخت", "پرداخت تسهیلات", onBack, onUnavailable)
}
