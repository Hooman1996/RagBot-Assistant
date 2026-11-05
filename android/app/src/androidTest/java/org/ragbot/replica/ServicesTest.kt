package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The services built on the form engine, end to end against the mock bank. */
@RunWith(AndroidJUnit4::class)
class ServicesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun click(tag: String) {
        compose.waitForIdle()
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

    private fun fill(field: String, value: String) {
        compose.onNodeWithTag("svc-$field").performScrollTo().performTextReplacement(value)
    }

    private fun next() = click("svc-continue")

    private fun confirm() {
        click("svc-confirm")
        compose.onNodeWithTag("otp-code").performTextReplacement("12345")
        compose.onNodeWithTag("otp-ok").performClick()
    }

    private fun failure(key: String) {
        compose
            .onNodeWithTag("bank-failure")
            .assertTextContains(MockData.current.messages.getValue(key))
        compose.onNodeWithTag("bank-failure-close").performClick()
    }

    private fun chequeMenu(key: String) {
        click("demo-entry")
        click("menu-cheque")
        click("cheque-menu-$key")
    }

    @Test
    fun registeringASayadChequeEndsInAReceiptAndTheInquiryFindsIt() {
        chequeMenu("AUTHENTICATED_HOME_CHEQUE_PICHAK_ISSUE_PAPER_CHEQUE")
        shown("S-165")
        next()
        compose.onNodeWithText(PaymentRules.MANDATORY, substring = true).assertExists()
        fill("sayad", "0000000000000900")
        fill("amount", "7000000")
        fill("due", "۱۴۰۵/۱۰/۰۱")
        next()
        shown("S-171")
        fill("receiver", "رضا")
        fill("code", "0000000019")
        next()
        shown("S-170")
        confirm()
        shown("S-168")
        compose.onNodeWithTag("bank-receipt").assertExists()
        click("bank-receipt-done")
        click("menu-cheque")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_INQUIRY_BYHOLDER")
        shown("S-164")
        click("svc-kind-holder")
        next()
        fill("sayad", "0000000000000900")
        click("svc-confirm")
        shown("S-166")
        compose.onNodeWithText("ثبت شده").assertExists()
    }

    @Test
    fun anUnknownChequeFailsTheInquiry() {
        chequeMenu("AUTHENTICATED_HOME_CHEQUE_INQUIRY_BYHOLDER")
        click("svc-kind-issuer")
        next()
        fill("sayad", "0000000000000999")
        click("svc-confirm")
        failure("sayad-not-found")
        shown("S-162")
    }

    @Test
    fun aReceivedChequeIsConfirmedThenTransferredAndLeavesThePendingList() {
        chequeMenu("DESIGNED_SAYAD_ANSWER")
        shown("S-167")
        click("svc-sayad-0000000000000311")
        next()
        shown("S-163")
        click("svc-answer-accept")
        next()
        shown("S-170")
        confirm()
        shown("S-168")
        click("bank-receipt-done")
        click("menu-cheque")
        click("cheque-menu-DESIGNED_SAYAD_ANSWER")
        compose.onNodeWithTag("svc-sayad-0000000000000311").assertDoesNotExist()
        click("page-back")
        click("cheque-menu-DESIGNED_SAYAD_TRANSFER")
        shown("S-172")
        click("svc-sayad-0000000000000311")
        next()
        fill("receiver", "رضا")
        fill("code", "0000000019")
        next()
        confirm()
        shown("S-168")
    }

    @Test
    fun theChequeHistoryListsEveryCheque() {
        chequeMenu("DESIGNED_SAYAD_HISTORY")
        shown("S-169")
        compose.onNodeWithText("0000000000000321", substring = true).assertExists()
    }
}
