package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockBankTest {
    private val reza = "0000000000000091"
    private val source = "0000000000000018"

    @Before
    fun freshSession() {
        MockBank.clock = { "۱۴۰۵/۰۸/۱۴ - ۱۰:۰۰" }
        MockBank.reset()
    }

    private fun transfer(amount: String, destination: String = reza, password: String = "12345") =
        MockBank.cardTransfer(
            CardTransferDraft(source, destination, amount),
            "123",
            "0812",
            password,
        )

    private fun message(result: BankResult) = (result as BankResult.Failed).message

    private val messages
        get() = MockData.current.messages

    @Test
    fun aTransferToRezaMovesExactMoneyAndAppearsInHistory() {
        val done = transfer("250000") as BankResult.Done
        assertEquals(12_250_000L, MockBank.balance("account-1"))
        assertEquals("رضا", done.receipt.destination)
        assertEquals(done.receipt.trace, MockBank.transactions.first().key)
        assertEquals(250_000L, MockBank.transactions.first().amount)
        MockBank.reset()
        assertEquals(12_500_000L, MockBank.balance("account-1"))
    }

    @Test
    fun aFailedTransferChangesNothing() {
        val before = MockBank.transactions.size
        assertEquals(messages["insufficient-balance"], message(transfer("12500001")))
        assertEquals(messages["wrong-otp"], message(transfer("1000", password = "00000")))
        assertEquals(messages["invalid-card"], message(transfer("1000", "0000000000000019")))
        assertEquals(messages["recipient-not-found"], message(transfer("1000", "4000000000000002")))
        assertEquals(12_500_000L, MockBank.balance("account-1"))
        assertEquals(before, MockBank.transactions.size)
    }

    @Test
    fun cardChecksumAndIbanRecipientsAreChecked() {
        assertTrue(MockBank.validCard(reza))
        assertFalse(MockBank.validCard("0000000000000092"))
        assertFalse(MockBank.validCard("123"))
        assertTrue(MockBank.knownIban("IR070000000000000000000077"))
        assertFalse(MockBank.knownIban("IR080000000000000000000077"))
    }

    @Test
    fun aBillPaysOnceAndAnUnknownBillFails() {
        val id = "9000000000001"
        assertTrue(MockBank.bill("account-1", id, "1250001") is BankResult.Done)
        assertEquals(messages["bill-paid"], message(MockBank.bill("account-1", id, "1250001")))
        assertEquals(messages["bill-not-found"], message(MockBank.bill("account-1", id, "1")))
        assertEquals(11_250_000L, MockBank.balance("account-1"))
    }

    @Test
    fun walletsMoveMoneyBothWays() {
        val wallet = "0000000000101"
        assertTrue(MockBank.walletCharge("account-1", wallet, 1_000L) is BankResult.Done)
        assertEquals(3_251_000L, MockBank.walletBalance(wallet))
        assertEquals(12_499_000L, MockBank.balance("account-1"))
        val result =
            MockBank.walletPay("wallet-decharge", "برداشت", wallet, 3_251_001L, "حساب")
        assertEquals(messages["insufficient-balance"], message(result))
    }

    @Test
    fun aCardShowsItsAccountsLiveBalance() {
        transfer("1000")
        val card = CardFixture.cards.first()
        assertEquals(12_499_000L, card.accountBalance)
        assertEquals(12_499_000L, card.cardBalance)
    }
}
