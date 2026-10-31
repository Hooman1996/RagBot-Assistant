package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoanGroupsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun groupEligibilityExplainsTheGateWithoutOpeningAnApplication() {
        compose.onNodeWithTag("demo-S-307").performScrollTo().performClick()
        compose.onNodeWithTag("loan-group-inactive").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("loan-group-recent").performScrollTo().performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("مدت لازم", substring = true)
        compose.onNodeWithTag("S-309").assertDoesNotExist()
        back()
        compose.onNodeWithTag("loan-group-credit").performScrollTo().performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("استعلام", substring = true)
        compose.onNodeWithTag("S-309").assertDoesNotExist()
    }

    @Test
    fun catalogueDetailSurvivesRecreationAndCannotOpenCalculation() {
        compose.onNodeWithTag("demo-S-307").performScrollTo().performClick()
        compose.onNodeWithTag("loan-group-standard").performScrollTo().performClick()
        compose.onNodeWithTag("S-309").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("loan-group-detail-title").assertTextEquals("گروه تسهیلات نمونه")
        compose
            .onNodeWithTag("loan-option-warning-limited")
            .performScrollTo()
            .assertTextContains("۶ ماه", substring = true)
        compose.onNodeWithTag("loan-option-open-inactive").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("loan-option-open-normal").performScrollTo().performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("محاسبه", substring = true)
        compose.onNodeWithTag("S-308").assertDoesNotExist()
        back()
        back()
        compose.onNodeWithTag("S-307").assertIsDisplayed()
        compose.onNodeWithTag("loan-group-standard").performScrollTo().assertIsDisplayed()
    }
}
