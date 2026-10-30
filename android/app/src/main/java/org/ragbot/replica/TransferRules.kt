package org.ragbot.replica

/** Local preview amount parsing, not a recovered bank limit or eligibility decision. */
internal object TransferRules {
    fun amountRial(value: String, maxDigits: Int? = null): Long? {
        val digits = latinDigits(value)
        if (
            digits.isEmpty() ||
                digits.any { it !in '0'..'9' } ||
                (maxDigits != null && digits.length > maxDigits)
        )
            return null
        return digits.toLongOrNull()?.takeIf { it > 0 }
    }
}
