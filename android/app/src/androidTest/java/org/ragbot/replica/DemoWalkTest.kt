package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The owner's demo walk, in one session, with the money checked at the end. */
@RunWith(AndroidJUnit4::class)
class DemoWalkTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun click(tag: String) {
        compose.closeKeyboard(compose.activity)
        val node = compose.onNodeWithTag(tag)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
            // Not inside a scrolling parent.
        }
        node.performClick()
    }

    private fun shown(id: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(id).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun type(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    }

    private fun otp() {
        shown("otp-code")
        compose.onNodeWithTag("otp-code").performTextReplacement(PREVIEW_OTP)
        compose.onNodeWithTag("otp-ok").performClick()
    }

    private fun done(receipt: String) {
        shown(receipt)
        compose.onNodeWithTag("bank-receipt").assertExists()
        click("bank-receipt-done")
        shown("S-026")
    }

    private fun cardTransfer(amount: String) {
        click("menu-card-transfer")
        click("card-transfer-source-picker")
        click("card-transfer-source-card-1")
        click("card-transfer-favorites")
        click("card-transfer-contact-reza-card")
        type("card-transfer-amount", amount)
        click("card-transfer-review")
        type("card-transfer-cvv2", "123")
        type("card-transfer-expiry", "0812")
        type("card-transfer-password", PREVIEW_OTP)
        click("card-transfer-submit")
    }

    @Test
    fun theFullDemoWalk() {
        // Log in.
        click("login-start")
        click("intro-next")
        click("welcome-login")
        type("login-username", MockData.current.customer.username)
        type("login-password", MockData.current.customer.password)
        click("login-submit")
        shown("S-026")

        // Card-to-card to Reza succeeds.
        cardTransfer("250000")
        done("S-093")

        // A transfer over the balance fails with a clear message.
        cardTransfer("99999999")
        compose
            .onNodeWithTag("bank-failure")
            .assertTextContains(MockData.current.messages.getValue("insufficient-balance"))
        click("bank-failure-close")
        repeat(2) { click("page-back") }
        shown("S-026")

        // Pay a bill.
        click("menu-bill")
        type("bill-id", "9000000000001")
        type("bill-pay-id", "1250001")
        click("bill-continue")
        click("bill-accept")
        otp()
        done("S-092")

        // Buy mobile charge.
        click("menu-topup")
        type("topup-phone", "09010000000")
        click("topup-amount-200000")
        click("topup-continue")
        click("topup-accept")
        otp()
        done("S-094")

        // Check a Sayad cheque.
        click("menu-cheque")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_INQUIRY_BYHOLDER")
        click("svc-kind-holder")
        click("svc-continue")
        compose.onNodeWithTag("svc-sayad").performScrollTo().performTextReplacement("0000000000000321")
        click("svc-confirm")
        shown("S-166")
        click("bank-receipt-done")
        shown("S-026")

        // Pay a toll.
        click("menu-vehicle")
        click("vehicle-menu-AUTHENTICATED_HOME_TRAFFIC_TOLL")
        click("vehicle-plate-1")
        shown("S-275")
        click("toll-inquiry")
        click("toll-fee-account-1")
        otp()
        shown("S-276")
        click("toll-select-all")
        click("toll-pay")
        shown("S-274")
        click("cheque-account")
        click("cheque-account-account-1")
        click("payment-submit")
        otp()
        done("S-261")

        // Request a loan.
        click("menu-loan")
        click("loan-request")
        click("svc-product-personal")
        click("svc-continue")
        type("svc-amount", "120000000")
        type("svc-months", "12")
        click("svc-continue")
        type("svc-job", "کارمند")
        click("svc-continue")
        type("svc-guarantor", "رضا")
        type("svc-guarantorCode", "0000000019")
        click("svc-continue")
        click("svc-agree-yes")
        click("svc-continue")
        click("svc-note-issue")
        click("svc-continue")
        click("svc-sign-sign")
        click("svc-continue")
        click("svc-confirm")
        otp()
        done("S-337")

        // The new entries are in history, and the money adds up exactly.
        click("menu-history")
        shown("S-091")
        for (title in listOf("کارت به کارت", "پرداخت قبض", "خرید شارژ", "درخواست تسهیلات")) {
            val row = MockBank.transactions.first { it.title == title }
            compose.onNodeWithTag("history-scroll").performScrollToNode(hasTestTag("history-${row.key}"))
            compose.onNodeWithTag("history-${row.key}").assertExists()
        }
        assertEquals(10_350_000L, MockBank.balance("account-1"))
    }
}
