package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun click(tag: String) {
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

    private fun qr(text: String) {
        compose.onNodeWithTag("wallet-qr-content").assertTextContains(text, substring = true)
    }

    @Test
    fun homeWalletShowsWalletsAndKeepsTheStatementSelectionThroughDetailAndBack() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-wallet").performScrollTo().performClick()
        shown("S-111")
        compose.onNodeWithTag("wallet-0000000000103").assertExists()
        compose.onNodeWithText("حقیقی/ غیرفعال").assertExists()
        click("wallet-show-0000000000102")
        compose.onNodeWithTag("wallet-balance-0000000000102").assertTextEquals("۱۸٬۰۰۰٬۰۰۰ ریال")
        compose.onNodeWithTag("wallet-balance-0000000000101").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("wallet-balance-0000000000102").assertExists()
        click("wallet-hide-0000000000102")
        compose.onNodeWithTag("wallet-show-0000000000102").assertExists()
        click("wallet-action-AUTHENTICATED_WALLET_CHARGE")
        dialog("wallet-charge-AUTHENTICATED_WALLET_CHARGE_IPG")
        shown("S-231")
        back()
        shown("S-111")
        click("wallet-menu-AUTHENTICATED_WALLET_STATEMENT")
        shown("S-115")
        click("wallet-picker")
        compose.onNodeWithTag("wallet-pick-0000000000103").assertDoesNotExist()
        dialog("wallet-pick-0000000000102")
        compose.onNodeWithText("کیف نمایشی تجاری · 0000000000102").assertExists()
        compose.onNodeWithTag("wallet-row-9").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wallet-row-10").assertDoesNotExist()
        click("wallet-more")
        compose.onNodeWithTag("wallet-row-11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wallet-more").assertDoesNotExist()
        compose.onNodeWithTag("wallet-start").performTextReplacement(todayPersian())
        compose.onNodeWithTag("wallet-end").performTextReplacement(dateDaysAgo(3))
        click("wallet-apply")
        compose.onNodeWithTag("wallet-date-error").assertExists()
        compose.onNodeWithTag("wallet-row-11").assertExists()
        click("wallet-row-10")
        shown("S-114")
        compose.onNodeWithText("0000000000102").assertExists()
        compose.onNodeWithText("کد رهگیری:").assertExists()
        compose.onNodeWithText("DEMO-TX-110").assertExists()
        // Row 10 is a gateway payment: no participant or name row.
        compose.onNodeWithText("مقصد:").assertDoesNotExist()
        compose.onNodeWithText("نام").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        back()
        shown("S-115")
        compose.onNodeWithText("کیف نمایشی تجاری · 0000000000102").assertExists()
        compose.onNodeWithTag("wallet-row-11").assertExists()
        click("wallet-export")
        unavailable("دریافت فایل")
    }

    @Test
    fun qrShowsTheLocalPayloadForTheChosenWalletAndCannotPay() {
        compose.onNodeWithTag("demo-S-111").performScrollTo().performClick()
        click("wallet-menu-AUTHENTICATED_WALLET_RECEIVE_BARCODE")
        shown("S-112")
        qr("\"amount\": null")
        qr("\"destination\": \"0000000000101\"")
        click("wallet-add-amount")
        compose.onNodeWithTag("wallet-amount").performTextInput("25a")
        compose.onNodeWithTag("wallet-amount-accept").assertIsNotEnabled()
        compose.onNodeWithTag("wallet-amount").performTextReplacement("250000")
        dialog("wallet-amount-accept")
        qr("\"amount\": \"250000\"")
        compose.activityRule.scenario.recreate()
        qr("\"amount\": \"250000\"")
        click("wallet-picker")
        dialog("wallet-pick-0000000000102")
        qr("\"destination\": \"0000000000102\"")
        click("wallet-copy")
        compose.onNodeWithText("شناسه با موفقیت کپی شد.").assertExists()
        compose.onNodeWithText("بستن").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        click("wallet-share")
        unavailable("اشتراک‌گذاری")
        shown("S-112")
        click("wallet-remove-amount")
        qr("\"amount\": null")
    }

    @Test
    fun historyMenuOpensTheTransactionListAndStatesStaySynthetic() {
        compose.onNodeWithTag("demo-S-111").performScrollTo().performClick()
        click("wallet-menu-AUTHENTICATED_WALLET_TRANSACTION_HISTORY")
        shown("S-110")
        WalletFixture.history.forEach {
            compose.onNodeWithTag("wallet-menu-${it.key}").assertExists()
        }
        click("wallet-menu-AUTHENTICATED_WALLET_W2W_TRANSACTION_HISTORY")
        shown("S-091")
        back()
        shown("S-110")
        repeat(3) { back() }
        compose.onNodeWithTag("demo-S-113").performScrollTo().performClick()
        shown("S-113")
        compose.onNodeWithTag("wallet-receipt-disclosure").assertExists()
        click("wallet-receipt-share")
        unavailable("اشتراک‌گذاری")
        repeat(2) { back() }
        compose.onNodeWithTag("scenario-empty").performScrollTo().performClick()
        compose.onNodeWithTag("demo-S-111").performScrollTo().performClick()
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
        repeat(2) { back() }
        compose.onNodeWithTag("scenario-error").performScrollTo().performClick()
        compose.onNodeWithTag("demo-S-111").performScrollTo().performClick()
        compose.onNodeWithTag("data-error").assertIsDisplayed()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.onNodeWithTag("wallet-0000000000101").assertExists()
    }
}
