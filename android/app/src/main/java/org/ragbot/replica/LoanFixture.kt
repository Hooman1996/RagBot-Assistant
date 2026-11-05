package org.ragbot.replica

internal object LoanDestinations {
    const val MENU = "S-324"
    const val SUMMARY = "S-338"
    const val DETAIL = "S-306"
    const val CONFIRM = "S-331"
    const val GROUP = "S-307"
    const val GROUP_DETAIL = "S-309"
    val implemented = setOf(MENU, SUMMARY, DETAIL, CONFIRM, GROUP, GROUP_DETAIL)
}

internal enum class LoanInstallmentStatus {
    PAID,
    UNPAID,
    OVERDUE,
    TRACKABLE_OVERDUE,
}

internal data class LoanInstallment(
    val number: Int,
    val dueDate: String,
    val status: LoanInstallmentStatus,
    val amountIrr: Long,
    val penaltyIrr: Long,
    val paidIrr: Long,
    val remainingIrr: Long,
)

internal data class LoanRecord(
    val id: String,
    val title: String,
    val fileNumber: String,
    val principalIrr: Long,
    val paidIrr: Long,
    val totalDebtIrr: Long,
    val discountIrr: Long,
    val guaranteeAccount: String,
    val installments: List<LoanInstallment>,
    val corporateTitle: String? = null,
    val loanType: String = "نوع نمایشی",
)

/** A product of the generic loan request; Designed for mock. [rate] is a yearly percent. */
internal data class LoanProduct(
    val key: String,
    val title: String,
    val maxAmount: Long,
    val maxMonths: Int,
    val rate: Long,
)

internal object LoanRequestRules {
    /** The monthly installment in whole rials: simple interest, rounded down. */
    fun installment(amount: Long, months: Long, rate: Long): Long {
        if (amount <= 0 || months <= 0) return 0
        return (amount + amount * rate * months / 1200) / months
    }
}

/** LOANS-RECOVERY: recovered presentation rules; every record and IRR amount is synthetic. */
internal object LoanFixture {
    const val ORDER_ERROR = "انتخاب باید به ترتیب تاریخ سررسید باشد."
    const val OVERDUE_ERROR = "اقساط دارای وضعیت بدهی را انتخاب کنید."
    const val SELECT_ERROR = "یکی از اقساط را جهت پرداخت انتخاب کنید."
    const val PAID_ERROR = "این قسط پرداخت شده است."
    const val SOURCE_ERROR = "حساب مبدا را انتخاب کنید."

    val loans
        get() =
            MockData.current.loans.map { loan ->
                val paid =
                    loan.installments.filter { MockBank.settled("loan:${loan.id}:${it.number}") }
                val amount = paid.sumOf { it.remainingIrr }
                loan.copy(
                    paidIrr = loan.paidIrr + amount,
                    totalDebtIrr = loan.totalDebtIrr - amount,
                    installments =
                        loan.installments.map {
                            if (it in paid)
                                it.copy(
                                    status = LoanInstallmentStatus.PAID,
                                    paidIrr = it.paidIrr + it.remainingIrr,
                                    remainingIrr = 0,
                                )
                            else it
                        },
                )
            }

    /** Original list indices preserve the due-date order returned by the detail response. */
    fun selectionError(loan: LoanRecord, selected: List<Int>, index: Int): String? {
        val item = loan.installments.getOrNull(index) ?: return SELECT_ERROR
        if (item.status == LoanInstallmentStatus.PAID) return PAID_ERROR
        if (index in selected) return if (index + 1 in selected) ORDER_ERROR else null
        val firstTrackable =
            loan.installments.indexOfFirst { it.status == LoanInstallmentStatus.TRACKABLE_OVERDUE }
        if (firstTrackable >= 0 && item.status != LoanInstallmentStatus.TRACKABLE_OVERDUE) {
            return OVERDUE_ERROR
        }
        val first =
            if (firstTrackable >= 0) firstTrackable
            else loan.installments.indexOfFirst { it.status != LoanInstallmentStatus.PAID }
        return if (index == first || index - 1 in selected) null else ORDER_ERROR
    }

    fun selectedSumIrr(loan: LoanRecord, selected: List<Int>): Long =
        selected.sumOf { loan.installments[it].remainingIrr }

    fun payableIrr(loan: LoanRecord, selected: List<Int>): Long {
        val amount = selectedSumIrr(loan, selected)
        return if (amount == loan.totalDebtIrr && loan.discountIrr > 0L) amount - loan.discountIrr
        else amount
    }

    fun reviewError(loan: LoanRecord, selected: List<Int>, source: String): String? {
        if (selected.isEmpty()) return SELECT_ERROR
        val checked = mutableListOf<Int>()
        for (index in selected.sorted()) {
            if (index in checked) return ORDER_ERROR
            selectionError(loan, checked, index)?.let {
                return it
            }
            checked.add(index)
        }
        return if (source.isBlank()) SOURCE_ERROR else null
    }
}
