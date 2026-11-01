package org.ragbot.replica

import java.io.Serializable

/** TL-RECOVERY: each toll's inquiry, result and (except pollution) confirmation pages. */
internal enum class TollService(
    val key: String,
    val title: String,
    val inquiry: String,
    val result: String,
    val confirm: String?,
) {
    ANNUAL("AUTHENTICATED_HOME_ANNUAL_TOLL", "عوارض سالانه", "S-241", "S-242", "S-240"),
    TRAFFIC("AUTHENTICATED_HOME_TRAFFIC_TOLL", "عوارض شهرداری", "S-275", "S-276", "S-274"),
    FREEWAY("AUTHENTICATED_HOME_FREEWAY_TOLL", "عوارض آزادراهی", "S-250", "S-251", "S-249"),
    // Recovered: the pollution inquiry ends on its quota result; it has no payment step.
    POLLUTION(
        "AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY",
        "تردد طرح آلودگی هوا",
        "S-257",
        "S-258",
        null,
    );

    companion object {
        fun ofKey(key: String) = entries.find { it.key == key }

        fun of(page: String) =
            entries.first { page == it.inquiry || page == it.result || page == it.confirm }
    }
}

internal object TollDestinations {
    const val RECEIPT = "S-261"
    val implemented =
        TollService.entries.flatMap { listOfNotNull(it.inquiry, it.result, it.confirm) }.toSet() +
            RECEIPT
}

/** A charge row; annual rows also carry penalty and depreciation, dated rows a date. */
internal data class TollItem(
    val id: String,
    val title: String,
    val price: Long,
    val penalty: Long = 0L,
    val frazzle: Long = 0L,
)

/** Non-secret inquiry and review fields. The OTP is never kept. */
internal data class TollDraft(
    val service: TollService = TollService.ANNUAL,
    val plate: List<String> = listOf("", "", "", ""),
    val automobile: Boolean = true,
    val mine: Boolean = true,
    val code: String = "",
    val phone: String = "",
    val vin: String = "",
    val selected: List<String> = emptyList(),
    val account: String = "",
) : Serializable

internal object TollRules {
    const val PLATE_ERROR = "شماره پلاک را به صورت کامل وارد نمایید."
    const val SELECT_ITEM = "یکی از موارد را جهت پرداخت انتخاب نمایید"

    /** The plate as one string: automobile 2+2+3+2 = 9, motorcycle 3+5 = 8 characters. */
    fun plateNumber(draft: TollDraft): String =
        if (draft.automobile) {
            val (first, letter, second, region) = draft.plate
            first +
                (TollFixture.letters.find { it.second == letter }?.first ?: "") +
                second +
                region
        } else draft.plate.take(2).joinToString("")

    /** Recovered national-code check. */
    fun nationalCode(value: String): Boolean {
        if (value.length != 10 || value.any { it !in '0'..'9' } || value.toSet().size == 1)
            return false
        val r = (0 until 9).sumOf { (value[it] - '0') * (10 - it) } % 11
        return value[9] - '0' == if (r < 2) r else 11 - r
    }

    /** Recovered inquiry checks: plate length, then the owner fields for others, then the VIN. */
    fun errors(draft: TollDraft): Map<String, String> = buildMap {
        val plate = plateNumber(draft)
        val automobileOnly = draft.service == TollService.POLLUTION
        if (
            (automobileOnly && !draft.automobile) || plate.length != if (draft.automobile) 9 else 8
        ) {
            put("plate", PLATE_ERROR)
            return@buildMap
        }
        if (!draft.mine) {
            val code = latinDigits(draft.code)
            val phone = latinDigits(draft.phone)
            when {
                code.isEmpty() -> put("code", PaymentRules.MANDATORY)
                !nationalCode(code) -> put("code", PaymentRules.INVALID)
            }
            when {
                phone.isEmpty() -> put("phone", PaymentRules.MANDATORY)
                phone.length != 11 || !phone.startsWith("09") -> put("phone", PaymentRules.INVALID)
            }
        }
        if (draft.service == TollService.ANNUAL && draft.vin.isBlank())
            put("vin", PaymentRules.MANDATORY)
    }

    /** Designed for mock: only the first saved automobile plate has dues. */
    fun items(draft: TollDraft): List<TollItem> =
        if (plateNumber(draft) != TollFixture.DUES_PLATE) emptyList()
        else TollFixture.items.getValue(draft.service)

    fun total(draft: TollDraft): Long =
        when (draft.service) {
            TollService.ANNUAL ->
                items(draft).sumOf { it.price + it.penalty + it.frazzle } +
                    if (items(draft).isEmpty()) 0L else TollFixture.COMMISSION
            else -> items(draft).filter { it.id in draft.selected }.sumOf { it.price }
        }
}

internal object TollFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ استعلام یا پرداخت عوارضی ارسال نمی‌شود."
    const val INQUIRY_FEE = 5_000L
    const val COMMISSION = 10_000L

    // The service supplies the plate letters with their codes; these are synthetic.
    val letters = listOf("01" to "الف", "02" to "ب", "03" to "ج", "04" to "د")
    const val DUES_PLATE = "120234567"

    val items =
        mapOf(
            TollService.ANNUAL to
                listOf(
                    TollItem("annual-1404", "عوارض سال ۱۴۰۴", 450_000L, 45_000L, 20_000L),
                    TollItem("annual-1405", "عوارض سال ۱۴۰۵", 500_000L),
                ),
            TollService.TRAFFIC to
                listOf(
                    TollItem("traffic-1", "۱۴۰۵/۰۷/۲۰", 120_000L),
                    TollItem("traffic-2", "۱۴۰۵/۰۷/۲۳", 85_000L),
                    TollItem("traffic-3", "۱۴۰۵/۰۸/۰۲", 240_000L),
                ),
            TollService.FREEWAY to
                listOf(
                    TollItem("freeway-1", "۱۴۰۵/۰۷/۱۵", 36_000L),
                    TollItem("freeway-2", "۱۴۰۵/۰۸/۰۱", 52_000L),
                ),
            TollService.POLLUTION to emptyList(),
        )

    // Pollution quota for the season: allowed, remaining and consumed per season.
    const val QUOTA_ALLOWED = 20
    const val QUOTA_REMAINING = 7
    val consumed = listOf(Triple(1405, 2, 8), Triple(1405, 3, 5))
}
