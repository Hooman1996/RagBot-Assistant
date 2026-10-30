package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardRequestsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun startRequest(key: String) {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("card-action-$key").performScrollTo().performClick()
    }

    @Test
    fun issueInPersonSkipsAddressAndShowsALabelledLocalResult() {
        startRequest("issue")
        compose.onNodeWithTag("S-124").assertIsDisplayed()
        compose.onNodeWithTag("source-account-1").assertIsSelected()
        compose.onNodeWithTag("delivery-branch").assertIsSelected()
        compose.onNodeWithTag("source-account-2").performClick()
        compose.onNodeWithTag("first-ok").performScrollTo().performClick()
        compose.onNodeWithTag("S-123").assertIsDisplayed()
        compose.onNodeWithTag("color-ok").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("color-color-2").performClick()
        compose.onNodeWithTag("color-ok").performScrollTo().performClick()
        compose.onNodeWithTag("S-128").assertIsDisplayed()
        compose.onNodeWithText("۰۰۰۰۰۰۰۰۰۲").assertIsDisplayed()
        compose.onNodeWithText("۲۵۰٬۰۰۰ ریال").assertIsDisplayed()
        compose.onNodeWithText("سرمه‌ای").assertIsDisplayed()
        compose.onNodeWithText("آدرس:").assertDoesNotExist()
        compose.onNodeWithTag("request-submit").performScrollTo().performClick()
        compose.onNodeWithTag("request-result").assertTextContains("هیچ کارتی", substring = true)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("request-result").assertIsDisplayed()
        compose.onNodeWithTag("request-done").performClick()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("control-CARD").assertIsSelected()
    }

    @Test
    fun postalModifyNeedsAnAddressAndBackRestoresTheDraft() {
        startRequest("recolor")
        compose.onNodeWithTag("S-127").assertIsDisplayed()
        compose.onNodeWithTag("source-card-1").assertIsSelected()
        compose.onNodeWithTag("source-card-2").performClick()
        compose.onNodeWithTag("delivery-post").performClick()
        compose.onNodeWithTag("first-ok").performScrollTo().performClick()
        compose.onNodeWithTag("S-129").assertIsDisplayed()
        compose.onNodeWithTag("address-ok").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("add-address").performClick()
        compose.onNodeWithTag("S-463").assertIsDisplayed()
        back()
        compose.onNodeWithTag("address-address-2").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("address-address-2").assertIsSelected()
        compose.onNodeWithTag("address-ok").performScrollTo().performClick()
        compose.onNodeWithTag("color-color-1").performClick()
        compose.onNodeWithTag("color-ok").performScrollTo().performClick()
        compose.onNodeWithText("۰۰۰۰ ۰۰۰۰ ۰۰۰۰ ۰۰۰۲").assertIsDisplayed()
        compose.onNodeWithText("تغییر رنگ کارت").assertExists()
        compose.onNodeWithText("شماره حساب:").assertDoesNotExist()
        compose.onNodeWithText("شعبه:").assertDoesNotExist()
        back()
        back()
        compose.onNodeWithTag("S-129").assertIsDisplayed()
        compose.onNodeWithTag("address-address-2").assertIsSelected()
        back()
        compose.onNodeWithTag("delivery-post").assertIsSelected()
        compose.onNodeWithTag("source-card-2").assertIsSelected()
    }

    @Test
    fun cancelLeavesTheWholeFlowWithoutAResult() {
        startRequest("extend")
        compose.onNodeWithTag("first-ok").performScrollTo().performClick()
        compose.onNodeWithTag("color-color-3").performClick()
        compose.onNodeWithTag("color-ok").performScrollTo().performClick()
        compose.onNodeWithTag("request-cancel").performScrollTo().performClick()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("request-result").assertDoesNotExist()
        compose.onNodeWithTag("card-action-extend").performScrollTo().performClick()
        compose.onNodeWithTag("S-127").assertIsDisplayed()
        compose.onNodeWithTag("first-ok").performScrollTo().performClick()
        compose.onNodeWithTag("color-ok").performScrollTo().assertIsNotEnabled()
    }
}
