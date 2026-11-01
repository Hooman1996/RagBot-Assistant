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
class TollsTest {
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

    private fun plates(key: String) {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-vehicle").performScrollTo().performClick()
        click("vehicle-menu-AUTHENTICATED_HOME_$key")
        shown("S-256")
    }

    /** Fee account, then the local OTP sheet; the code is never kept. */
    private fun inquire() {
        click("toll-inquiry")
        // Choosing the fee source opens the OTP sheet, so focus stays in a dialog.
        compose.onNodeWithTag("toll-fee-account-1").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("otp-code").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("otp-code").performTextInput(PREVIEW_OTP)
        compose.onNodeWithTag("otp-ok").performClick()
    }

    @Test
    fun trafficTollSelectionReviewAndBackKeepTheVehicle() {
        plates("TRAFFIC_TOLL")
        click("vehicle-plate-1")
        shown("S-275")
        inquire()
        shown("S-276")
        compose.onNodeWithText("120234567").assertExists()
        click("toll-pay")
        compose.onNodeWithTag("toll-select-error").assertExists()
        click("toll-check-traffic-2")
        click("toll-check-traffic-3")
        compose.onNodeWithTag("toll-total").assertTextEquals("مبلغ کل: ۳۲۵٬۰۰۰ ریال")
        click("toll-select-all")
        compose.onNodeWithTag("toll-total").assertTextEquals("مبلغ کل: ۴۴۵٬۰۰۰ ریال")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("toll-total").assertTextEquals("مبلغ کل: ۴۴۵٬۰۰۰ ریال")
        click("toll-pay")
        shown("S-274")
        compose.onNodeWithTag("toll-confirm-total").assertTextEquals("مبلغ کل: ۴۴۵٬۰۰۰ ریال")
        click("payment-submit")
        unavailable("پرداخت عوارض شهرداری")
        compose.onNodeWithTag("otp-code").assertDoesNotExist()
        click("payment-cancel")
        shown("S-276")
        compose.onNodeWithTag("toll-total").assertTextEquals("مبلغ کل: ۴۴۵٬۰۰۰ ریال")
    }

    @Test
    fun othersOwnershipAndAnnualVinAreCheckedBeforeTheInquiry() {
        plates("ANNUAL_TOLL")
        click("vehicle-new")
        shown("S-241")
        click("toll-inquiry")
        compose.onNodeWithTag("toll-plate-error").assertExists()
        compose.onNodeWithTag("toll-plate-0").performTextInput("12")
        click("toll-letter")
        dialog("toll-letter-ب")
        compose.onNodeWithTag("toll-plate-2").performTextInput("345")
        compose.onNodeWithTag("toll-plate-3").performTextInput("67")
        click("toll-owner-false")
        click("toll-inquiry")
        compose.onAllNodesWithText(PaymentRules.MANDATORY).assertCountEquals(3)
        compose.onNodeWithTag("toll-code").performTextReplacement("0000000018")
        compose.onNodeWithTag("toll-phone").performTextReplacement("09123456789")
        compose.onNodeWithTag("toll-vin").performTextReplacement("IRFC0000000006903")
        click("toll-inquiry")
        compose.onNodeWithText(PaymentRules.INVALID).assertExists()
        compose.onNodeWithTag("toll-code").performTextReplacement("0000000019")
        inquire()
        shown("S-242")
        compose.onNodeWithTag("toll-total").assertTextEquals("مبلغ کل: ۱٬۰۲۵٬۰۰۰ ریال")
        click("toll-pay")
        shown("S-240")
        back()
        back()
        shown("S-241")
        compose.onNodeWithTag("toll-vin").assertTextContains("IRFC0000000006903")
    }

    @Test
    fun noDuesPollutionQuotaAndReceiptStayLocal() {
        plates("FREEWAY_TOLL")
        click("vehicle-plate-2")
        shown("S-250")
        inquire()
        shown("S-251")
        compose.onNodeWithTag("toll-no-dues").assertExists()
        compose.onNodeWithTag("toll-pay").assertDoesNotExist()
        back()
        back()
        back()
        click("vehicle-menu-AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY")
        click("vehicle-plate-1")
        shown("S-257")
        compose.onNodeWithTag("toll-type-false").assertDoesNotExist()
        click("toll-inquiry")
        compose.onNodeWithTag("toll-fee-0000000000101").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("otp-code").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("otp-code").performTextInput(PREVIEW_OTP)
        compose.onNodeWithTag("otp-ok").performClick()
        shown("S-258")
        compose.onNodeWithText("سهمیه باقیمانده فصل جاری").assertExists()
        compose.onNodeWithTag("toll-pay").assertDoesNotExist()
    }

    @Test
    fun receiptEntryAndErrorScenarioStayLocal() {
        click("demo-S-261")
        shown("S-261")
        compose.onNodeWithTag("toll-receipt-disclosure").assertExists()
        click("toll-track")
        unavailable("پیگیری تراکنش")
        back()
        back()
        click("scenario-error")
        click("demo-S-261")
        compose.onNodeWithTag("data-error").assertIsDisplayed()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.onNodeWithTag("toll-receipt-disclosure").assertExists()
    }
}
