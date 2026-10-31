package org.ragbot.replica

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecurringTransfersTest {
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

    private fun openMenu() {
        compose.onNodeWithTag("demo-S-193").performScrollTo().performClick()
        click("transfer-continuous")
        shown("S-180")
    }

    @Test
    fun scheduleValidationCorrectionAndContinuousFormNeverSubmit() {
        openMenu()
        click("recurring-new")
        shown("S-181")
        click("schedule-continue")
        compose.onNodeWithTag("schedule-error").assertIsDisplayed()
        compose.onNodeWithTag("schedule-title").performTextInput("اجارهٔ نمونه")
        compose.onNodeWithTag("schedule-start").performTextInput("1405/09/01")
        compose.onNodeWithTag("schedule-end").performTextInput("1404/09/01")
        click("schedule-frequency-MONTHLY")
        compose.onNodeWithTag("schedule-day-31").performScrollTo()
        dialog("schedule-day-31")
        compose.onNodeWithTag("schedule-month-warning").performScrollTo().assertIsDisplayed()
        click("schedule-continue")
        compose.onNodeWithTag("schedule-error").assertTextContains("قبل از", substring = true)
        compose.onNodeWithTag("schedule-end").performTextReplacement("1406/09/02")
        click("schedule-continue")
        compose.onNodeWithTag("schedule-error").assertTextContains("یک سال", substring = true)
        compose.onNodeWithTag("schedule-end").performTextReplacement("1405/12/01")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("schedule-title").assertTextContains("اجارهٔ نمونه")
        click("schedule-continue")
        dialog("schedule-type-S-192")
        shown("S-177")
        compose.onNodeWithTag("schedule-execution-unknown").assertExists()
        click("schedule-execution-continue")
        shown("S-192")
        compose.onNodeWithTag("account-transfer-schedule").assertExists()
        compose.onNodeWithTag("account-transfer-destination").performTextInput("0000000000002")
        compose.onNodeWithTag("account-transfer-amount").performTextInput("1500000")
        click("account-transfer-reason")
        dialog("account-transfer-reason-0")
        click("account-transfer-continue")
        shown("S-173")
        compose.onNodeWithTag("account-transfer-schedule").assertExists()
        click("account-transfer-submit")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        back()
        back()
        shown("S-177")
        back()
        shown("S-181")
        compose.onNodeWithTag("schedule-end").assertTextContains("1405/12/01")
        back()
        back()
        shown("S-193")
        click("transfer-internal")
        compose.onNodeWithTag("account-transfer-schedule").assertDoesNotExist()
        compose
            .onNodeWithTag("account-transfer-destination")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
            )
    }

    @Test
    fun historyDetailsCancelOnlyWaitingRunsAndDeleteBeforeStart() {
        openMenu()
        click("recurring-history")
        shown("S-178")
        compose.onNodeWithTag("continuous-search").performTextInput("هفتگی")
        compose.onNodeWithTag("continuous-DEMO-STANDING-0001").assertDoesNotExist()
        compose.onNodeWithTag("continuous-search").performTextClearance()
        click("continuous-DEMO-STANDING-0001")
        shown("S-179")
        click("continuous-execution-0-receipt")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        click("continuous-action-CANCEL")
        dialog("continuous-confirm")
        compose.onNodeWithTag("continuous-action-BACK").assertExists()
        compose
            .onNode(hasAnyAncestor(hasTestTag("continuous-execution-0")) and hasText("موفق"))
            .assertExists()
        compose
            .onNode(hasAnyAncestor(hasTestTag("continuous-execution-3")) and hasText("لغو شده"))
            .assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("continuous-action-BACK").performScrollTo().assertIsDisplayed()
        back()
        click("continuous-DEMO-STANDING-0002")
        click("continuous-action-DELETE")
        dialog("continuous-confirm")
        shown("S-178")
        compose.onNodeWithTag("continuous-DEMO-STANDING-0002").assertDoesNotExist()
        click("continuous-DEMO-STANDING-0003")
        click("continuous-action-BACK")
        shown("S-178")
        click("continuous-new")
        shown("S-181")
    }

    @Test
    fun homeBookmarkRepeatsIntoExistingReviewsWithoutPrevalidationOrSubmission() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("header-SHORTCUTS").performClick()
        shown("S-200")
        compose.onNodeWithTag("repetitive-search").performTextInput("پایا")
        compose.onNodeWithTag("repetitive-repeat-internal").assertDoesNotExist()
        compose.onNodeWithTag("repetitive-search").performTextClearance()
        click("repetitive-repeat-internal-repeat")
        shown("S-173")
        compose.onNodeWithText("0000000000002").assertExists()
        compose.onNodeWithTag("account-transfer-schedule").assertDoesNotExist()
        back()
        click("repetitive-repeat-card-repeat")
        shown("S-176")
        compose
            .onNodeWithTag("card-transfer-review-destination")
            .assertTextContains(CardTransferFixture.destination, substring = true)
        back()
        click("repetitive-repeat-charge-repeat")
        shown("S-212")
        compose.onNodeWithText("همراه اول · 09000000000").assertExists()
        back()
        click("repetitive-repeat-paya-delete")
        dialog("repetitive-delete-confirm")
        compose.onNodeWithTag("repetitive-repeat-paya").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("repetitive-repeat-paya").assertDoesNotExist()
        back()
        compose.onNodeWithTag("S-026").assertIsDisplayed()
    }

    @Test
    fun realKeyboardLeavesTheScheduleContinueReachable() {
        openMenu()
        click("recurring-new")
        compose.onNodeWithTag("schedule-end").performClick().performTextInput("1405")
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("schedule-continue").performScrollTo().assertIsDisplayed()
        val metrics = compose.activity.resources.displayMetrics
        val keyboardHeight =
            checkNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView))
                .getInsets(WindowInsetsCompat.Type.ime())
                .bottom
        val keyboardTop = (metrics.heightPixels - keyboardHeight) / metrics.density
        org.junit.Assert.assertTrue(
            compose.onNodeWithTag("schedule-continue").getUnclippedBoundsInRoot().bottom.value <=
                keyboardTop
        )
    }
}
