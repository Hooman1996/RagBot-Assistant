package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The services built on the form engine, end to end against the mock bank. */
@RunWith(AndroidJUnit4::class)
class ServicesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun click(tag: String) {
        compose.waitForIdle()
        val node = compose.onNodeWithTag(tag)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
            // Not inside a scrolling parent.
        }
        node.performClick()
    }

    private fun shown(id: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(id).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun fill(field: String, value: String) {
        compose.onNodeWithTag("svc-$field").performScrollTo().performTextReplacement(value)
    }

    private fun next() = click("svc-continue")

    private fun confirm() {
        click("svc-confirm")
        compose.onNodeWithTag("otp-code").performTextReplacement("12345")
        compose.onNodeWithTag("otp-ok").performClick()
    }

    private fun failure(key: String) {
        compose
            .onNodeWithTag("bank-failure")
            .assertTextContains(MockData.current.messages.getValue(key))
        compose.onNodeWithTag("bank-failure-close").performClick()
    }

    private fun chequeMenu(key: String) {
        click("demo-entry")
        click("menu-cheque")
        click("cheque-menu-$key")
    }

    @Test
    fun registeringASayadChequeEndsInAReceiptAndTheInquiryFindsIt() {
        chequeMenu("AUTHENTICATED_HOME_CHEQUE_PICHAK_ISSUE_PAPER_CHEQUE")
        shown("S-165")
        next()
        compose.onNodeWithText(PaymentRules.MANDATORY, substring = true).assertExists()
        fill("sayad", "0000000000000900")
        fill("amount", "7000000")
        fill("due", "۱۴۰۵/۱۰/۰۱")
        next()
        shown("S-171")
        fill("receiver", "رضا")
        fill("code", "0000000019")
        next()
        shown("S-170")
        confirm()
        shown("S-168")
        compose.onNodeWithTag("bank-receipt").assertExists()
        click("bank-receipt-done")
        click("menu-cheque")
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_INQUIRY_BYHOLDER")
        shown("S-164")
        click("svc-kind-holder")
        next()
        fill("sayad", "0000000000000900")
        click("svc-confirm")
        shown("S-166")
        compose.onNodeWithText("ثبت شده").assertExists()
    }

    @Test
    fun anUnknownChequeFailsTheInquiry() {
        chequeMenu("AUTHENTICATED_HOME_CHEQUE_INQUIRY_BYHOLDER")
        click("svc-kind-issuer")
        next()
        fill("sayad", "0000000000000999")
        click("svc-confirm")
        failure("sayad-not-found")
        shown("S-162")
    }

    @Test
    fun aReceivedChequeIsConfirmedThenTransferredAndLeavesThePendingList() {
        chequeMenu("DESIGNED_SAYAD_ANSWER")
        shown("S-167")
        click("svc-sayad-0000000000000311")
        next()
        shown("S-163")
        click("svc-answer-accept")
        next()
        shown("S-170")
        confirm()
        shown("S-168")
        click("bank-receipt-done")
        click("menu-cheque")
        click("cheque-menu-DESIGNED_SAYAD_ANSWER")
        compose.onNodeWithTag("svc-sayad-0000000000000311").assertDoesNotExist()
        click("page-back")
        click("cheque-menu-DESIGNED_SAYAD_TRANSFER")
        shown("S-172")
        click("svc-sayad-0000000000000311")
        next()
        fill("receiver", "رضا")
        fill("code", "0000000019")
        next()
        confirm()
        shown("S-168")
    }

    @Test
    fun theChequeHistoryListsEveryCheque() {
        chequeMenu("DESIGNED_SAYAD_HISTORY")
        shown("S-169")
        compose.onNodeWithText("0000000000000321", substring = true).assertExists()
    }

    private fun vehicleMenu(key: String) {
        click("demo-entry")
        click("menu-vehicle")
        click("vehicle-menu-AUTHENTICATED_HOME_$key")
    }

    @Test
    fun violationsArePaidOnceThroughTheUsersConfirm() {
        vehicleMenu("RAHVAR_TOLL")
        click("vehicle-plate-1")
        shown("S-244")
        next()
        shown("S-245")
        compose.onNodeWithText("سرعت غیرمجاز", substring = true).assertExists()
        next()
        shown("S-243")
        click("svc-source-account-1")
        confirm()
        shown("S-261")
        compose.onNodeWithTag("bank-receipt").assertExists()
        click("bank-receipt-done")
        click("menu-vehicle")
        click("vehicle-menu-AUTHENTICATED_HOME_RAHVAR_TOLL")
        click("vehicle-plate-1")
        next()
        compose.onNodeWithText("سرعت غیرمجاز", substring = true).assertDoesNotExist()
        next()
        click("svc-source-account-1")
        confirm()
        failure("nothing-to-pay")
    }

    @Test
    fun transferTaxIsPaidFromTheChosenAccount() {
        vehicleMenu("TRANSFER_TOLL")
        click("vehicle-plate-1")
        shown("S-278")
        next()
        shown("S-279")
        next()
        shown("S-277")
        click("svc-source-account-2")
        confirm()
        shown("S-261")
    }

    @Test
    fun theVehicleInquiriesShowTheirRecords() {
        vehicleMenu("VEHICLE_DOC_INQUIRY")
        click("vehicle-plate-1")
        next()
        shown("S-281")
        compose.onNodeWithText("کارت خودرو").assertExists()
        repeat(4) { click("page-back") }
        click("menu-vehicle")
        click("vehicle-menu-AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY")
        click("vehicle-contact-1")
        next()
        shown("S-239")
        compose.onNodeWithText("12 ب 345 ایران 67").assertExists()
        repeat(4) { click("page-back") }
        click("menu-vehicle")
        click("vehicle-menu-AUTHENTICATED_HOME_DRIVING_LICENSE_INQUIRY")
        click("vehicle-contact-1")
        shown("S-247")
        compose.onNodeWithText("معتبر").assertExists()
        repeat(3) { click("page-back") }
        click("menu-vehicle")
        click("vehicle-menu-AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY")
        click("vehicle-licence-1")
        shown("S-255")
        compose.onNodeWithText("۱۲").assertExists()
    }

    private fun inquiryMenu(key: String) {
        click("demo-entry")
        click("menu-inquiry")
        shown("S-443")
        click("svc-menu-$key")
        shown("S-442")
    }

    @Test
    fun aCreditReportTakesTheFeeThenShowsTheReportAndItsDetail() {
        inquiryMenu("credit-report")
        click("svc-source-account-1")
        confirm()
        shown("S-445")
        compose.onNodeWithText("۶۱۲").assertExists()
        assert(MockBank.balance("account-1") == 12_250_000L)
        click("svc-detail")
        shown("S-441")
        compose.onNodeWithText("ندارد").assertExists()
    }

    @Test
    fun theFeeCanComeFromAnotherAccountAndAMissingSourceIsRefused() {
        inquiryMenu("bounced-cheques")
        click("svc-confirm")
        compose.onNodeWithTag("otp-code").performTextReplacement("12345")
        compose.onNodeWithTag("otp-ok").performClick()
        failure("incomplete")
        click("svc-source-account-2")
        confirm()
        shown("S-444")
        assert(MockBank.balance("account-2") == 3_500_000L)
        click("svc-detail")
        shown("S-440")
    }

    @Test
    fun theCreditRateIsAPaidInquiry() {
        inquiryMenu("credit-rate")
        click("svc-source-account-1")
        confirm()
        shown("S-453")
        compose.onNodeWithText("خوب").assertExists()
    }

    private fun loanApplication(amount: String) {
        click("demo-entry")
        click("menu-loan")
        click("loan-request")
        shown("S-287")
        click("svc-product-personal")
        next()
        shown("S-308")
        fill("amount", amount)
        fill("months", "12")
        compose.onNodeWithText("قسط ماهانه (ریال)").assertExists()
        next()
        shown("S-286")
        fill("job", "کارمند")
        next()
        shown("S-335")
        fill("guarantor", "رضا")
        fill("guarantorCode", "0000000019")
        next()
        shown("S-285")
        click("svc-agree-yes")
        next()
        shown("S-334")
        click("svc-note-issue")
        next()
        shown("S-378")
        click("svc-sign-sign")
        next()
        shown("S-336")
        confirm()
    }

    @Test
    fun aLoanRequestWalksEveryStepToItsMockResult() {
        loanApplication("120000000")
        shown("S-337")
        compose.onNodeWithText("تایید اولیه").assertExists()
        compose.onNodeWithText(persianNumber(11_800_000L)).assertExists()
    }

    @Test
    fun aLoanOverTheProductLimitIsRefused() {
        loanApplication("900000000")
        failure("loan-over-limit")
        shown("S-336")
    }

    private fun login(password: String) {
        compose.onNodeWithTag("login-username").performTextReplacement("demo.user")
        compose.onNodeWithTag("login-password").performTextReplacement(password)
        click("login-submit")
    }

    @Test
    fun theDemoUserLogsInThroughTheWelcomePagesAndCanLogOut() {
        click("login-start")
        shown("S-048")
        click("intro-next")
        shown("S-049")
        click("welcome-login")
        shown("S-027")
        login("wrong")
        compose.onNodeWithTag("login-error").assertTextEquals(LoginRules.WRONG)
        login(MockData.current.customer.password)
        shown("S-026")
        click("header-LOGOUT")
        shown("S-027")
        compose.onNodeWithTag("login-password").assertTextEquals("رمز عبور", "")
        login(MockData.current.customer.password)
        shown("S-026")
    }

    @Test
    fun backingOutOfLoginReturnsToTheFrontPageNotHome() {
        click("login-start")
        shown("S-048")
        click("page-back")
        shown("demo-entry")
        compose.onNodeWithTag("S-026").assertDoesNotExist()
    }

    @Test
    fun aLoginPasswordIsGoneAfterRecreation() {
        click("login-start")
        click("intro-next")
        click("welcome-login")
        compose.onNodeWithTag("login-password").performTextReplacement("Demo@1234")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("login-password").assertTextEquals("رمز عبور", "")
    }

    @Test
    fun theDashboardTotalAgreesWithItsCategoriesAndABudgetCanChange() {
        click("demo-entry")
        click("menu-performance")
        shown("S-436")
        val total = FinanceRules.spending().sumOf { it.amount }
        assert(total == 5_735_000L)
        compose
            .onNodeWithTag("finance-total")
            .assertTextContains(persianNumber(total), substring = true)
        click("finance-budgets")
        shown("S-434")
        click("budget-food")
        shown("S-435")
        compose.onNodeWithTag("budget-limit").performScrollTo().performTextReplacement("abc")
        click("budget-save")
        compose.onNodeWithText(PaymentRules.INVALID, substring = true).assertExists()
        compose.onNodeWithTag("budget-limit").performTextReplacement("1000000")
        compose.onNodeWithTag("budget-over").assertExists()
        click("budget-save")
        shown("S-434")
        assert(MockBank.budget("food") == 1_000_000L)
    }

    @Test
    fun aPaidBillAppearsInItsCategory() {
        MockBank.clock = { "۱۴۰۵/۰۸/۱۴ - ۱۰:۰۰" }
        val before = FinanceRules.spending().first { it.category == "bills" }.amount
        MockBank.bill("account-1", "9000000000001", "1250001")
        val after = FinanceRules.spending().first { it.category == "bills" }.amount
        assert(after == before + 1_250_000L)
    }

    @Test
    fun anAccountOpeningApplicationWalksEveryStep() {
        click("demo-entry")
        click("menu-open-account")
        shown("S-062")
        next()
        compose.onNodeWithText(PaymentRules.MANDATORY, substring = true).assertExists()
        click("svc-kind-saving")
        next()
        shown("S-054")
        fill("mobile", "0901")
        fill("email", "demo@example.test")
        next()
        compose.onNodeWithText(PaymentRules.INVALID, substring = true).assertExists()
        fill("mobile", "09010000000")
        next()
        shown("S-053")
        fill("job", "کارمند")
        next()
        shown("S-061")
        click("svc-document-card")
        next()
        shown("S-052")
        click("svc-branch-north")
        next()
        shown("S-058")
        click("svc-agree-yes")
        next()
        shown("S-057")
        compose.onNodeWithText("شعبه نمایشی شمال").assertExists()
        confirm()
        shown("S-059")
        compose.onNodeWithText("درخواست ثبت شد").assertExists()
    }

    @Test
    fun aWageAdvanceCreditsTheChosenAccountWithinItsLimit() {
        click("demo-entry")
        click("menu-wage")
        shown("S-414")
        next()
        shown("S-419")
        fill("amount", "2000000")
        click("svc-target-account-1")
        next()
        shown("S-417")
        click("svc-agree-yes")
        next()
        shown("S-418")
        confirm()
        shown("S-183")
        assert(MockBank.balance("account-1") == 14_500_000L)
        assert(MockBank.wageAvailable == 48_000_000L)
    }

    @Test
    fun theCreditProductActivatesThroughItsGuide() {
        click("demo-entry")
        click("menu-loan")
        click("loan-credit-product")
        shown("S-407")
        next()
        shown("S-406")
        next()
        shown("S-408")
        click("svc-agree-yes")
        confirm()
        shown("S-409")
        compose.onNodeWithText("فعال شد").assertExists()
    }

    @Test
    fun rewardPointsMoveWithoutTouchingMoneyAndCannotOverdraw() {
        click("demo-entry")
        click("menu-loyalty")
        shown("S-432")
        click("svc-menu-rewards-transfer")
        shown("S-428")
        fill("mobile", "09010000000")
        fill("points", "5000")
        next()
        shown("S-427")
        confirm()
        failure("points-not-enough")
        click("page-back")
        fill("points", "200")
        next()
        confirm()
        shown("S-431")
        assert(MockBank.points == 1_000L)
        assert(MockBank.balance("account-1") == 12_500_000L)
        click("bank-receipt-done")
        click("menu-loyalty")
        click("svc-menu-rewards-history")
        shown("S-426")
        compose.onNodeWithText(persianNumber(1_000L)).assertExists()
    }
}
