package org.ragbot.replica

import java.io.Serializable

internal object CardTransferDestinations {
    const val ANONYMOUS = "S-174"
    const val WITHOUT_SOURCE = "S-175"
    const val CONFIRM = "S-176"
    const val RECEIPT = "S-093"
    val implemented = setOf(ANONYMOUS, WITHOUT_SOURCE, CONFIRM, RECEIPT)
}

/** Non-secret local preview fields. Card credentials never enter this saved draft. */
internal data class CardTransferDraft(
    val source: String = "",
    val destination: String = "",
    val amount: String = "",
    val note: String = "",
    val addFavorite: Boolean = false,
) : Serializable

internal object CardTransferFixture {
    const val source = "0000000000000001"
    const val destination = "0000000000000002"
    val receipt = CardTransferDraft(source, destination, "250000")
}

internal object CardTransferRules {
    // Shape checks enable inspection; the fixture PANs deliberately identify no real cards.
    // Bank discovery, checksum/eligibility and inquiry remain unavailable.
    fun errors(draft: CardTransferDraft): Map<String, String> = buildMap {
        val source = latinDigits(draft.source)
        val destination = latinDigits(draft.destination)
        if (source.length != 16 || source.any { it !in '0'..'9' })
            put("source", "شماره کارت مبدا باید ۱۶ رقم باشد.")
        if (destination.length != 16 || destination.any { it !in '0'..'9' })
            put("destination", "شماره کارت مقصد باید ۱۶ رقم باشد.")
        else if (source == destination) put("destination", "کارت مبدا و مقصد نباید یکسان باشد.")
        if (TransferRules.amountRial(draft.amount, 11) == null)
            put("amount", "مبلغ ریالی مثبت، حداکثر ۱۱ رقم، وارد کنید.")
    }
}
