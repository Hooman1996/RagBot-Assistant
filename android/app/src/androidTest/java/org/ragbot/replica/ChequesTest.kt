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
class ChequesTest {
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

    private fun unavailable(title: String) {
        compose.onNodeWithTag("unavailable").assertTextContains(title, substring = true)
        back()
        compose.onNodeWithTag("unavailable").assertDoesNotExist()
    }

    private fun home(tile: String) {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-$tile").performScrollTo().performClick()
    }

    @Test
    fun homeChequeMenuOpensBooksWithAnEmptyAccountSummaryAndSearch() {
        home("cheque")
        shown("S-147")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_CHAKAD_CARTABLE_DIGITAL_CHEQUE")
        unavailable("کارتابل چک دیجیتال")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_REPORT")
        unavailable("گزارش چک‌های واگذار شده")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_CHEQUEBOOK")
        shown("S-142")
        click("cheque-account")
        dialog("cheque-account-account-3")
        compose.onNodeWithTag("cheque-empty").assertExists()
        click("cheque-account")
        dialog("cheque-account-account-1")
        click("cheque-book-DEMO-BOOK-01")
        shown("S-146")
        compose.onNodeWithTag("cheque-534004").assertExists()
        click("cheque-summary")
        compose.onNodeWithText("قابل استفاده").assertExists()
        compose.onNodeWithText("بستن").performClick()
        compose.onNodeWithTag("cheque-search").performTextReplacement("534002")
        compose.onNodeWithTag("cheque-534001").assertDoesNotExist()
        compose.onNodeWithTag("cheque-534002").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("cheque-534001").assertDoesNotExist()
        back()
        shown("S-142")
        compose.onNodeWithTag("cheque-book-DEMO-BOOK-01").assertExists()
    }

    @Test
    fun trackingRequiresANumberAndOnlyFindsTheSelectedAccountsCheque() {
        home("cheque")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_MY_CHEQUE_TRACE")
        shown("S-149")
        click("cheque-track")
        compose.onNodeWithText(PaymentRules.MANDATORY).assertExists()
        compose.onNodeWithTag("cheque-no").performTextReplacement("229902")
        click("cheque-track")
        compose.onNodeWithTag("cheque-not-found").assertExists()
        compose.onNodeWithText("بستن").performClick()
        compose.onNodeWithTag("cheque-no").performTextReplacement("534002")
        click("cheque-track")
        compose.onNodeWithText("برگشتی").assertExists()
        compose.onNodeWithTag("cheque-share").performClick()
        unavailable("اشتراک‌گذاری")
        compose.onNodeWithText("بازگشت").performClick()
        compose.onNodeWithTag("cheque-track-result").assertDoesNotExist()
        back()
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_FOLLOWUP")
        shown("S-150")
        compose.onNodeWithTag("cheque-track").assertExists()
    }

    @Test
    fun digitalChequebookRequestValidatesAndStopsBeforeTheOtp() {
        home("cheque")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_DIGITAL_CHECKBOOK")
        shown("S-143")
        compose.onNodeWithText("درخواست صدور دسته چک دیجیتال").assertExists()
        compose.onNodeWithTag("cheque-page-50").assertDoesNotExist()
        click("cheque-issue-continue")
        compose.onNodeWithTag("cheque-account-error").assertExists()
        compose.onNodeWithTag("cheque-page-error").assertExists()
        click("cheque-account")
        dialog("cheque-account-account-3")
        click("cheque-page-20")
        click("cheque-issue-continue")
        shown("S-144")
        compose.onNodeWithText("۱۵۰٬۰۰۰ ریال").assertExists()
        compose.onNodeWithTag("cheque-cardboard-note").assertExists()
        click("payment-submit")
        unavailable("تایید تراکنش با رمز پویا")
        compose.onNodeWithTag("otp-code").assertDoesNotExist()
        click("payment-cancel")
        shown("S-143")
        compose.onNodeWithTag("cheque-page-20").assertIsSelected()
        back()
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRACE_ISSUANCE_CHECKBOOK")
        shown("S-145")
        compose.onNodeWithText("دلیل رد نمایشی").assertExists()
    }

    @Test
    fun cardBoardTabsOpenTheirRecoveredDetailVariantsWithoutActing() {
        home("cartable")
        shown("S-154")
        click("cardboard-task-2")
        shown("S-155")
        compose.onNodeWithText("گیرندهٔ نمایشی").assertExists()
        click("cardboard-act")
        unavailable("تایید درخواست کارتابل")
        back()
        click("cardboard-task-1")
        shown("S-156")
        click("cardboard-reject")
        unavailable("رد درخواست کارتابل")
        back()
        click("cardboard-tab-TRACK")
        compose.onNodeWithTag("cheque-empty").assertExists()
        click("cardboard-tab-DONE")
        click("cardboard-task-3")
        shown("S-156")
        compose.onNodeWithTag("cardboard-act").assertDoesNotExist()
    }

    @Test
    fun reportEntryFiltersAndScenarioStatesStayLocal() {
        click("demo-S-153")
        shown("S-153")
        compose.onNodeWithTag("cheque-report-880001").assertExists()
        click("cheque-report-OTHER")
        compose.onNodeWithTag("cheque-report-880001").assertDoesNotExist()
        compose.onNodeWithTag("cheque-report-990001").assertExists()
        click("cheque-report-INTERNAL")
        compose.onNodeWithTag("cheque-report-search").performTextReplacement("9500000")
        compose.onNodeWithTag("cheque-report-880001").assertDoesNotExist()
        compose.onNodeWithTag("cheque-report-880002").assertExists()
        back()
        back()
        for (state in listOf("empty", "loading", "error")) {
            click("scenario-$state")
            click("demo-S-153")
            compose.onNodeWithTag("data-$state").assertIsDisplayed()
            if (state == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("cheque-report-880001").assertExists()
            } else {
                back()
                back()
            }
        }
    }
}
