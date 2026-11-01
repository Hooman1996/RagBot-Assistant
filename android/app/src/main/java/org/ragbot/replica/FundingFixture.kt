package org.ragbot.replica

import java.io.Serializable

/**
 * FN-RECOVERY: the four increase-balance graphs; the hosts S-220/S-229 are containers, not pages.
 */
internal enum class FundingFlow(
    val amount: String,
    val gateway: String,
    val waiting: String,
    val receipt: String,
    val wallet: Boolean,
    val inFlow: Boolean,
) {
    ACCOUNT_STANDALONE("S-222", "S-228", "S-224", "S-217", false, false),
    ACCOUNT_IN_FLOW("S-221", "S-227", "S-223", "S-232", false, true),
    // The standalone wallet result opens the existing wallet receipt (S-113).
    WALLET_STANDALONE("S-231", "S-225", "S-235", WalletOverviewDestinations.RECEIPT, true, false),
    WALLET_IN_FLOW("S-230", "S-226", "S-234", "S-233", true, true);

    val pages = listOf(amount, gateway, waiting, receipt)

    companion object {
        fun of(page: String) = entries.first { page in it.pages }
    }
}

internal object FundingDestinations {
    // S-219/S-218 (legacy amount and result) reach only the unresolved web view S-236.
    val implemented =
        FundingFlow.entries.flatMap { it.pages }.toSet() - WalletOverviewDestinations.RECEIPT
}

/** Designed for mock: the synthetic gateway return the user picks on the local stand-in. */
internal enum class FundingOutcome(val title: String) {
    SUCCESS("بازگشت موفق نمونه"),
    PENDING("بازگشت موفق با تاخیر نتیجه"),
    FAILED("بازگشت ناموفق نمونه"),
    UNKNOWN("قطع ارتباط هنگام دریافت نتیجه"),
}

/** [target] is an account key or a wallet number. No card or gateway credential is kept. */
internal data class FundingDraft(
    val target: String,
    val amount: String = FundingFixture.DEFAULT_AMOUNT.toString(),
    val outcome: FundingOutcome = FundingOutcome.SUCCESS,
    val pendingSeen: Boolean = false,
) : Serializable

internal object FundingRules {
    /** Recovered: the first 13 characters are read; anything else counts as zero. */
    fun amount(text: String): Long = latinDigits(text).take(13).toLongOrNull() ?: 0L

    fun plus(value: Long) = value + FundingFixture.STEP

    fun minus(value: Long) = (value - FundingFixture.STEP).coerceAtLeast(0L)

    /** Recovered order: empty, the transaction limits, then the proposed range. */
    fun error(value: Long): String? =
        when {
            value == 0L -> PaymentRules.MANDATORY
            value > FundingFixture.limits.last ->
                "حداکثر مبلغ افزایش موجودی ${persianNumber(FundingFixture.limits.last)} ریال است."
            value < FundingFixture.limits.first ->
                "حداقل مبلغ افزایش موجودی ${persianNumber(FundingFixture.limits.first)} ریال است."
            value < FundingFixture.proposed.first ->
                "مقدار وارد شده از حداقل مبلغ مورد نیازکم تر است."
            value > FundingFixture.proposed.last -> "مقدار وارد شده از حداکثر مبلغ بیش تر است."
            else -> null
        }
}

internal object FundingFixture {
    const val NOTICE =
        "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ درگاه، نشانی یا برنامهٔ پرداختی باز نمی‌شود و مبلغی شارژ نمی‌شود."
    const val STEP = 100_000L
    const val DEFAULT_AMOUNT = 1_000_000L
    const val TRACKING = "DEMO-IPG-0001"

    // Server-supplied values; all Designed for mock. Exact IRR integers.
    val limits = 10_000L..500_000_000L
    val proposed = 50_000L..200_000_000L
    val proposedAmounts = listOf(500_000L, 1_000_000L, 5_000_000L)
}
