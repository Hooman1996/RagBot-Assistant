package org.ragbot.replica

import java.io.Serializable

internal object DigitalChequeDestinations {
    const val ACTIVATION = "S-157"
    const val CARTABLE = "S-159"
    const val DETAIL = "S-158"
    const val TRANSFER = "S-151"
    const val CONFIRM = "S-152"
    val implemented = setOf(ACTIVATION, CARTABLE, DETAIL, TRANSFER, CONFIRM)
}

/** DC-RECOVERY: a digital cheque in the cartable; status texts come from the service. */
internal data class DigitalCheque(
    val sayadId: String,
    val amount: Long,
    val dueDate: String,
    val bank: String,
    val branch: String,
    val status: String,
    val guarantee: String,
    val block: String,
    val lock: String,
    val holderStatus: String,
    val reason: String,
    val description: String,
    val holders: List<String>,
    val digitalIban: String,
)

/** Non-secret transfer fields; [fixed] means the Sayad ID came from the detail page. */
internal data class DigitalTransferDraft(
    val sayadId: String = "",
    val accountKey: String = "account-1",
    val bounce: Boolean = false,
    val fixed: Boolean = false,
) : Serializable

internal object DigitalChequeRules {
    // Recovered: only an empty Sayad ID is rejected before the inquiry.
    fun sayadError(value: String) = if (value.isEmpty()) PaymentRules.MANDATORY else null

    fun search(query: String): List<DigitalCheque> =
        DigitalChequeFixture.cartable.filter {
            latinDigits(query).let { q -> q.isEmpty() || q in it.sayadId }
        }

    fun assigned(query: String): List<String> =
        DigitalChequeFixture.assigned.filter { latinDigits(query).let { q -> q in it } }

    /** Designed for mock inquiry by holder. */
    fun inquiry(sayadId: String) = DigitalChequeFixture.cartable.find { it.sayadId == sayadId }
}

internal object DigitalChequeFixture {
    const val NOTICE = "نسخه نمایشی با داده و بانک محلی؛ هیچ پول واقعی جابه‌جا نمی‌شود."
    val FEE
        get() = MockData.current.digitalChequeFee


    // The service supplies these texts as HTML; synthetic plain text here.
    const val ACTIVATION_TEXT =
        "شرایط نمایشی فعال‌سازی امضای چکاد. متن اصلی از سرویس دریافت می‌شود و امضا با برنامهٔ هامون انجام می‌شود."
    const val UPDATE_TEXT = "شرایط نمایشی همگام‌سازی با هامون. متن اصلی از سرویس دریافت می‌شود."

    val cartable
        get() = MockData.current.digitalCheques

    // Cheques assigned to the user, offered by the transfer picker.
    val assigned
        get() = cartable.map { it.sayadId }
}
