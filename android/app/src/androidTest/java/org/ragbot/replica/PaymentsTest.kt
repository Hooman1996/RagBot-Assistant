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
class PaymentsTest {
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

    private fun demo(id: String) {
        compose.onNodeWithTag("demo-$id").performScrollTo().performClick()
        shown(id)
    }

    private fun unavailable(title: String) {
        compose.onNodeWithTag("unavailable").assertTextContains(title, substring = true)
        back()
        compose.onNodeWithTag("unavailable").assertDoesNotExist()
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

    @Test
    fun accountBillValidatesInquiresAndKeepsCorrectionsWithoutPaying() {
        demo("S-209")
        click("bill-continue")
        compose.onAllNodesWithText(PaymentRules.MANDATORY).assertCountEquals(2)
        compose.onNodeWithTag("bill-id").performTextInput("12345")
        // Rendered review: editing a field clears only its own stale error.
        compose.onAllNodesWithText(PaymentRules.MANDATORY).assertCountEquals(1)
        compose.onNodeWithTag("bill-pay-id").performTextInput("12345678901234")
        value("bill-pay-id", "")
        compose.onNodeWithTag("bill-pay-id").performTextInput("430002")
        click("bill-continue")
        compose.onNodeWithText(PaymentRules.INVALID).assertExists()
        compose.onNodeWithTag("bill-id").performTextReplacement("9000000000002")
        click("bill-continue")
        compose.onNodeWithTag("bill-error").assertTextContains("پرداخت شده", substring = true)
        compose.onNodeWithTag("bill-id").performTextReplacement("9000000000001")
        compose.onNodeWithTag("bill-pay-id").performTextReplacement("1250001")
        click("bill-source")
        dialog("bill-source-account-2")
        click("bill-continue")
        shown("S-207")
        compose.onNodeWithText("شرکت آب نمایشی").assertExists()
        compose.onNodeWithText("۱٬۲۵۰٬۰۰۰").assertExists()
        compose.onNodeWithText("پس‌انداز نمایشی · ۰۰۰۰۰۰۰۰۰۲").assertExists()
        click("bill-attachment")
        unavailable("پیوست")
        click("bill-accept")
        unavailable("پرداخت قبض")
        compose.activityRule.scenario.recreate()
        shown("S-207")
        click("bill-edit")
        shown("S-209")
        value("bill-id", "9000000000001")
        click("bill-scan")
        unavailable("بارکد")
        back()
        back()
        compose.onNodeWithTag("demo-S-209").assertExists()
    }

    @Test
    fun cardSourceModesOpenTheirOwnCardInfoAndNeverKeepSecrets() {
        demo("S-210")
        compose.onNodeWithTag("bill-id").performTextInput("9000000000001")
        compose.onNodeWithTag("bill-pay-id").performTextInput("1250001")
        click("bill-continue")
        shown("S-207")
        compose.onNodeWithText(CardFixture.of(CardKind.DEBIT).first().number).assertExists()
        click("bill-accept")
        shown("S-205")
        compose.onNodeWithTag("payment-card-cvv2").performTextInput("123")
        click("payment-card-static")
        compose.onNodeWithTag("payment-card-otp").assertDoesNotExist()
        click("payment-card-dynamic")
        click("payment-card-otp")
        unavailable("رمز پویا")
        click("payment-submit")
        unavailable("پرداخت قبض")
        compose.activityRule.scenario.recreate()
        value("payment-card-cvv2", "")
        repeat(4) { back() }
        demo("S-208")
        value("bill-id", "")
        compose.onNodeWithTag("bill-source").performTextInput("123")
        compose.onNodeWithTag("bill-id").performTextInput("9000000000001")
        compose.onNodeWithTag("bill-pay-id").performTextInput("1250001")
        click("bill-continue")
        compose.onNodeWithText(PaymentRules.INVALID).assertExists()
        compose.onNodeWithTag("bill-source").performTextReplacement("0000000000000001")
        click("bill-continue")
        shown("S-207")
        click("bill-accept")
        shown("S-206")
        compose.onNodeWithTag("payment-card-static").assertDoesNotExist()
        compose.onNodeWithTag("payment-card-keep").assertDoesNotExist()
        click("payment-submit")
        unavailable("پرداخت قبض")
    }

    @Test
    fun insuranceSelectsOnlyPayableInstallmentsAndConfirmsWithoutPaying() {
        demo("S-403")
        click("insurance-history")
        unavailable("سوابق")
        click("insurance-list")
        shown("S-402")
        compose.onNodeWithText(PaymentFixture.insuredName).assertExists()
        click("insurance-policy-policy-2")
        shown("S-401")
        compose.onNodeWithTag("insurance-pay").assertDoesNotExist()
        back()
        click("insurance-policy-policy-1")
        click("insurance-pay")
        compose.onNodeWithTag("insurance-error").assertTextContains(PaymentRules.SELECT_INSTALLMENT)
        click("installment-1")
        compose.onNodeWithTag("installment-1").assertIsNotSelected()
        compose.onNodeWithTag("installment-2").assertIsNotEnabled()
        click("installment-3")
        compose.onNodeWithTag("installment-3").assertIsSelected()
        compose.onNodeWithTag("insurance-error").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("installment-3").assertIsSelected()
        click("insurance-pay")
        dialog("insurance-source-account-1")
        shown("S-404")
        compose.onNodeWithText("۱٬۵۰۰٬۰۰۰").assertExists()
        click("payment-submit")
        unavailable("پرداخت قسط بیمه")
        click("payment-cancel")
        shown("S-401")
        compose.onNodeWithTag("installment-3").assertIsSelected()
        repeat(4) { back() }
        demo("S-405")
        compose.onNodeWithTag("payment-receipt-disclosure").assertExists()
        click("payment-receipt-share")
        unavailable("اشتراک‌گذاری")
        back()
        back()
        compose.onNodeWithTag("scenario-empty").performScrollTo().performClick()
        demo("S-403")
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
    }

    @Test
    fun creditRepaymentChecksTheAmountAndConfirmsWithoutPaying() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("card-kind-CREDIT").performClick()
        compose.onNodeWithTag("card-action-credit").performClick()
        shown("S-131")
        click("credit-pay")
        // The amount dialog stays open while its amount is rejected.
        compose.onNodeWithTag("credit-amount-pay").performClick()
        compose.onNodeWithText(PaymentRules.MANDATORY).assertExists()
        compose.onNodeWithTag("credit-amount").performTextInput("0")
        compose.onNodeWithTag("credit-amount-pay").performClick()
        compose.onNodeWithText(PaymentRules.OUT_OF_RANGE).assertExists()
        compose.onNodeWithTag("credit-amount").performTextReplacement("180000001")
        compose.onNodeWithTag("credit-amount-pay").performClick()
        compose.onNodeWithText(PaymentRules.OVER_DEBT).assertExists()
        compose.onNodeWithTag("credit-amount").performTextReplacement("50000000")
        compose.onNodeWithTag("credit-amount-pay").performClick()
        dialog("credit-source-account-2")
        shown("S-132")
        compose.onNodeWithText("۵۰٬۰۰۰٬۰۰۰").assertExists()
        click("payment-submit")
        unavailable("بازپرداخت کارت اعتباری")
        click("payment-cancel")
        shown("S-131")
    }
}
