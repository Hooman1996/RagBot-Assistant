package org.ragbot.replica

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedDestinationsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun homeSearchOpensShortcutContentAndRetainsOriginAndLocalPins() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("header-SEARCH").performClick()
        compose.onNodeWithTag("S-084").assertIsDisplayed()
        compose.onNodeWithTag("shortcut-search").performTextInput("کارت")
        compose.onNodeWithTag("shortcut-pin-preview-cards").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("shortcut-search").assertTextContains("کارت", substring = true)
        compose.onNodeWithTag("shortcut-item-preview-cards").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        compose.onNodeWithTag("S-084").assertIsDisplayed()
        compose.onNodeWithTag("shortcut-search").performTextClearance()
        compose.onNodeWithTag("shortcut-item-preview-cards").assertIsDisplayed()
        back()
        compose.onNodeWithTag("control-CARD").assertIsSelected()
        compose.onNodeWithTag("header-SHORTCUTS").performClick()
        compose.onNodeWithTag("S-200").assertIsDisplayed()
        back()
        compose.onNodeWithTag("header-SEARCH").performClick()
        compose.onNodeWithTag("shortcut-item-preview-cards").assertIsDisplayed()
        compose.onNodeWithTag("shortcut-pin-preview-cards").performClick()
        compose.onNodeWithTag("shortcut-item-preview-cards").assertDoesNotExist()
    }

    @Test
    fun shortcutEmptyLoadingErrorAndRetryStayLocal() {
        compose.onNodeWithTag("scenario-empty").performClick()
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("header-SEARCH").performClick()
        compose.onNodeWithTag("shortcut-empty").assertIsDisplayed()
        back()
        back()
        compose.onNodeWithTag("scenario-loading").performClick()
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("header-SEARCH").performClick()
        compose.onNodeWithTag("data-loading").assertIsDisplayed()
        back()
        back()
        compose.onNodeWithTag("scenario-error").performClick()
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("header-SEARCH").performClick()
        compose.onNodeWithTag("data-error").assertIsDisplayed()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.onNodeWithTag("shortcut-list").assertIsDisplayed()
        compose.onNodeWithTag("shortcut-search").performTextInput("missing-service")
        compose.onNodeWithTag("shortcut-empty").assertIsDisplayed()
    }

    private fun review(id: String) {
        compose.onNodeWithTag("demo-$id").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(5_000) { compose.onNodeWithTag(id).isDisplayed() }
        compose.onNodeWithTag(id).assertIsDisplayed()
    }

    @Test
    fun pickerReturnsIdentityAndCancelPreservesReviewDraftAndResult() {
        val draft = "یادداشت فرم DEMO"
        compose.onNodeWithTag("picker-review-draft").performScrollTo().performTextInput(draft)
        review("S-198")
        compose.onNodeWithTag("saved-search").performTextInput("DEMO-LOAN-2")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("saved-search").assertTextContains("DEMO-LOAN-2", substring = true)
        compose.onNodeWithTag("saved-item-loan-1").assertDoesNotExist()
        compose.onNodeWithTag("saved-item-loan-2").performClick()
        compose
            .onNodeWithTag("picker-review-result")
            .performScrollTo()
            .assertTextContains("loan-2", substring = true)
        compose.onNodeWithTag("picker-review-draft").assertTextContains(draft, substring = true)
        review("S-198")
        back()
        compose
            .onNodeWithTag("picker-review-result")
            .performScrollTo()
            .assertTextContains("loan-2", substring = true)
        compose.activityRule.scenario.recreate()
        compose
            .onNodeWithTag("picker-review-draft")
            .performScrollTo()
            .assertTextContains(draft, substring = true)
        compose.onNodeWithTag("S-199").assertDoesNotExist()
    }

    @Test
    fun legacyEditValidationDraftAndDeleteCancellationSurviveRecreation() {
        review("S-066")
        reveal("account-1")
        compose.onNodeWithTag("saved-edit-account-1").performClick()
        compose.onNodeWithTag("saved-name").performTextClearance()
        compose.onNodeWithTag("saved-save").performScrollTo().performClick()
        compose.onNodeWithTag("saved-validation").assertIsDisplayed()
        compose.onNodeWithTag("saved-name").performTextReplacement("نام محلی DEMO")
        compose.onNodeWithTag("saved-description").performTextReplacement("توضیح محلی")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("saved-name").assertTextContains("نام محلی DEMO", substring = true)
        compose.onNodeWithTag("saved-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose
            .onNode(hasText("نام محلی DEMO") and hasAnyAncestor(hasTestTag("saved-item-account-1")))
            .assertIsDisplayed()
        reveal("account-1")
        compose.onNodeWithTag("saved-delete-account-1").performClick()
        compose.onNodeWithTag("saved-delete-cancel").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("saved-item-account-1").assertIsDisplayed()
        reveal("account-1")
        compose.onNodeWithTag("saved-delete-account-1").performClick()
        compose.onNodeWithTag("saved-delete-confirm").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("saved-item-account-1").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("saved-item-account-1").assertDoesNotExist()
        compose.onNodeWithTag("saved-type-CARD").performClick()
        compose.onNodeWithTag("saved-item-card-1").assertIsDisplayed()
        compose.onNodeWithTag("saved-item-account-2").assertDoesNotExist()
    }

    @Test
    fun legacyAddRejectsDuplicateThenKeepsOnlyTheLocalRecord() {
        review("S-066")
        compose.onNodeWithTag("saved-add").performClick()
        compose.onNodeWithTag("saved-name").performTextInput("حساب جدید نمونه")
        compose.onNodeWithTag("saved-number").performTextInput("0000000000001")
        compose.onNodeWithTag("saved-save").performScrollTo().performClick()
        compose.onNodeWithTag("saved-validation").assertIsDisplayed()
        compose.onNodeWithTag("saved-number").performTextReplacement("123")
        compose.onNodeWithTag("saved-save").performScrollTo().performClick()
        compose.onNodeWithTag("saved-validation").assertIsDisplayed()
        compose.onNodeWithTag("saved-number").performTextReplacement("0000000000099")
        compose.onNodeWithTag("saved-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("saved-search").performTextInput("حساب جدید نمونه")
        compose.onNodeWithText("0000000000099").assertIsDisplayed()
        back()
        back()
        review("S-066")
        compose.onNodeWithTag("saved-search").performTextInput("حساب جدید نمونه")
        compose.onNodeWithText("0000000000099").assertIsDisplayed()
    }

    @Test
    fun loanManagementSearchDeleteAndUnavailableEditStayOnTheSameScreen() {
        review("S-197")
        compose.onNodeWithTag("saved-search").performTextInput("DEMO-LOAN-3")
        reveal("loan-3")
        compose.onNodeWithTag("saved-edit-loan-3").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        reveal("loan-3")
        compose.onNodeWithTag("saved-delete-loan-3").performClick()
        compose.onNodeWithTag("saved-delete-cancel").assertIsDisplayed()
        compose.waitUntil(5_000) { !compose.activity.hasWindowFocus() }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("saved-item-loan-3").assertIsDisplayed()
        reveal("loan-3")
        compose.onNodeWithTag("saved-delete-loan-3").performClick()
        compose.onNodeWithTag("saved-delete-confirm").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
        compose.onNodeWithTag("saved-search").assertTextContains("DEMO-LOAN-3", substring = true)
        compose.onNodeWithTag("saved-search").performTextClearance()
        compose.onNodeWithTag("saved-item-loan-1").assertIsDisplayed()
        compose.onNodeWithTag("saved-item-loan-3").assertDoesNotExist()
    }

    @Test
    fun savedDestinationEmptyLoadingErrorRetryAreIndependentReviewStates() {
        for ((scenario, tag) in
            listOf("empty" to "data-empty", "loading" to "data-loading", "error" to "data-error")) {
            compose.onNodeWithTag("scenario-$scenario").performScrollTo().performClick()
            review("S-198")
            compose.onNodeWithTag(tag).assertIsDisplayed()
            if (scenario == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("saved-item-loan-1").assertIsDisplayed()
            }
            back()
        }
    }

    private fun action(id: String, label: String) {
        val action =
            compose
                .onNodeWithTag("saved-item-$id")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first { it.label == label }
        compose.runOnIdle { assertTrue(action.action()) }
    }

    private fun reveal(id: String) = action(id, "نمایش گزینه‌ها")

    @Test
    fun legacyOrderCancelApplyAndRecreationPreserveTheCorrectList() {
        review("S-066")
        compose.onNodeWithTag("saved-edit-order").performClick()
        action("account-1", "انتقال به پایین")
        compose.activityRule.scenario.recreate()
        fun assertOrder(first: String, second: String) {
            compose.onNodeWithTag("saved-list").performScrollToIndex(0)
            assertTrue(
                compose.onNodeWithTag("saved-item-$first").getUnclippedBoundsInRoot().top <
                    compose.onNodeWithTag("saved-item-$second").getUnclippedBoundsInRoot().top
            )
        }
        assertOrder("account-2", "account-1")
        compose.onNodeWithTag("saved-order-cancel").performClick()
        assertOrder("account-1", "account-2")
        compose.onNodeWithTag("saved-edit-order").performClick()
        compose.onNodeWithTag("saved-item-account-1").performScrollTo().performTouchInput {
            down(center)
            advanceEventTime(700)
            val start = center
            for (step in 1..12) {
                moveTo(start.copy(y = start.y + height * 0.8f * step / 12), delayMillis = 50)
            }
            up()
        }
        assertOrder("account-2", "account-1")
        compose.onNodeWithTag("saved-order-apply").performClick()
        compose.activityRule.scenario.recreate()
        assertOrder("account-2", "account-1")
        back()
        back()
        review("S-066")
        assertOrder("account-2", "account-1")
    }

    @Test
    fun editorScrollsLocalSaveAndCancelAboveTheRealKeyboard() {
        review("S-066")
        compose.onNodeWithTag("saved-add").performClick()
        compose.onNodeWithTag("saved-name").performClick().performTextInput("draft")
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        fun assertAboveKeyboard(tag: String) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            val metrics = compose.activity.resources.displayMetrics
            val keyboardHeight =
                checkNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView))
                    .getInsets(WindowInsetsCompat.Type.ime())
                    .bottom
            val keyboardTop = (metrics.heightPixels - keyboardHeight) / metrics.density
            val bottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom.value
            assertTrue("$tag bottom=$bottom must clear IME top=$keyboardTop", bottom <= keyboardTop)
        }
        assertAboveKeyboard("saved-save")
        assertAboveKeyboard("saved-editor-cancel")
        compose.onNodeWithTag("saved-editor-cancel").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("S-066").assertIsDisplayed()
    }
}
