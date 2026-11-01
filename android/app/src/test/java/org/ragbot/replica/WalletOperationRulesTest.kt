package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class WalletOperationRulesTest {
    private val personal = "0000000000101"
    private val receiver = "0000000000000201"

    @Test
    fun destinationChecksEmptyThenSameSourceThenSixteenCharacters() {
        assertEquals(PaymentRules.MANDATORY, WalletOperationRules.destinationError("", personal))
        assertEquals(
            WalletOperationRules.SAME,
            WalletOperationRules.destinationError("۰۰۰۰۰۰۰۰۰۰۱۰۱", personal),
        )
        assertEquals(
            PaymentRules.INVALID,
            WalletOperationRules.destinationError("000000000000020", personal),
        )
        assertNull(WalletOperationRules.destinationError(receiver, personal))
    }

    @Test
    fun onlyAMissingAmountIsRejectedLocally() {
        assertEquals(PaymentRules.MANDATORY, WalletOperationRules.amountError(""))
        assertNull(WalletOperationRules.amountError("۱"))
        // No client limit was recovered; the bank's pre-validation decides.
        assertNull(WalletOperationRules.amountError("999999999999"))
    }

    @Test
    fun eachOperationChecksItsOwnFields() {
        assertEquals(
            setOf("source", "destination", "amount"),
            WalletOperationRules.errors(WalletOperation.W2W, WalletOperationDraft()).keys,
        )
        assertEquals(
            setOf("destination", "amount"),
            WalletOperationRules.errors(WalletOperation.DECHARGE, WalletOperationDraft(personal))
                .keys,
        )
        assertEquals(
            setOf("mobile", "operator", "amount"),
            WalletOperationRules.errors(WalletOperation.MOBILE, WalletOperationDraft(personal)).keys,
        )
        assertEquals(
            mapOf("bill" to PaymentRules.INVALID, "pay" to PaymentRules.MANDATORY),
            WalletOperationRules.errors(
                WalletOperation.BILL,
                WalletOperationDraft(personal, billId = "12345"),
            ),
        )
        // A purchase without a scanned destination is not a field error.
        assertEquals(
            setOf("amount"),
            WalletOperationRules.errors(WalletOperation.BUY, WalletOperationDraft(personal)).keys,
        )
    }

    @Test
    fun previewNamesTheDestinationAndRejectsMoreThanTheSyntheticBalance() {
        val draft = WalletOperationDraft(personal, receiver, "3250000")
        assertEquals(
            WalletPreview("گیرندهٔ نمایشی", 3_250_000L) to null,
            WalletOperationRules.preview(WalletOperation.W2W, draft),
        )
        val (over, error) =
            WalletOperationRules.preview(WalletOperation.W2W, draft.copy(amount = "3250001"))
        assertNull(over)
        assertEquals("موجودی ساختگی مبدا برای این مبلغ کافی نیست.", error)
        assertNull(
            WalletOperationRules.preview(
                    WalletOperation.W2W,
                    draft.copy(destination = "0000000000000999"),
                )
                .first
        )
    }

    @Test
    fun chargeUsesTheAccountBalanceAndBillTheInquiryAmount() {
        // account-3 reports no balance, so only the bank could refuse it.
        assertEquals(
            WalletPreview("کاربر نمایشی", 99_000_000L),
            WalletOperationRules.preview(
                    WalletOperation.CHARGE,
                    WalletOperationDraft("account-3", personal, "99000000"),
                )
                .first,
        )
        val bill = WalletOperationDraft(personal, billId = "9000000000001", payId = "1250001")
        assertEquals(
            WalletPreview("شرکت آب نمایشی", 1_250_000L),
            WalletOperationRules.preview(WalletOperation.BILL, bill).first,
        )
        assertEquals(
            "این قبض در دادهٔ نمایشی پرداخت شده است.",
            WalletOperationRules.preview(
                    WalletOperation.BILL,
                    bill.copy(billId = "9000000000002", payId = "430002"),
                )
                .second,
        )
    }

    @Test
    fun scannerStaysUnregisteredAndEveryPageBelongsToOneOperation() {
        assertEquals(12, WalletOperationDestinations.implemented.size)
        assertFalse("S-108" in HomeDestinations.implemented)
        WalletOperation.entries.forEach {
            assertEquals(it, WalletOperation.of(it.form))
            assertEquals(it, WalletOperation.of(it.confirm))
        }
        assertEquals(
            listOf("S-104", "S-231"),
            WalletOperationFixture.chargeTypes.map { it.destination },
        )
    }
}
