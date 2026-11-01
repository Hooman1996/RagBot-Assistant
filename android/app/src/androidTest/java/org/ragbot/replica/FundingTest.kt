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
class FundingTest {
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

    private fun amount(text: String) {
        compose.onNodeWithTag("funding-amount").performScrollTo().performTextReplacement(text)
    }

    private fun value(text: String) {
        compose
            .onNodeWithTag("funding-amount")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString(text),
                )
            )
    }

    private fun error(text: String) {
        click("funding-confirm")
        compose.onNodeWithTag("funding-error", useUnmergedTree = true).assertTextEquals(text)
    }

    private fun unavailable(title: String) {
        compose.onNodeWithTag("unavailable").assertTextContains(title, substring = true)
        back()
        compose.onNodeWithTag("unavailable").assertDoesNotExist()
    }

    @Test
    fun homeIncreaseBalanceValidatesTheAmountAndWaitsThroughAPendingResult() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithText("افزایش موجودی").performClick()
        shown("S-222")
        value("1000000")
        click("funding-plus")
        value("1100000")
        click("funding-minus")
        click("funding-minus")
        value("900000")
        amount("")
        error(PaymentRules.MANDATORY)
        amount("5000")
        error("حداقل مبلغ افزایش موجودی ۱۰٬۰۰۰ ریال است.")
        amount("20000")
        error("مقدار وارد شده از حداقل مبلغ مورد نیازکم تر است.")
        amount("600000000")
        error("حداکثر مبلغ افزایش موجودی ۵۰۰٬۰۰۰٬۰۰۰ ریال است.")
        amount("300000000")
        error("مقدار وارد شده از حداکثر مبلغ بیش تر است.")
        click("funding-proposed-5000000")
        value("5000000")
        click("funding-target")
        dialog("funding-target-account-2")
        click("funding-confirm")
        shown("S-228")
        compose.onNodeWithTag("funding-gateway-disclosure").assertExists()
        click("funding-gateway-error")
        compose
            .onNodeWithTag("unavailable")
            .assertTextContains("لطفا اتصال اینترنت را بررسی و دوباره تلاش کنید.")
        back()
        shown("S-222")
        value("5000000")
        click("funding-confirm")
        click("funding-outcome-PENDING")
        shown("S-224")
        click("funding-fetch")
        compose.onNodeWithTag("funding-pending").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("funding-pending").assertExists()
        click("funding-fetch")
        shown("S-217")
        compose
            .onNodeWithTag("funding-status")
            .assertTextEquals("فرایند افزایش موجودی با موفقیت انجام شد.")
        compose.onNodeWithText("۵٬۰۰۰٬۰۰۰").assertExists()
        compose.onNodeWithText("پس‌انداز نمایشی · ۰۰۰۰۰۰۰۰۰۲").assertExists()
        click("funding-share")
        unavailable("اشتراک‌گذاری رسید")
        click("funding-done")
        shown("S-026")
        compose.onNodeWithText("افزایش موجودی").performClick()
        value("1000000")
    }

    @Test
    fun walletGatewayChargeEndsOnTheWalletReceiptWithTheReturnedStatus() {
        compose.onNodeWithTag("demo-S-111").performScrollTo().performClick()
        click("wallet-action-AUTHENTICATED_WALLET_CHARGE")
        dialog("wallet-charge-AUTHENTICATED_WALLET_CHARGE_IPG")
        shown("S-231")
        click("funding-target")
        compose.onNodeWithTag("funding-target-0000000000103").assertDoesNotExist()
        dialog("funding-target-0000000000102")
        click("funding-confirm")
        shown("S-225")
        click("funding-outcome-FAILED")
        shown("S-235")
        click("funding-fetch")
        shown("S-113")
        compose.onNodeWithText("افزایش موجودی | ناموفق").assertExists()
        compose.onNodeWithText("0000000000102").assertExists()
        back()
        shown("S-111")
    }

    @Test
    fun inFlowGraphsKeepTheirDestinationAndEndOnTheirOwnReceipts() {
        click("demo-S-221")
        compose.onNodeWithTag("funding-target").assertDoesNotExist()
        compose.onNodeWithText("حساب مقصد").assertExists()
        click("funding-confirm")
        shown("S-227")
        click("funding-outcome-UNKNOWN")
        shown("S-223")
        click("funding-fetch")
        shown("S-232")
        compose
            .onNodeWithTag("funding-status")
            .assertTextEquals("فرایند افزایش موجودی نامعلوم است.")
        compose.onNodeWithText("شماره پیگیری: DEMO-IPG-0001").assertExists()
        click("funding-track")
        unavailable("پیگیری نتیجهٔ افزایش موجودی")
        click("funding-done")
        shown("S-026")
        back()
        compose.onNodeWithTag("demo-S-230").assertExists()

        click("demo-S-230")
        compose.onNodeWithText("کیف مقصد").assertExists()
        click("funding-confirm")
        click("funding-outcome-SUCCESS")
        shown("S-234")
        click("funding-fetch")
        shown("S-233")
        compose.onNodeWithText("مبلغ ۱٬۰۰۰٬۰۰۰ ریال شارژ شد").assertExists()
        click("funding-done")
        shown("S-026")
        back()
        compose.onNodeWithTag("demo-S-230").assertExists()
    }

    @Test
    fun scenarioStatesStayLocalAndTheReturnLeavesTheTopUp() {
        for (state in listOf("empty", "loading", "error")) {
            click("scenario-$state")
            click("demo-S-221")
            compose.onNodeWithTag("data-$state").assertIsDisplayed()
            if (state == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("funding-amount").assertExists()
            } else {
                back()
                back()
            }
        }
        amount("2000000")
        click("funding-return")
        shown("S-026")
        back()
        click("demo-S-221")
        value("1000000")
    }
}
