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
class WalletOperationsTest {
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

    private fun type(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    private fun value(tag: String, text: String) {
        compose
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString(text),
                )
            )
    }

    private fun error(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().assertTextContains(text, substring = true)
    }

    private fun unavailable(title: String) {
        compose.onNodeWithTag("unavailable").assertTextContains(title, substring = true)
        back()
        compose.onNodeWithTag("unavailable").assertDoesNotExist()
    }

    private fun failure(text: String) {
        compose.onNodeWithTag("wallet-op-failure").assertTextEquals(text)
        compose.onNodeWithText("بستن").performClick()
        compose.onNodeWithTag("wallet-op-failure").assertDoesNotExist()
    }

    private fun wallet() {
        click("demo-S-111")
        shown("S-111")
    }

    @Test
    fun walletToWalletChecksTheDestinationAndBalanceThenKeepsTheDraftForCorrection() {
        wallet()
        click("wallet-action-AUTHENTICATED_WALLET_W2W")
        shown("S-116")
        click("wallet-op-continue")
        compose.onAllNodesWithText(PaymentRules.MANDATORY).assertCountEquals(2)
        type("wallet-op-destination", "0000000000101")
        click("wallet-op-continue")
        compose.onNodeWithText(WalletOperationRules.SAME).assertExists()
        type("wallet-op-destination", "000000000000020")
        click("wallet-op-continue")
        compose.onNodeWithText(PaymentRules.INVALID).assertExists()
        type("wallet-op-destination", "0000000000000201")
        type("wallet-op-amount", "3250001")
        click("wallet-op-continue")
        failure("موجودی ساختگی مبدا برای این مبلغ کافی نیست.")
        shown("S-116")
        type("wallet-op-amount", "400000")
        click("wallet-op-continue")
        shown("S-117")
        compose.onNodeWithText("گیرندهٔ نمایشی").assertExists()
        compose.onNodeWithText("۴۰۰٬۰۰۰").assertExists()
        click("wallet-op-note")
        compose.onNodeWithTag("wallet-op-note-text").performTextInput("یادداشت آزمون")
        dialog("wallet-op-note-accept")
        click("wallet-op-favorite")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("یادداشت آزمون").assertExists()
        compose.onNodeWithTag("wallet-op-favorite").assertIsOn()
        click("wallet-op-submit")
        unavailable("امضا و انجام کیف به کیف")
        shown("S-117")
        click("wallet-op-edit")
        shown("S-116")
        value("wallet-op-destination", "0000000000000201")
        value("wallet-op-amount", "400000")
        click("wallet-op-familiar")
        unavailable("مقصدهای آشنا")
        click("wallet-op-continue")
        click("wallet-op-cancel")
        shown("S-111")
        click("wallet-action-AUTHENTICATED_WALLET_W2W")
        value("wallet-op-destination", "")
    }

    @Test
    fun chargeAndWithdrawalUseAccountPickersAndStopBeforeTheOtp() {
        wallet()
        click("wallet-action-AUTHENTICATED_WALLET_CHARGE")
        dialog("wallet-charge-AUTHENTICATED_WALLET_CHARGE_ACCOUNT")
        shown("S-104")
        type("wallet-op-amount", "500000")
        click("wallet-op-continue")
        error("wallet-op-destination-error", "تکمیل این فیلد اجباری است.")
        click("wallet-op-destination")
        dialog("wallet-op-destination-0000000000102")
        click("wallet-op-continue")
        shown("S-105")
        compose.onNodeWithText("حساب مبدا").assertExists()
        click("wallet-op-submit")
        unavailable("تایید تراکنش با رمز پویا")
        compose.onNodeWithTag("otp-code").assertDoesNotExist()
        click("wallet-op-cancel")
        shown("S-111")

        click("wallet-action-AUTHENTICATED_WALLET_DECHARGE")
        shown("S-106")
        click("wallet-op-show-balance")
        compose.onNodeWithTag("wallet-op-balance").assertTextEquals("۳٬۲۵۰٬۰۰۰ ریال")
        click("wallet-op-source")
        dialog("wallet-op-source-0000000000102")
        compose.onNodeWithTag("wallet-op-balance").assertDoesNotExist()
        compose.onNodeWithTag("wallet-op-source-0000000000103").assertDoesNotExist()
        click("wallet-op-destination")
        dialog("wallet-op-destination-account-2")
        type("wallet-op-amount", "18000001")
        click("wallet-op-continue")
        failure("موجودی ساختگی مبدا برای این مبلغ کافی نیست.")
        type("wallet-op-amount", "18000000")
        click("wallet-op-continue")
        shown("S-107")
        compose.onNodeWithText("۱۸٬۰۰۰٬۰۰۰").assertExists()
        click("wallet-op-submit")
        unavailable("رمز پویا")
    }

    @Test
    fun purchaseNeedsAScannedDestinationAndNeverScans() {
        wallet()
        click("wallet-menu-AUTHENTICATED_WALLET_BUY_WITH_WALLET")
        shown("S-100")
        type("wallet-op-amount", "250000")
        click("wallet-op-continue")
        unavailable("خرید بدون بارکد اسکن‌شده")
        shown("S-100")
        back()
        back()
        back()
        click("demo-wallet-buy-qr")
        shown("S-100")
        compose.onNodeWithText("0000000000000301").assertExists()
        click("wallet-op-continue")
        compose.onAllNodesWithText(PaymentRules.MANDATORY).assertCountEquals(1)
        type("wallet-op-amount", "250000")
        click("wallet-op-continue")
        shown("S-101")
        compose.onNodeWithText("فروشگاه نمایشی").assertExists()
        click("wallet-op-submit")
        unavailable("خرید با کیف")
    }

    @Test
    fun mobileChargeAndBillReuseTheirRecoveredChecks() {
        wallet()
        click("wallet-menu-AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE")
        shown("S-102")
        click("wallet-op-continue")
        error("wallet-op-operator-error", "اپراتور تلفن همراه خود را انتخاب کنید.")
        error("wallet-op-amount-error", "تکمیل این فیلد اجباری است.")
        type("wallet-op-mobile", "09010000000")
        compose.onNodeWithTag("wallet-op-operator-IRANCELL").assertIsSelected()
        click("wallet-op-amount-200000")
        click("wallet-op-continue")
        shown("S-103")
        compose.onNodeWithText("ایرانسل").assertExists()
        compose.onNodeWithTag("wallet-op-note").assertDoesNotExist()
        back()
        back()

        click("wallet-menu-AUTHENTICATED_WALLET_PAY_BILL")
        shown("S-109")
        click("wallet-op-scan")
        unavailable("اسکن بارکد قبض")
        type("wallet-op-bill", "12345")
        click("wallet-op-continue")
        compose.onNodeWithText(PaymentRules.INVALID).assertExists()
        compose.onNodeWithText(PaymentRules.MANDATORY).assertExists()
        type("wallet-op-bill", "9000000000002")
        type("wallet-op-pay", "430002")
        click("wallet-op-continue")
        failure("این قبض در دادهٔ نمایشی پرداخت شده است.")
        type("wallet-op-bill", "9000000000001")
        type("wallet-op-pay", "1250001")
        click("wallet-op-continue")
        shown("S-099")
        compose.onNodeWithText("شرکت آب نمایشی").assertExists()
        compose.onNodeWithText("۱٬۲۵۰٬۰۰۰").assertExists()
        click("wallet-op-submit")
        unavailable("پرداخت قبض")
    }

    @Test
    fun scenarioStatesStayLocalAndTheConfirmationClearsTheBottomEdge() {
        for (state in listOf("empty", "loading", "error")) {
            click("scenario-$state")
            click("demo-wallet-buy-qr")
            compose.onNodeWithTag("data-$state").assertIsDisplayed()
            if (state == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("wallet-op-amount").assertExists()
            } else {
                back()
                back()
            }
        }
        type("wallet-op-amount", "250000")
        click("wallet-op-continue")
        shown("S-101")
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithTag("wallet-op-cancel").performScrollTo()
        cancel.assertIsDisplayed()
        assert(cancel.getUnclippedBoundsInRoot().bottom <= root.bottom)
        // RTL: the start-aligned Back control sits on the right.
        assert(compose.onNodeWithTag("page-back").getUnclippedBoundsInRoot().left > root.right / 2)
    }
}
