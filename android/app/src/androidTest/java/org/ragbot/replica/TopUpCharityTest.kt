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
class TopUpCharityTest {
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

    private fun amountError(text: String) {
        compose.onNodeWithText(text, substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun accountTopUpDetectsTheOperatorAndChecksItsAmountRules() {
        demo("S-211")
        click("topup-continue")
        compose.onNodeWithTag("topup-error").assertTextContains(TopUpRules.CHOOSE_OPERATOR)
        compose.onAllNodesWithText(PaymentRules.MANDATORY).assertCountEquals(2)
        compose.onNodeWithTag("topup-phone").performTextInput("0901000")
        compose.onNodeWithTag("topup-operator-IRANCELL").assertIsSelected()
        compose.onNodeWithTag("topup-error").assertDoesNotExist()
        compose.onNodeWithTag("topup-amount").performTextInput("75000")
        click("topup-continue")
        amountError(PaymentRules.INVALID)
        compose.onNodeWithTag("topup-phone").performTextReplacement("09010000000")
        click("topup-continue")
        amountError("ضریبی از ۵۰٬۰۰۰ ریال")
        click("topup-operator-RIGHTEL")
        compose.onNodeWithTag("topup-amount").performTextReplacement("2500000")
        click("topup-continue")
        amountError("حداکثر مبلغ شارژ برای این اپراتور ۲٬۰۰۰٬۰۰۰ ریال است.")
        click("topup-amount-500000")
        value("topup-amount", "500000")
        click("topup-continue")
        shown("S-212")
        compose.onNodeWithText("رایتل · 09010000000").assertExists()
        click("topup-accept")
        unavailable("خرید شارژ")
        compose.activityRule.scenario.recreate()
        click("topup-edit")
        shown("S-211")
        value("topup-phone", "09010000000")
        compose.onNodeWithTag("topup-operator-RIGHTEL").assertIsSelected()
        compose.onNodeWithTag("topup-phone").performTextReplacement("090")
        compose.onNodeWithTag("topup-operator-RIGHTEL").assertIsNotSelected()
    }

    @Test
    fun cardTopUpModesOpenTheirOwnCardInfoWithoutKeepingSecrets() {
        demo("S-213")
        compose.onNodeWithTag("topup-phone").performTextInput("09000000000")
        click("topup-amount-100000")
        click("topup-continue")
        shown("S-212")
        click("topup-accept")
        shown("S-215")
        compose.onNodeWithTag("payment-card-cvv2").performTextInput("123")
        compose.onNodeWithTag("payment-card-keep").assertExists()
        click("payment-submit")
        unavailable("خرید شارژ")
        compose.activityRule.scenario.recreate()
        value("payment-card-cvv2", "")
        repeat(4) { back() }
        demo("S-214")
        compose.onNodeWithTag("topup-source").performTextInput("123")
        compose.onNodeWithTag("topup-phone").performTextInput("09000000000")
        click("topup-amount-100000")
        click("topup-continue")
        compose.onNodeWithText(PaymentRules.INVALID).assertExists()
        compose.onNodeWithTag("topup-source").performTextReplacement("0000000000000001")
        click("topup-continue")
        click("topup-accept")
        shown("S-216")
        compose.onNodeWithTag("payment-card-keep").assertDoesNotExist()
        click("payment-card-otp")
        unavailable("رمز پویا")
    }

    @Test
    fun charityChoosesASyntheticCharityAndConfirmsWithoutDonating() {
        demo("S-283")
        click("charity-2")
        shown("S-284")
        compose.onNodeWithText("خیریهٔ نمایشی سلامت").assertExists()
        click("charity-continue")
        compose.onNodeWithText(PaymentRules.MANDATORY).assertExists()
        compose.onNodeWithTag("charity-amount").performTextInput("9999")
        click("charity-continue")
        compose.onNodeWithText(PaymentRules.OUT_OF_RANGE).assertExists()
        compose.onNodeWithTag("charity-amount").performTextReplacement("500000")
        click("charity-source")
        dialog("charity-source-account-3")
        click("charity-extra")
        compose.onNodeWithTag("charity-description").performTextInput("کمک نمونه")
        click("charity-continue")
        shown("S-282")
        compose.onNodeWithText("۵۰۰٬۰۰۰").assertExists()
        compose.onNodeWithText("کمک نمونه").assertExists()
        click("charity-attachment")
        unavailable("پیوست")
        click("payment-submit")
        unavailable("کمک به خیریه")
        compose.activityRule.scenario.recreate()
        click("payment-cancel")
        shown("S-284")
        value("charity-amount", "500000")
        click("charity-change")
        shown("S-283")
        click("charity-1")
        compose.onNodeWithText("خیریهٔ نمایشی آموزش").assertExists()
        value("charity-amount", "500000")
        repeat(3) { back() }
        demo("S-095")
        click("payment-receipt-share")
        unavailable("اشتراک‌گذاری")
        repeat(2) { back() }
        compose.onNodeWithTag("scenario-empty").performScrollTo().performClick()
        demo("S-283")
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
    }
}
