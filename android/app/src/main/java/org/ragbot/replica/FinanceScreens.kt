package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal object FinanceDestinations {
    const val MAIN = "S-436"
    const val BUDGETS = "S-434"
    const val BUDGET = "S-435"
    val implemented = setOf(MAIN, BUDGETS, BUDGET)
}

/** One spending category; [amount] is what the user spent, in whole rials. */
internal data class Spending(val category: String, val title: String, val amount: Long)

/** The dashboard's figures: exact sums of transactions.json and this session. */
internal object FinanceRules {
    fun spending() =
        MockData.current.categories.map { (key, title) ->
            val listed = MockData.current.spending.filter { it.category == key }
            Spending(key, title, listed.sumOf { it.amount } + MockBank.sessionSpending(key))
        }

    fun total() = spending().sumOf { it.amount }

    fun limitError(value: String) =
        if (TransferRules.amountRial(value) == null) PaymentRules.INVALID else null
}

/** The personal finance dashboard and budgets. Designed for mock. */
@Composable
internal fun FinancePage(
    page: String,
    category: String,
    onCategory: (String) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val title =
        when (page) {
            FinanceDestinations.MAIN -> "عملکرد مالی"
            FinanceDestinations.BUDGETS -> "بودجه‌ها"
            else -> "جزئیات بودجه"
        }
    PageFrame(page, title, onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (page) {
                FinanceDestinations.MAIN -> {
                    Text(
                        "مجموع هزینه‌ها: ${persianNumber(FinanceRules.total())} ریال",
                        Modifier.testTag("finance-total"),
                    )
                    val largest = FinanceRules.spending().maxOf { it.amount }.coerceAtLeast(1)
                    FinanceRules.spending().forEach { SpendingBar(it, largest) }
                    Button(
                        { onOpen(FinanceDestinations.BUDGETS) },
                        Modifier.fillMaxWidth().testTag("finance-budgets"),
                    ) {
                        Text("بودجه‌ها")
                    }
                }
                FinanceDestinations.BUDGETS ->
                    FinanceRules.spending().forEach { item ->
                        OutlinedCard(
                            Modifier.fillMaxWidth()
                                .testTag("budget-${item.category}")
                                .clickable(role = Role.Button) {
                                    onCategory(item.category)
                                    onOpen(FinanceDestinations.BUDGET)
                                }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                PaymentRow("دسته", item.title)
                                PaymentRow("هزینه (ریال)", persianNumber(item.amount))
                                PaymentRow(
                                    "بودجه (ریال)",
                                    persianNumber(MockBank.budget(item.category)),
                                )
                            }
                        }
                    }
                else -> BudgetForm(category, onBack)
            }
        }
    }
}

@Composable
private fun SpendingBar(item: Spending, largest: Long) {
    Column(Modifier.testTag("finance-${item.category}")) {
        PaymentRow(item.title, "${persianNumber(item.amount)} ریال")
        Box(
            Modifier.fillMaxWidth(item.amount.toFloat() / largest)
                .height(8.dp)
                .background(Color(0xFF308A48))
        )
    }
}

@Composable
private fun BudgetForm(category: String, onBack: () -> Unit) {
    var value by pageValue("budget-limit", MockBank.budget(category).toString())
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val item = FinanceRules.spending().first { it.category == category }
    PaymentRow("دسته", item.title)
    PaymentRow("هزینه (ریال)", persianNumber(item.amount))
    PaymentInput("budget-limit", "بودجه ماهانه (ریال)", value, error) {
        value = it
        error = null
    }
    if (item.amount > (latinDigits(value).toLongOrNull() ?: 0L))
        Text("هزینه این دسته از بودجه بیشتر است.", Modifier.testTag("budget-over"))
    Button(
        {
            error = FinanceRules.limitError(value)
            if (error == null) {
                MockBank.setBudget(category, latinDigits(value).toLong())
                onBack()
            }
        },
        Modifier.fillMaxWidth().testTag("budget-save"),
    ) {
        Text("ذخیره")
    }
}
