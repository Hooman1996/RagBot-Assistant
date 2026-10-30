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
class IndividualTransfersTest {
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

    private fun open(id: String) {
        closeKeyboard()
        compose.onNodeWithTag("demo-$id").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onNodeWithTag(id).isDisplayed() }
    }

    private fun click(tag: String) {
        closeKeyboard()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    @Test
    fun cardDraftValidationReviewCorrectionsAndSecretsRespectTheBoundary() {
        open("S-175")
        click("card-transfer-review")
        compose.onNodeWithTag("S-175").assertIsDisplayed()
        compose.onNodeWithTag("S-176").assertDoesNotExist()
        compose.onNodeWithTag("card-transfer-source").performTextInput("0000000000000001")
        compose.onNodeWithTag("card-transfer-destination").performTextInput("0000000000000002")
        compose.onNodeWithTag("card-transfer-amount").performTextInput("250000")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("card-transfer-amount").assertTextContains("250000", substring = true)
        click("card-transfer-review")
        compose.waitUntil(5_000) { compose.onNodeWithTag("S-176").isDisplayed() }
        compose
            .onNodeWithTag("card-transfer-review-destination")
            .assertTextContains("0000000000000002", substring = true)
        compose.onNodeWithTag("card-transfer-cvv2").performTextInput("123")
        compose.onNodeWithTag("card-transfer-password").performScrollTo().performTextInput("98765")
        compose
            .onNodeWithTag("card-transfer-note")
            .performScrollTo()
            .performTextInput("یادداشت محلی DEMO")
        compose.activityRule.scenario.recreate()
        for (tag in
            listOf("card-transfer-cvv2", "card-transfer-password", "card-transfer-expiry")) {
            compose
                .onNodeWithTag(tag)
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.EditableText,
                        AnnotatedString(""),
                    )
                )
        }
        compose
            .onNodeWithTag("card-transfer-note")
            .performScrollTo()
            .assertTextContains("یادداشت محلی DEMO", substring = true)
        click("card-transfer-submit")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.onNodeWithTag("S-093").assertDoesNotExist()
        back()
        click("card-transfer-attachment")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        click("card-transfer-otp")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        click("card-transfer-edit")
        compose.onNodeWithTag("S-175").assertIsDisplayed()
        compose
            .onNodeWithTag("card-transfer-destination")
            .assertTextContains("0000000000000002", substring = true)
        compose.onNodeWithTag("card-transfer-amount").performTextReplacement("300000")
        click("card-transfer-review")
        compose
            .onNodeWithTag("card-transfer-review-amount")
            .assertTextContains("300000", substring = true)
        compose
            .onNodeWithTag("card-transfer-note")
            .performScrollTo()
            .assertTextContains("یادداشت محلی DEMO", substring = true)
    }

    @Test
    fun anonymousPickerVariantAndUnsupportedControlsStayDistinct() {
        open("S-174")
        compose.onNodeWithTag("card-transfer-source").assertDoesNotExist()
        compose.onNodeWithTag("card-transfer-source-picker").performClick()
        compose
            .onNodeWithTag("card-transfer-source-picker")
            .assertTextContains("0000000000000001", substring = true)
        for (tag in
            listOf("card-transfer-scan", "card-transfer-favorites", "card-transfer-contacts")) {
            click(tag)
            compose.onNodeWithTag("unavailable").assertIsDisplayed()
            back()
            compose.onNodeWithTag("S-174").assertIsDisplayed()
        }
        click("card-transfer-cancel")
        compose.onNodeWithTag("S-026").assertIsDisplayed()
    }

    @Test
    fun internalSourceAndNonSecretDraftReturnFromReviewWithoutSubmission() {
        open("S-193")
        click("transfer-internal")
        click("account-transfer-continue")
        compose.onNodeWithTag("account-transfer-error").assertIsDisplayed()
        click("account-transfer-source")
        compose.onNodeWithTag("account-transfer-source-account-2").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("account-transfer-destination").performTextInput("0000000000002")
        compose.onNodeWithTag("account-transfer-amount").performTextInput("250000")
        click("account-transfer-reason")
        compose.onNodeWithTag("account-transfer-reason-0").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        click("account-transfer-extra")
        compose
            .onNodeWithTag("account-transfer-source-description")
            .performScrollTo()
            .performTextInput("شرح نمونه DEMO")
        compose.activityRule.scenario.recreate()
        click("account-transfer-continue")
        compose.onNodeWithTag("S-173").assertIsDisplayed()
        compose.onNodeWithText(AccountFixture.accounts[1].number).assertIsDisplayed()
        compose.onNodeWithText("شرح نمونه DEMO").assertExists()
        click("account-transfer-submit")
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.onNodeWithTag("S-098").assertDoesNotExist()
        back()
        back()
        compose.onNodeWithTag("S-192").assertIsDisplayed()
        compose
            .onNodeWithTag("account-transfer-source")
            .assertTextContains(AccountFixture.accounts[1].number, substring = true)
        compose
            .onNodeWithTag("account-transfer-source-description")
            .performScrollTo()
            .assertTextContains("شرح نمونه DEMO", substring = true)
        back()
        compose.onNodeWithTag("S-193").assertIsDisplayed()
    }

    @Test
    fun interbankRejectsBadChecksumAndRetainsItsOwnDraft() {
        open("S-193")
        click("transfer-interbank")
        compose
            .onNodeWithTag("account-transfer-destination")
            .performTextInput("IR000000000000000000000001")
        compose.onNodeWithTag("account-transfer-amount").performTextInput("250000")
        click("account-transfer-reason")
        compose.onNodeWithTag("account-transfer-reason-1").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        click("account-transfer-continue")
        compose.onNodeWithTag("account-transfer-error").assertIsDisplayed()
        compose
            .onNodeWithTag("account-transfer-destination")
            .performTextReplacement("IR220000000000000000000001")
        click("account-transfer-continue")
        compose.onNodeWithTag("S-173").assertIsDisplayed()
        compose.onNodeWithText("IR220000000000000000000001").assertIsDisplayed()
        back()
        back()
        click("transfer-internal")
        compose
            .onNodeWithTag("account-transfer-destination")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
            )
        back()
        click("transfer-interbank")
        compose
            .onNodeWithTag("account-transfer-destination")
            .assertTextContains("IR220000000000000000000001", substring = true)
    }

    @Test
    fun receiptsAreExplicitIndependentSamplesAndServiceActionsAreUnavailable() {
        for ((id, tag, action) in
            listOf(
                Triple("S-093", "card-transfer-receipt-synthetic", "card-transfer-share"),
                Triple("S-097", "account-transfer-receipt-disclosure", "account-transfer-share"),
                Triple("S-098", "account-transfer-receipt-disclosure", "account-transfer-share"),
            )) {
            open(id)
            compose.onNodeWithTag(tag).assertExists()
            click(action)
            compose.onNodeWithTag("unavailable").assertIsDisplayed()
            back()
            back()
            back()
        }
    }

    @Test
    fun localScenarioErrorRetriesWithoutSendingOrChangingTheDraft() {
        compose.onNodeWithTag("scenario-error").performClick()
        open("S-175")
        compose.onNodeWithTag("data-error").assertIsDisplayed()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.onNodeWithTag("card-transfer-source").assertIsDisplayed()
        back()
        back()
        compose.onNodeWithTag("scenario-loading").performScrollTo().performClick()
        open("S-193")
        compose.onNodeWithTag("data-loading").assertIsDisplayed()
        back()
        back()
        compose.onNodeWithTag("scenario-empty").performScrollTo().performClick()
        open("S-193")
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
    }

    @Test
    fun unresolvedHomeQuickTransfersDoNotInventAnIncomingRoute() {
        compose.onNodeWithTag("demo-entry").performClick()
        for (tag in listOf("menu-transfer", "menu-card-transfer")) {
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.onNodeWithTag("unavailable").assertIsDisplayed()
            compose.onNodeWithTag("S-193").assertDoesNotExist()
            compose.onNodeWithTag("S-175").assertDoesNotExist()
            back()
        }
    }

    @Test
    fun realKeyboardLeavesTheCardReviewControlReachable() {
        open("S-175")
        compose.onNodeWithTag("card-transfer-amount").performClick().performTextInput("250000")
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("card-transfer-review").performScrollTo().assertIsDisplayed()
        val metrics = compose.activity.resources.displayMetrics
        val keyboardHeight =
            checkNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView))
                .getInsets(WindowInsetsCompat.Type.ime())
                .bottom
        val keyboardTop = (metrics.heightPixels - keyboardHeight) / metrics.density
        org.junit.Assert.assertTrue(
            compose.onNodeWithTag("card-transfer-review").getUnclippedBoundsInRoot().bottom.value <=
                keyboardTop
        )
    }
}
