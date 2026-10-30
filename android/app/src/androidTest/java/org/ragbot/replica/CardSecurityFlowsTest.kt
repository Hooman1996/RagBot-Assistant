package org.ragbot.replica

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardSecurityFlowsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun open(key: String, kind: String = "DEBIT") {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("card-kind-$kind").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("card-action-$key").performScrollTo().performClick()
    }

    @Test
    fun blockUsesTheTabGroupWarnsAndEndsInALabelledResult() {
        open("block", "BON")
        compose.onNodeWithTag("S-121").assertIsDisplayed()
        compose.onNodeWithTag("block-card-1").assertDoesNotExist()
        compose.onNodeWithTag("block-card-6").performClick()
        compose.onNodeWithText("آیا مایل به مسدود کردن کارت هستید؟").assertIsDisplayed()
        compose.onNodeWithText("انصراف").performClick()
        compose.onNodeWithTag("security-result").assertDoesNotExist()
        compose.onNodeWithTag("block-card-6").performClick()
        compose.onNodeWithTag("block-ok").performClick()
        compose.onNodeWithTag("security-result").assertTextContains("هیچ تغییری", substring = true)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("security-done").performClick()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("card-kind-BON").assertIsSelected()
    }

    @Test
    fun pinValidationWrongAndCancelledOtpThenResult() {
        open("pin")
        compose.onNodeWithTag("S-135").assertIsDisplayed()
        compose.onNodeWithTag("pin-CHANGE_SECOND").performClick()
        compose.onNodeWithTag("secret-current").performTextReplacement("11111")
        compose.onNodeWithTag("secret-new").performTextReplacement("1234")
        compose.onNodeWithTag("pin-ok").performScrollTo().performClick()
        // Rule hint plus the field error.
        compose.onAllNodesWithText("طول رمز باید بین ۵ تا ۱۲ رقم باشد.").assertCountEquals(2)
        compose.onNodeWithTag("secret-new").performTextReplacement("123456")
        compose.onNodeWithTag("secret-retype").performTextReplacement("123456")
        compose.onNodeWithTag("pin-ok").performScrollTo().performClick()
        compose.onNodeWithTag("pin-confirm").performClick()
        compose.onNodeWithTag("otp-code").performTextReplacement("00000")
        compose.onNodeWithTag("otp-ok").performClick()
        compose.onNodeWithText("کد وارد شده صحیح نیست.").assertIsDisplayed()
        compose.onNodeWithTag("otp-cancel").performClick()
        compose.onNodeWithTag("security-result").assertDoesNotExist()
        compose.onNodeWithTag("pin-ok").performScrollTo().performClick()
        compose.onNodeWithTag("pin-confirm").performClick()
        compose.onNodeWithTag("otp-code").performTextReplacement("12345")
        compose.onNodeWithTag("otp-ok").performClick()
        compose.onNodeWithTag("security-result").assertIsDisplayed()
        compose.onNodeWithTag("security-done").performClick()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
    }

    @Test
    fun secretsAreNotRestoredAfterRecreation() {
        open("pin")
        compose.onNodeWithTag("pick-card-2").performClick()
        compose.onNodeWithTag("pin-ISSUE_FIRST").performClick()
        compose.onNodeWithTag("secret-new").performTextReplacement("1234")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("pick-card-2").assertIsSelected()
        compose.onNodeWithTag("pin-ok").performScrollTo().performClick()
        // Both the new PIN and its retype are empty again.
        compose.onAllNodesWithText("تکمیل این فیلد اجباری است.").assertCountEquals(2)
    }

    @Test
    fun activationAndPreLoginBlockEndInLabelledResults() {
        open("activation")
        compose.onNodeWithTag("S-136").assertIsDisplayed()
        compose.onNodeWithTag("activation-send").performClick()
        compose.onNodeWithTag("otp-code").performTextReplacement("۱۲۳۴۵")
        compose.onNodeWithTag("otp-ok").performClick()
        compose.onNodeWithTag("security-result").assertTextContains("رمز پویا", substring = true)
        compose.onNodeWithTag("security-done").performClick()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("demo-S-122").performClick()
        compose.onNodeWithTag("S-122").assertIsDisplayed()
        compose.onNodeWithTag("noLogin-dynamic").performScrollTo().performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("noLogin-ok").performScrollTo().performClick()
        compose.onNodeWithText("شماره کارت باید ۱۶ رقم باشد.").assertExists()
        compose.onNodeWithTag("noLogin-number").performTextReplacement("0000000000000009")
        compose.onNodeWithTag("secret-cvv2").performTextReplacement("1234")
        compose.onNodeWithTag("noLogin-expiry").performTextReplacement("05/08")
        compose.onNodeWithTag("secret-password").performTextReplacement("12345")
        compose.onNodeWithTag("noLogin-ok").performScrollTo().performClick()
        compose.onNodeWithTag("security-result").assertTextContains("۰۰۰۹", substring = true)
    }
}
