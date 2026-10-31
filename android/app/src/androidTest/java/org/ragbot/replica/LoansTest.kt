package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoansTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun shown(id: String) {
        compose.onNodeWithTag(id).assertIsDisplayed()
    }

    private fun source(id: String) {
        if (id == "account-3") compose.onNodeWithTag("loan-source-shared-true").performClick()
        compose.onNodeWithTag("loan-source-$id").performClick()
        compose.onNodeWithTag("loan-source-confirm").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        shown("S-331")
    }

    @Test
    fun installmentReviewKeepsSelectionsAndAllowsCorrectionWithoutPayment() {
        click("demo-S-324")
        click("loan-current")
        shown("S-338")
        click("loan-loan-personal")
        shown("S-306")
        compose.onNodeWithTag("loan-installment-0").assertIsNotEnabled()
        click("loan-review")
        compose.onNodeWithTag("loan-error").assertTextEquals(LoanFixture.SELECT_ERROR)
        click("loan-installment-2")
        compose.onNodeWithTag("loan-error").assertTextEquals(LoanFixture.ORDER_ERROR)
        click("loan-installment-1")
        click("loan-installment-2")
        click("loan-installment-1")
        compose.onNodeWithTag("loan-error").assertTextEquals(LoanFixture.ORDER_ERROR)
        compose
            .onNodeWithTag("loan-selected-sum")
            .assertTextContains("۱۰٬۰۰۰٬۰۰۰", substring = true)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("loan-installment-1").assertIsSelected()
        click("loan-review")
        compose.onNodeWithTag("loan-source-confirm").performClick()
        compose.onNodeWithTag("loan-source-error").assertTextEquals(LoanFixture.SOURCE_ERROR)
        compose.onNodeWithTag("loan-payment-id-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("loan-payment-id").performTextInput("12345678901234567")
        compose
            .onNodeWithTag("loan-payment-id")
            .assertTextContains("12345678901234567", substring = true)
        source("account-2")
        compose.onNodeWithText("12345678901234567").assertExists()
        compose.onNodeWithTag("loan-payable").assertTextEquals("مبلغ قابل پرداخت: ۹٬۷۵۰٬۰۰۰ ریال")
        compose.activityRule.scenario.recreate()
        click("payment-submit")
        compose.onNodeWithTag("unavailable").assertTextContains("پرداخت تسهیلات", substring = true)
        back()
        shown("S-331")
        click("loan-correct")
        shown("S-306")
        compose.onNodeWithTag("loan-installment-2").assertIsSelected()
        click("loan-installment-2")
        click("loan-review")
        source("account-1")
        compose.onNodeWithTag("loan-payable").assertTextEquals("مبلغ قابل پرداخت: ۵٬۰۰۰٬۰۰۰ ریال")
        back()
        back()
        shown("S-338")
        click("loan-category-true")
        click("loan-loan-corporate")
        shown("S-306")
        back()
        compose.onNodeWithTag("loan-category-true").assertIsSelected()
    }

    @Test
    fun overdueDebtUsesRemainingAmountsAndPaidLoanHasNoReview() {
        click("demo-S-338")
        click("loan-loan-overdue")
        click("loan-installment-2")
        compose.onNodeWithTag("loan-error").assertTextEquals(LoanFixture.OVERDUE_ERROR)
        click("loan-installment-1")
        compose.onNodeWithTag("loan-error").assertTextEquals(LoanFixture.ORDER_ERROR)
        click("loan-installment-0")
        click("loan-installment-1")
        click("loan-review")
        source("account-1")
        compose.onNodeWithTag("loan-payable").assertTextEquals("مبلغ قابل پرداخت: ۸٬۱۵۰٬۰۰۰ ریال")
        back()
        back()
        click("loan-loan-paid")
        compose.onNodeWithTag("loan-installment-0").assertIsNotEnabled()
        compose.onNodeWithTag("loan-review").assertDoesNotExist()
    }

    @Test
    fun noLoansLoadingAndErrorAreLocalAndRetryReturnsMultipleLoans() {
        click("scenario-empty")
        click("demo-S-338")
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
        back()
        back()
        click("scenario-loading")
        click("demo-S-338")
        compose.onNodeWithTag("data-loading").assertIsDisplayed()
        back()
        back()
        click("scenario-error")
        click("demo-S-338")
        compose.onNodeWithTag("data-error").assertIsDisplayed()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.onNodeWithTag("loan-loan-personal").assertExists()
        compose.onNodeWithTag("loan-loan-overdue").assertExists()
    }

    @Test
    fun sourcePickerBackAndRecreationPreserveOnlyTheLocalDraft() {
        click("demo-S-338")
        click("loan-loan-personal")
        click("loan-installment-1")
        click("loan-review")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("انتخاب حساب مبدا").assertIsDisplayed()
        back()
        shown("S-306")
        compose.onNodeWithTag("loan-installment-1").assertIsSelected()
        click("loan-review")
        source("account-3")
        compose.onNodeWithTag("loan-payable").assertTextContains("۵٬۰۰۰٬۰۰۰", substring = true)
        compose.onNodeWithTag("security-otp").assertDoesNotExist()
    }
}
