package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** One form field of a service. [choices] are key and title; a field with none is typed. */
internal data class FlowField(
    val key: String,
    val label: String,
    val type: String = "text",
    val choices: () -> List<Pair<String, String>> = { emptyList() },
    val check: (String) -> String? = ::required,
)

internal fun required(value: String) = if (value.isBlank()) PaymentRules.MANDATORY else null

internal fun digits(count: Int): (String) -> String? = { value ->
    val text = latinDigits(value)
    if (text.length == count && text.all { it in '0'..'9' }) null else PaymentRules.INVALID
}

internal fun amountCheck(value: String) =
    if (TransferRules.amountRial(value) == null) PaymentRules.INVALID else null

internal fun money(key: String, label: String) = FlowField(key, label, "money", check = ::amountCheck)

internal fun choice(key: String, label: String, choices: () -> List<Pair<String, String>>) =
    FlowField(key, label, "choice", choices)

internal fun accountChoice(key: String = "source", label: String = "حساب مبدا") =
    FlowField(key, label, "ref", { AccountFixture.accounts.map { it.key to chequeAccount(it.key) } })

/**
 * One page of a service. [show] lists read-only rows. A step with [submit] is the review page: its
 * confirm is the user's and goes to the bank.
 */
internal data class FlowStep(
    val page: String,
    val title: String,
    val fields: List<FlowField> = emptyList(),
    val show: (Map<String, String>) -> List<Pair<String, String>> = { emptyList() },
)

/**
 * A service built from [steps]. The last step is the review. When [submit] is set its confirm goes
 * to the bank, and [result] shows the receipt; [otp] asks for the code first.
 */
internal class ServiceFlow(
    val key: String,
    val steps: List<FlowStep>,
    val result: String? = null,
    val otp: Boolean = true,
    val submit: ((Map<String, String>) -> BankResult)? = null,
)

/** A generic service page: fields, Continue, and on the last step the user's confirm. */
@Composable
internal fun ServicePage(
    flow: ServiceFlow,
    page: String,
    values: Map<String, String>,
    onValues: (Map<String, String>) -> Unit,
    onNext: (String) -> Unit,
    onBack: () -> Unit,
) {
    val index = flow.steps.indexOfFirst { it.page == page }
    val step = flow.steps[index]
    var attempted by rememberSaveable { mutableStateOf(false) }
    val errors =
        if (attempted)
            step.fields.mapNotNull { f ->
                f.check(values[f.key].orEmpty())?.let { f.key to it }
            }.toMap()
        else emptyMap()
    PageFrame(page, step.title, onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(PaymentFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
            step.show(values).forEach { (label, value) -> TransferValue(label, value) }
            step.fields.forEach { field ->
                FlowInput(field, values[field.key].orEmpty(), errors[field.key]) {
                    onValues(values + (field.key to it))
                }
            }
            val last = index == flow.steps.lastIndex
            if (!last)
                Button(
                    {
                        attempted = true
                        if (step.fields.all { it.check(values[it.key].orEmpty()) == null })
                            onNext(flow.steps[index + 1].page)
                    },
                    Modifier.fillMaxWidth().testTag("svc-continue"),
                ) {
                    Text("ادامه")
                }
            else
                flow.submit?.let { submit ->
                    BankButton("تایید", "svc-confirm", flow.result, flow.otp) {
                        attempted = true
                        if (step.fields.any { it.check(values[it.key].orEmpty()) != null })
                            MockBank.incomplete()
                        else submit(values)
                    }
                }
            OutlinedButton(onBack, Modifier.fillMaxWidth().testTag("svc-back")) { Text("بازگشت") }
        }
    }
}

@Composable
private fun FlowInput(field: FlowField, value: String, error: String?, onChange: (String) -> Unit) {
    val choices = field.choices()
    if (choices.isEmpty()) {
        PaymentInput("svc-${field.key}", field.label, value, error) { onChange(it) }
        return
    }
    Text(field.label, style = MaterialTheme.typography.labelMedium)
    choices.forEach { (key, title) ->
        FilterChip(
            value == key,
            { onChange(key) },
            { Text(title) },
            Modifier.fillMaxWidth().testTag("svc-${field.key}-$key"),
        )
    }
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
}

/** The receipt page of a service. */
@Composable
internal fun ServiceResultPage(page: String, onBack: () -> Unit, onUnavailable: (String) -> Unit) {
    PageFrame(page, "نتیجه عملیات", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            BankReceipt(onBack, onUnavailable)
        }
    }
}

/** A plain menu page: one button per entry. */
@Composable
internal fun MenuPage(
    page: String,
    title: String,
    entries: List<Triple<String, String, String>>,
    onOpen: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    PageFrame(page, title, onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            entries.forEach { (key, label, target) ->
                Button({ onOpen(key, target) }, Modifier.fillMaxWidth().testTag("svc-menu-$key")) {
                    Text(label)
                }
            }
        }
    }
}
