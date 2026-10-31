package org.ragbot.replica

import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdentifierConversionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun click(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun input(value: String) {
        compose
            .onNodeWithTag("conversion-identifier")
            .performScrollTo()
            .performTextReplacement(value)
    }

    private fun mode(mode: ConversionMode) = click("conversion-mode-${mode.name}")

    private fun calculate() = click("conversion-calculate")

    private fun bank(key: String) {
        click("conversion-bank")
        compose.onNodeWithTag("conversion-bank-$key").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
    }

    @Test
    fun accountRequiresBankAndResultCopyAndBackPreserveTheLocalDraft() {
        click("demo-S-201")
        mode(ConversionMode.ACCOUNT_TO_IBAN)
        calculate()
        compose.onNodeWithTag("conversion-error").assertTextContains("اجباری", substring = true)
        input(ConversionFixture.account)
        calculate()
        compose.onNodeWithTag("conversion-error").assertTextEquals("بانک را انتخاب کنید.")
        bank("internal")
        click("conversion-favorite")
        calculate()
        compose.onNodeWithTag("S-011").assertIsDisplayed()
        click("conversion-copy-0-شماره شبا")
        compose.runOnUiThread {
            val clipboard =
                compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(
                ConversionFixture.iban,
                clipboard.primaryClip!!.getItemAt(0).text.toString(),
            )
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("S-011").assertIsDisplayed()
        compose.onNodeWithTag("conversion-return").performClick()
        compose.onNodeWithTag("conversion-identifier").assertTextContains(ConversionFixture.account)
        compose.onNodeWithTag("conversion-favorite").assertIsOn()
        bank("external")
        calculate()
        compose.onNodeWithTag("unavailable").assertTextContains("استعلام کارمزدی", substring = true)
        back()
        compose.onNodeWithTag("S-201").assertIsDisplayed()
    }

    @Test
    fun cardDirectionsKeepSeparateInputsAndNeverInventOwnershipOrPaidResults() {
        click("demo-S-202")
        mode(ConversionMode.CARD_TO_IBAN)
        input("123")
        calculate()
        compose.onNodeWithTag("conversion-error").assertExists()
        input("0000 0000 0000 0001")
        calculate()
        compose.onNodeWithText(ConversionFixture.iban).assertExists()
        back()
        mode(ConversionMode.CARD_TO_ACCOUNT)
        compose.onNodeWithTag("conversion-identifier").assertTextEquals("شماره کارت", "")
        input(ConversionFixture.card)
        calculate()
        compose.onNodeWithText(ConversionFixture.account).assertExists()
        compose.onNodeWithText(ConversionFixture.iban).assertDoesNotExist()
        back()
        input("0000000000000003")
        calculate()
        compose
            .onNodeWithTag("conversion-error")
            .assertTextContains("دادهٔ ساختگی", substring = true)
        input("0000010000000001")
        calculate()
        compose.onNodeWithTag("unavailable").assertExists()
        back()
        mode(ConversionMode.CARD_TO_IBAN)
        compose.onNodeWithTag("conversion-identifier").assertTextContains("0000 0000 0000 0001")
    }

    @Test
    fun ibanChecksChecksumAndOwnBankThenShowsAccountOrMultipleDistinctCards() {
        click("demo-S-202")
        mode(ConversionMode.IBAN_TO_ACCOUNT)
        input("IR230000000000000000000001")
        calculate()
        compose.onNodeWithTag("conversion-error").assertTextEquals("شماره شبا معتبر نیست.")
        input("IR480010000000000000000001")
        calculate()
        compose.onNodeWithTag("conversion-error").assertTextContains("فقط", substring = true)
        input(ConversionFixture.iban)
        calculate()
        compose.onNodeWithText(ConversionFixture.account).assertExists()
        back()
        mode(ConversionMode.IBAN_TO_CARD)
        input(ConversionFixture.iban)
        compose.activityRule.scenario.recreate()
        calculate()
        compose.onNodeWithTag("conversion-result-0").assertExists()
        compose.onNodeWithTag("conversion-result-1").assertExists()
        compose.onNodeWithText(ConversionFixture.cards[1]).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("conversion-return").performClick()
        compose.onNodeWithTag("S-202").assertIsDisplayed()
    }

    @Test
    fun paidPickerPreviewCannotPayAndRecreationClearsSecrets() {
        click("demo-S-203")
        click("conversion-source-picker")
        compose.waitUntil(5_000) { !compose.activity.hasWindowFocus() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("S-203").assertIsDisplayed()
        click("conversion-source-picker")
        compose.onNodeWithTag("conversion-source-card-1").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("payment-card-password").performScrollTo().performTextInput("876543")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("payment-card-password").assertTextEquals("رمز دوم", "")
        compose
            .onNodeWithTag("conversion-source-picker")
            .assertTextContains(CardFixture.of(CardKind.DEBIT).first().number)
        click("payment-submit")
        compose.onNodeWithTag("unavailable").assertExists()
        back()
        compose.onNodeWithTag("S-203").assertIsDisplayed()
        click("payment-cancel")
        compose.onNodeWithTag("S-011").assertDoesNotExist()
    }

    @Test
    fun typedPaidPreviewKeepsOnlySourceAndOtpAndFinalActionsAreUnavailable() {
        click("demo-S-204")
        compose.onNodeWithTag("conversion-source-picker").assertDoesNotExist()
        compose.onNodeWithTag("conversion-source").performTextInput(ConversionFixture.card)
        compose.onNodeWithTag("payment-card-cvv2").performScrollTo().performTextInput("123")
        compose.onNodeWithTag("payment-card-expiry").performScrollTo().performTextInput("0101")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("conversion-source").assertTextContains(ConversionFixture.card)
        compose.onNodeWithTag("payment-card-cvv2").assertTextEquals("CVV2", "")
        compose.onNodeWithTag("payment-card-expiry").assertTextEquals("تاریخ انقضا", "")
        click("payment-card-otp")
        compose.onNodeWithTag("unavailable").assertExists()
        back()
        click("payment-submit")
        compose.onNodeWithTag("unavailable").assertExists()
        back()
        compose.onNodeWithTag("S-204").assertIsDisplayed()
        compose.onNodeWithTag("S-011").assertDoesNotExist()
    }

    @Test
    fun emptyLoadingAndErrorRetryRemainLocal() {
        for (state in listOf("empty", "loading", "error")) {
            click("scenario-$state")
            click("demo-S-201")
            compose.onNodeWithTag("data-$state").assertIsDisplayed()
            if (state == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                mode(ConversionMode.ACCOUNT_TO_IBAN)
                compose.onNodeWithTag("conversion-identifier").assertExists()
            } else {
                back()
                back()
            }
        }
    }
}
