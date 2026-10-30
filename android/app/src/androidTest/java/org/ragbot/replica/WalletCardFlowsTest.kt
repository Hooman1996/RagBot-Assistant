package org.ragbot.replica

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletCardFlowsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun openWallet() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("card-kind-WALLET").performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun walletGroupShowsItsCardAndSeparateBalance() {
        openWallet()
        compose.onNodeWithTag("card-card-8").assertIsDisplayed()
        compose.onNodeWithContentDescription("کارت پیش‌فرض").assertDoesNotExist()
        compose
            .onNode(hasText("مشاهده موجودی") and hasAnyAncestor(hasTestTag("card-card-8")))
            .performClick()
        compose.onNodeWithText("۲٬۴۰۰٬۰۰۰ ریال").assertIsDisplayed()
    }

    @Test
    fun issueGoesThroughAddressToALabelledResultAndBackKeepsTheDraft() {
        openWallet()
        compose.onNodeWithTag("card-action-wallet-issue").performScrollTo().performClick()
        compose.onNodeWithTag("S-140").assertIsDisplayed()
        compose.onNodeWithTag("wallet-fee").assertTextContains("۱۲۰٬۰۰۰ ریال", substring = true)
        compose.onNodeWithTag("wallet-post").assertIsSelected()
        compose.onNodeWithTag("wallet-branch").assertIsNotSelected()
        compose.onNodeWithTag("wallet-type-ok").performScrollTo().performClick()
        compose.onNodeWithTag("S-137").assertIsDisplayed()
        compose.onNodeWithTag("wallet-address-ok").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("wallet-address-address-1").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("wallet-address-address-1").assertIsSelected()
        compose.onNodeWithTag("wallet-address-ok").performScrollTo().performClick()
        compose.onNodeWithTag("S-139").assertIsDisplayed()
        compose.onNodeWithText("۰۰۰۰۰۰۰۰۰۸").assertIsDisplayed()
        back()
        compose.onNodeWithTag("wallet-address-address-1").assertIsSelected()
        compose.onNodeWithTag("wallet-address-ok").performScrollTo().performClick()
        compose.onNodeWithTag("wallet-submit").performScrollTo().performClick()
        compose.onNodeWithTag("security-result").assertTextContains("هیچ تغییری", substring = true)
        compose.onNodeWithTag("security-done").performClick()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("card-kind-WALLET").assertIsSelected()
    }

    @Test
    fun cancelLeavesTheIssueFlowAndWalletPinUsesOtp() {
        openWallet()
        compose.onNodeWithTag("card-action-wallet-issue").performScrollTo().performClick()
        compose.onNodeWithTag("wallet-type-ok").performScrollTo().performClick()
        compose.onNodeWithTag("request-cancel").performScrollTo().performClick()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("card-action-wallet-pin").performScrollTo().performClick()
        compose.onNodeWithTag("S-141").assertIsDisplayed()
        compose.onNodeWithTag("pick-card-8").assertIsSelected()
        compose.onNodeWithTag("pin-ISSUE_FIRST").performClick()
        compose.onNodeWithTag("secret-new").performTextReplacement("1234")
        compose.onNodeWithTag("secret-retype").performTextReplacement("1234")
        compose.onNodeWithTag("pin-ok").performScrollTo().performClick()
        compose.onNodeWithTag("pin-confirm").performClick()
        compose.onNodeWithTag("otp-code").performTextReplacement("12345")
        compose.onNodeWithTag("otp-ok").performClick()
        compose.onNodeWithTag("security-result").assertIsDisplayed()
    }
}
