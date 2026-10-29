package org.ragbot.replica

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun enterAccounts() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-ACCOUNT").performClick()
    }

    @Test
    fun accountSelectionPersistsThroughBackRecreationAndHomeScrollReturn() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-branches").performScrollTo()
        val scroll =
            compose
                .onNodeWithTag("home-scroll")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        compose.onNodeWithTag("control-ACCOUNT").performClick()
        compose.onNodeWithTag("control-ACCOUNT").assertIsSelected()
        compose.onNodeWithTag("account-list").performClick()
        compose.onNodeWithTag("S-085").assertIsDisplayed()
        compose.onNodeWithTag("account-search").performTextReplacement("0000000002")
        compose.onNodeWithTag("choose-account-1").assertDoesNotExist()
        compose
            .onNodeWithTag("choose-account-2")
            .assertTextContains("۳٬۷۵۰٬۰۰۰ ریال", substring = true)
        compose
            .onNodeWithTag("choose-account-2")
            .assertTextContains(AccountFixture.accounts[1].iban, substring = true)
        compose.onNodeWithTag("choose-account-2").performClick()
        compose.onNodeWithText("۰۰۰۰۰۰۰۰۰۲", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("account-balance").performClick()
        compose.onNodeWithText("۳٬۷۵۰٬۰۰۰ ریال · عدم نمایش").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("۳٬۷۵۰٬۰۰۰ ریال · عدم نمایش").assertIsDisplayed()
        back()
        compose.onNodeWithTag("control-HOME").assertIsSelected()
        assertEquals(
            scroll,
            compose
                .onNodeWithTag("home-scroll")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value(),
            0f,
        )
        compose.onNodeWithTag("menu-branches").assertIsDisplayed()
        compose.onNodeWithTag("account-number").assertTextContains("۰۰۰۰۰۰۰۰۰۲", substring = true)
    }

    @Test
    fun managementFetchCancelApplyAndUnknownBalanceAreHonestLocalState() {
        enterAccounts()
        compose.onNodeWithTag("account-management").performClick()
        compose.onNodeWithTag("S-063").assertIsDisplayed()
        compose.onNodeWithTag("fetch-accounts").performClick()
        compose.onNodeWithTag("S-067").assertIsDisplayed()
        compose.onNodeWithTag("ownership-مشترک").performClick()
        compose.onNodeWithTag("choose-account-3").performClick()
        back()
        compose.onNodeWithTag("choose-account-3").assertDoesNotExist()
        compose.onNodeWithTag("fetch-accounts").performClick()
        compose.onNodeWithTag("ownership-مشترک").performClick()
        compose.onNodeWithTag("choose-account-3").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("choose-account-3").assertIsSelected()
        compose.onNodeWithTag("apply-accounts").performClick()
        compose.onNodeWithTag("ownership-مشترک").performClick()
        compose.onNodeWithTag("choose-account-3").assertExists()
        compose.onAllNodesWithText("حذف").onFirst().performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("حذف حساب", substring = true)
        back()
        back()
        compose.onNodeWithTag("account-list").performClick()
        compose.onNodeWithTag("ownership-مشترک").performClick()
        compose.onNodeWithTag("choose-account-3").performClick()
        compose.onNodeWithTag("account-balance").performClick()
        compose.onNodeWithText("نامشخص ریال · عدم نمایش").assertIsDisplayed()
        compose.onNodeWithTag("account-more").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        compose.onNodeWithTag("account-tab").assertIsDisplayed()
    }

    @Test
    fun bankStatementDatesPagingDetailAndRestorationUseTheirOwnSemanticIds() {
        enterAccounts()
        compose.onNodeWithTag("account-statement").performClick()
        compose.onNodeWithTag("S-016").assertIsDisplayed()
        compose.runOnIdle {
            val window = compose.activity.window
            assertTrue(
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars
            )
        }
        compose.onNodeWithTag("ledger-more").performScrollTo().performClick()
        compose.onNodeWithTag("statement-3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("statement-2").performScrollTo().performClick()
        compose.onNodeWithTag("S-014").assertIsDisplayed()
        compose.onNodeWithText("شناسه برداشت").assertIsDisplayed()
        compose.onNodeWithText("−۷۵۰٬۰۰۰ ریال", substring = true).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        back()
        compose.onNodeWithTag("statement-3").assertExists()
        compose.onNodeWithTag("ledger-start").performTextReplacement("1405/13/01")
        compose.onNodeWithTag("ledger-apply").performClick()
        compose.onNodeWithTag("ledger-error").assertIsDisplayed()
        compose.onNodeWithTag("statement-1").assertExists()
        compose.onNodeWithTag("ledger-start").performTextReplacement(dateDaysAgo(7))
        compose.onNodeWithTag("ledger-end").performTextReplacement(dateDaysAgo(8))
        compose.onNodeWithTag("ledger-apply").performClick()
        compose.onNodeWithTag("ledger-error").assertTextContains("ابتدایی", substring = true)
        compose.onNodeWithTag("ledger-end").performTextReplacement(dateDaysAgo(7))
        compose.onNodeWithTag("ledger-apply").performClick()
        compose.onNodeWithText("موردی برای نمایش وجود ندارد.").assertIsDisplayed()
    }

    @Test
    fun legacyHistorySearchDetailBackAndUnsupportedActionsRetainTheList() {
        compose.onNodeWithTag("demo-history").performClick()
        compose.onNodeWithTag("S-091").assertIsDisplayed()
        compose.onNodeWithTag("history-filter").assertIsNotEnabled()
        compose.onNodeWithTag("history-more").performClick()
        compose.onNodeWithTag("history-search").performTextReplacement("۷۵۰٬۰۰۰")
        compose.onNodeWithTag("history-preview-1").assertDoesNotExist()
        compose.onNodeWithTag("history-preview-2").performClick()
        compose.onNodeWithTag("S-090").assertIsDisplayed()
        compose.onNodeWithText("۷۵۰٬۰۰۰ ریال").assertIsDisplayed()
        compose.onNodeWithText("رسید").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        back()
        back()
        compose.onNodeWithTag("history-search").assertTextContains("۷۵۰٬۰۰۰")
        compose.onNodeWithTag("history-preview-2").assertIsDisplayed()
        compose.onNodeWithTag("history-search").performTextReplacement("نامی که وجود ندارد")
        compose.onNodeWithText("موردی برای نمایش وجود ندارد.").assertIsDisplayed()
    }

    @Test
    fun exportValidationCalendarAndNoPretendDownload() {
        compose.onNodeWithTag("demo-statement").performClick()
        compose.onNodeWithTag("S-086").assertIsDisplayed()
        compose.onNodeWithTag("statement-request").performClick()
        compose.onNodeWithTag("statement-error").assertIsDisplayed()
        compose.onNodeWithTag("statement-start-calendar").performClick()
        compose.onNodeWithTag("calendar-day-1").performClick()
        compose.onNodeWithTag("statement-start").assertTextContains("/01", substring = true)
        compose.onNodeWithTag("statement-start").performTextReplacement(dateDaysAgo(1))
        compose.onNodeWithTag("statement-end").performTextReplacement(todayPersian())
        compose.onNodeWithTag("export-format").performClick()
        compose.onNodeWithText("PDF صورتحساب").performClick()
        compose.onNodeWithTag("statement-request").performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("دریافت فایل", substring = true)
        compose.activityRule.scenario.recreate()
        back()
        compose.onNodeWithTag("statement-end").assertTextContains(todayPersian())
        compose.onNodeWithTag("export-format").assertTextContains("PDF صورتحساب")
    }

    @Test
    fun strictPersianCalendarAndRecoveredDateBoundariesAreValidated() {
        assertNotNull(persianDateMillis("۱۴۰۳/۱۲/۳۰"))
        assertNull(persianDateMillis("1404/12/30"))
        assertNull(persianDateMillis("1405/13/01"))
        assertNull(persianDateMillis("1405/08/07extra"))
        assertNull(statementDateError("1405/08/07", "1405/08/07", "1405/08/07"))
        assertTrue(statementDateError("1405/08/08", "1405/08/08", "1405/08/07")!!.contains("آینده"))
        assertTrue(
            bankStatementDateError("1403/01/01", "1405/08/07", "1405/08/07")!!.contains("۳۶۶")
        )
        assertTrue(
            bankStatementDateError("1405/04/01", "1405/07/01", "1405/08/07")!!.contains("۹۰")
        )
    }

    @Test
    fun localEmptyLoadingAndErrorScenariosHaveNoFabricatedAccountData() {
        listOf("empty", "loading", "error").forEach { scenario ->
            compose.onNodeWithTag("scenario-$scenario").performClick()
            enterAccounts()
            compose.onNodeWithTag("data-$scenario").assertIsDisplayed()
            compose.onNodeWithTag("account-list").assertDoesNotExist()
            if (scenario == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("account-tab").assertIsDisplayed()
            }
            back()
            back()
        }
    }
}
