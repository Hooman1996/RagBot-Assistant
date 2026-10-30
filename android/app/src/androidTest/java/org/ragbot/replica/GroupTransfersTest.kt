package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupTransfersTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun closeKeyboard() {
        compose.runOnIdle {
            WindowCompat.getInsetsController(
                    compose.activity.window,
                    compose.activity.window.decorView,
                )
                .hide(WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
    }

    private fun click(tag: String) {
        closeKeyboard()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun dialog(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
    }

    private fun shown(id: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag(id).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(id).assertIsDisplayed()
    }

    private fun startGroup(total: String, byFile: Boolean) {
        compose.onNodeWithTag("demo-S-193").performScrollTo().performClick()
        click("transfer-group")
        shown("S-187")
        compose.onNodeWithTag("group-total").performTextInput(total)
        if (byFile) click("group-mode-file")
        click("group-continue")
    }

    private fun input(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(text)
    }

    @Test
    fun manualRowsValidateTotalsAndSurviveCancelWithoutSubmission() {
        compose.onNodeWithTag("demo-S-193").performScrollTo().performClick()
        click("transfer-group")
        click("group-continue")
        compose.onNodeWithTag("group-error").assertIsDisplayed()
        click("group-source")
        dialog("group-source-account-2")
        compose.onNodeWithTag("group-total").performTextInput("300000")
        click("group-extra")
        input("group-withdraw-id", "1234")
        click("group-continue")
        shown("S-184")
        input("group-row-0-iban", "920000000000000000000002")
        input("group-row-0-amount", "150000")
        click("group-row-0-reason")
        dialog("group-row-0-reason-0")
        click("group-accept")
        compose
            .onNodeWithTag("group-accept-message")
            .assertTextContains(GroupTransferRules.SINGLE_WARNING)
        dialog("group-accept-ok")
        click("group-add")
        input("group-row-1-iban", "930000000000000000000002")
        input("group-row-1-amount", "100000")
        click("group-row-1-reason")
        dialog("group-row-1-reason-1")
        click("group-accept")
        compose.onNodeWithTag("group-accept-message").assertIsDisplayed()
        dialog("group-accept-ok")
        compose.onNodeWithTag("group-row-1-error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("group-row-1-iban").performTextReplacement("650000000000000000000003")
        compose.activityRule.scenario.recreate()
        compose
            .onNodeWithTag("group-row-1-amount")
            .performScrollTo()
            .assertTextContains("100000", substring = true)
        click("group-accept")
        compose.onNodeWithTag("group-conflict").assertTextContains("۲۵۰٬۰۰۰", substring = true)
        dialog("group-conflict-accept")
        shown("group-waiting-first")
        compose.activityRule.scenario.recreate()
        shown("S-190")
        back()
        dialog("group-cancel-continue")
        click("group-refresh")
        shown("S-188")
        click("group-validation-continue")
        shown("group-waiting-second")
        click("group-refresh")
        shown("S-189")
        compose.onNodeWithText("۲۵۰٬۰۰۰").assertExists()
        compose.onNodeWithText("1234").assertExists()
        click("group-verify-accept")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.onNodeWithTag("S-096").assertDoesNotExist()
        back()
        back()
        shown("S-184")
        compose
            .onNodeWithTag("group-row-0-iban")
            .performScrollTo()
            .assertTextContains("920000000000000000000002", substring = true)
        click("group-accept")
        shown("S-190")
        click("group-cancel")
        dialog("group-cancel-confirm")
        shown("S-187")
        compose.onNodeWithTag("group-total").assertTextContains("250000", substring = true)
        compose
            .onNodeWithTag("group-source")
            .assertTextContains(AccountFixture.accounts[1].number, substring = true)
        click("group-continue")
        shown("S-184")
        compose.onNodeWithTag("group-row-1").assertDoesNotExist()
    }

    @Test
    fun fileFixturesReportLimitsAndInvalidRowsCanBeCorrectedOrRemoved() {
        startGroup("450000", byFile = true)
        shown("S-186")
        compose.onNodeWithTag("group-file-apply").assertIsNotEnabled()
        for (key in listOf("empty", "large")) {
            click("group-file-pick")
            dialog("group-file-$key")
            compose.onNodeWithTag("group-file-apply").assertIsNotEnabled()
        }
        click("group-file-example")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        click("group-file-pick")
        dialog("group-file-invalid")
        click("group-file-apply")
        shown("S-182")
        click("group-only-invalid")
        compose.onNodeWithTag("group-file-row-1").assertDoesNotExist()
        compose.onNodeWithTag("group-file-row-4").assertExists()
        click("group-file-row-4-edit")
        compose.onNodeWithTag("group-edit-amount").performTextReplacement("50000")
        dialog("group-edit-apply")
        compose.onNodeWithTag("group-file-row-4").assertDoesNotExist()
        click("group-only-invalid")
        click("group-accept")
        compose.onNodeWithTag("group-accept-message").assertIsDisplayed()
        dialog("group-accept-ok")
        click("group-remove-invalid")
        dialog("group-remove-confirm")
        compose.onNodeWithTag("group-file-row-2").assertDoesNotExist()
        compose.onNodeWithTag("group-file-row-5").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("group-file-row-4").performScrollTo().assertIsDisplayed()
        click("group-accept")
        compose.onNodeWithTag("group-conflict").assertTextContains("۵۰۰٬۰۰۰", substring = true)
        dialog("group-conflict-accept")
        shown("group-waiting-first")
    }

    @Test
    fun rejectedRowsRouteThroughErrorsBeforeTheLocalVerification() {
        startGroup("500000", byFile = true)
        click("group-file-pick")
        dialog("group-file-rejected")
        click("group-file-apply")
        click("group-accept")
        shown("S-190")
        click("group-refresh")
        shown("S-188")
        click("group-validation-continue")
        shown("S-185")
        compose.onNodeWithTag("group-rejected-2").assertIsDisplayed()
        back()
        dialog("group-cancel-continue")
        shown("S-185")
        click("group-remove-rejected")
        shown("group-waiting-second")
        click("group-refresh")
        shown("S-189")
        compose.onNodeWithText("۲۵۰٬۰۰۰").assertExists()
        click("group-verify-details")
        compose.onNodeWithText("IR650000000000000000000003", substring = true).assertDoesNotExist()
    }

    @Test
    fun historyDestinationsAndReceiptAreLabelledLocalSamples() {
        compose.onNodeWithTag("demo-S-089").performScrollTo().performClick()
        shown("S-089")
        compose.onNodeWithTag("group-history-search").performTextInput("999")
        compose.onNodeWithTag("group-history-empty").assertIsDisplayed()
        compose.onNodeWithTag("group-history-search").performTextReplacement("۵۰۰")
        click("group-history-DEMO-GROUP-0001-attachment")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        click("group-history-DEMO-GROUP-0001-destinations")
        shown("S-087")
        compose.onNodeWithTag("group-destination-search").performTextInput("0003")
        compose.onNodeWithTag("group-destination-1").assertDoesNotExist()
        click("group-destination-2")
        shown("S-088")
        compose.onNodeWithText("DEMO-REF-2").assertExists()
        click("group-destination-back")
        shown("S-087")
        compose.activityRule.scenario.recreate()
        compose
            .onNodeWithTag("group-destination-search")
            .assertTextContains("0003", substring = true)
        back()
        back()
        back()
        compose.onNodeWithTag("demo-S-096").performScrollTo().performClick()
        compose.onNodeWithTag("group-receipt-disclosure").assertExists()
        click("group-receipt-share")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
    }

    @Test
    fun realKeyboardLeavesTheFirstPageContinueReachable() {
        compose.onNodeWithTag("demo-S-193").performScrollTo().performClick()
        click("transfer-group")
        compose.onNodeWithTag("group-total").performClick().performTextInput("250000")
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("group-continue").performScrollTo().assertIsDisplayed()
        val metrics = compose.activity.resources.displayMetrics
        val keyboardHeight =
            checkNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView))
                .getInsets(WindowInsetsCompat.Type.ime())
                .bottom
        val keyboardTop = (metrics.heightPixels - keyboardHeight) / metrics.density
        org.junit.Assert.assertTrue(
            compose.onNodeWithTag("group-continue").getUnclippedBoundsInRoot().bottom.value <=
                keyboardTop
        )
    }
}
