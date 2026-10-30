package org.ragbot.replica

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun enterCards() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-CARD").performClick()
    }

    // The first card sits on the right in RTL; the next one is reached by swiping right.
    private fun nextCard() {
        compose.onNodeWithTag("card-pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
    }

    private fun showBalance(card: String) {
        compose
            .onNode(hasText("مشاهده موجودی") and hasAnyAncestor(hasTestTag("card-$card")))
            .performClick()
    }

    private fun unavailable(title: String) {
        compose.onNodeWithTag("unavailable").assertTextContains(title, substring = true)
        back()
        compose.onNodeWithTag("unavailable").assertDoesNotExist()
    }

    @Test
    fun cardsTabIsHostedHomeContentWithDistinctBalanceStates() {
        enterCards()
        compose.onNodeWithTag("control-CARD").assertIsSelected()
        compose.onNodeWithTag("control-ACCOUNT").assertIsNotSelected()
        compose.onNodeWithTag("S-026").assertExists()
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("account-tab").assertDoesNotExist()
        compose.onNodeWithTag("card-kind-DEBIT").assertIsSelected()
        compose.onNodeWithText("۰۰۰۰ ۰۰۰۰ ۰۰۰۰ ۰۰۰۱").assertIsDisplayed()
        compose.onNodeWithContentDescription("کارت پیش‌فرض").assertIsDisplayed()
        showBalance("card-1")
        compose.onNodeWithText("۱۲٬۵۰۰٬۰۰۰ ریال").assertIsDisplayed()
        compose.onNodeWithContentDescription("عدم نمایش موجودی").performClick()
        compose.onNodeWithText("۱۲٬۵۰۰٬۰۰۰ ریال").assertDoesNotExist()
        nextCard()
        compose.onNodeWithTag("card-card-2").assertIsDisplayed()
        showBalance("card-2")
        compose.onNodeWithText("۰ ریال").assertIsDisplayed()
        nextCard()
        compose.onNodeWithText("۰۰۰۰ ۰۰** **** ۰۰۰۳").assertIsDisplayed()
        showBalance("card-3")
        compose.onNodeWithText("تلاش مجدد").performClick()
        unavailable("دریافت موجودی")
        compose.onNodeWithTag("card-copy").performClick()
        compose.onNodeWithText("با موفقیت کپی شد.").assertIsDisplayed()
        compose.onNodeWithText("بستن").performClick()
        compose.onNodeWithContentDescription("اشتراک‌گذاری کارت").performClick()
        unavailable("اشتراک‌گذاری کارت")
        compose.onNodeWithTag("card-card-3").assertIsDisplayed()
    }

    @Test
    fun groupSelectionCardPositionAndBalancesSurviveBackRecreationAndTabSwitches() {
        enterCards()
        nextCard()
        showBalance("card-2")
        compose.onNodeWithTag("card-kind-CREDIT").performClick()
        compose.onNodeWithTag("card-card-4").assertIsDisplayed()
        compose.onNodeWithTag("card-balance").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("card-kind-CREDIT").assertIsSelected()
        compose.onNodeWithTag("card-kind-DEBIT").performClick()
        compose.onNodeWithTag("card-card-2").assertIsDisplayed()
        compose.onNodeWithText("۰ ریال").assertIsDisplayed()
        compose.onNodeWithTag("card-action-info").performClick()
        compose.onNodeWithTag("S-012").assertIsDisplayed()
        compose.onNodeWithTag("pick-card-2").performClick()
        compose.onNodeWithText(CardFixture.cards[1].iban).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("pick-card-2").assertIsSelected()
        back()
        compose.onNodeWithTag("card-card-2").assertIsDisplayed()
        compose.onNodeWithTag("control-ACCOUNT").performClick()
        compose.onNodeWithTag("account-tab").assertIsDisplayed()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("card-card-2").assertIsDisplayed()
        back()
        compose.onNodeWithTag("control-HOME").assertIsSelected()
        compose.onNodeWithTag("home-scroll").assertIsDisplayed()
    }

    @Test
    fun creditDetailAndFileLoanUseTheFirstCreditCardAndKeepPaymentUnavailable() {
        enterCards()
        compose.onNodeWithTag("card-kind-CREDIT").performClick()
        nextCard()
        compose.onNodeWithTag("card-card-5").assertIsDisplayed()
        compose.onNodeWithTag("card-action-credit").performClick()
        compose.onNodeWithTag("S-131").assertIsDisplayed()
        compose.onNodeWithText("۰۰۰۰ ۰۰۰۰ ۰۰۰۰ ۰۰۰۴").assertIsDisplayed()
        compose.onNodeWithText("۵۰۰٬۰۰۰٬۰۰۰ ریال").assertIsDisplayed()
        compose.onNodeWithText("۳۲۰٬۰۰۰٬۰۰۰ ریال").assertIsDisplayed()
        compose.onNodeWithTag("credit-pay").performScrollTo().performClick()
        unavailable("پرداخت")
        back()
        compose.onNodeWithTag("card-card-5").assertIsDisplayed()
        compose.onNodeWithTag("card-action-loan").performClick()
        compose.onNodeWithTag("S-134").assertIsDisplayed()
        compose.onNodeWithText("۱۵۰٬۰۰۰٬۰۰۰ ریال").assertIsDisplayed()
        compose.onNodeWithTag("loan-۰۰۰۱").performClick()
        unavailable("پرونده تسهیلات")
        back()
        compose.onNodeWithTag("card-action-history").performClick()
        compose.onNodeWithTag("S-091").assertIsDisplayed()
        back()
        compose.onNodeWithTag("card-kind-CREDIT").assertIsSelected()
    }

    @Test
    fun cardMutationsAndSecureInquiriesStayUnavailableOnTheCurrentPage() {
        enterCards()
        compose.onNodeWithContentDescription("اشتراک‌گذاری کارت").performClick()
        unavailable("اشتراک‌گذاری کارت")
        compose.onNodeWithTag("card-tab").assertIsDisplayed()
        compose.onNodeWithTag("card-kind-BON").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("card-action-bon").performScrollTo().performClick()
        compose.onNodeWithTag("S-119").assertIsDisplayed()
        compose.onNodeWithTag("secure-ok").performClick()
        compose.onNodeWithText("وارد کردن CVV2 الزامی است.").assertIsDisplayed()
        compose.onNodeWithTag("secure-cvv2").performTextReplacement("1234")
        compose.onNodeWithTag("secure-expiry").performTextReplacement("05/08")
        compose.onNodeWithTag("secure-password").performTextReplacement("12345")
        compose.onNodeWithTag("secure-ok").performClick()
        unavailable("استعلام امن")
        compose.onNodeWithText("ریال", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("card-kind-GIFT").assertDoesNotExist()
        back()
        compose.onNodeWithTag("card-kind-GIFT").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("card-empty").assertIsDisplayed()
        // The legacy list search runs last because its soft keyboard stays open.
        compose.onNodeWithTag("card-kind-DEBIT").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("card-action-list").performScrollTo().performClick()
        compose.onNodeWithTag("S-125").assertIsDisplayed()
        compose.onNodeWithTag("row-card-1").assertHasNoClickAction()
        compose.onNodeWithText("۳٬۷۵۰٬۰۰۰ ریال").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("card-search").performTextReplacement("0002")
        compose.onNodeWithTag("row-card-1").assertDoesNotExist()
        compose.onNodeWithTag("row-card-3").assertDoesNotExist()
    }

    @Test
    fun localEmptyLoadingAndErrorScenariosAndRetry() {
        listOf("empty", "loading", "error").forEach { scenario ->
            compose.onNodeWithTag("scenario-$scenario").performClick()
            enterCards()
            if (scenario == "empty") {
                compose.onNodeWithTag("card-empty").assertIsDisplayed()
                compose.onNodeWithTag("card-pager").assertDoesNotExist()
            } else compose.onNodeWithTag("data-$scenario").assertIsDisplayed()
            if (scenario == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("card-pager").assertIsDisplayed()
            }
            back()
            back()
        }
    }

    @Test
    fun labelledReviewEntriesOpenManagementVisibilityAndAnonymousBalance() {
        compose.onNodeWithTag("demo-S-126").performClick()
        compose.onNodeWithTag("S-126").assertIsDisplayed()
        compose.onNodeWithTag("manage-search").performTextReplacement("0003")
        compose.onNodeWithTag("row-card-3").assertTextContains("قفل شده", substring = true)
        compose.onNodeWithText("حذف").performClick()
        unavailable("حذف کارت")
        compose.onNodeWithTag("fetch-cards").performScrollTo().performClick()
        compose.onNodeWithTag("S-068").assertIsDisplayed()
        compose.onNodeWithTag("fetch-ok").assertIsNotEnabled()
        compose.onNodeWithTag("candidate-card-7").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("candidate-card-7").assertIsOn()
        compose.onNodeWithTag("fetch-ok").performClick()
        unavailable("افزودن کارت")
        compose.onNodeWithTag("candidate-card-7").assertIsOn()
        back()
        compose.onNodeWithTag("manage-search").assertTextContains("0003")
        back()
        back()
        compose.onNodeWithTag("demo-S-130").performClick()
        compose.onNodeWithTag("card-visible").performClick()
        unavailable("وضعیت نمایش کارت")
        compose.onNodeWithTag("card-visible").assertIsOn()
        back()
        back()
        compose.onNodeWithTag("demo-S-120").performClick()
        compose.onNodeWithTag("S-120").assertIsDisplayed()
        compose.onNodeWithTag("secure-number").performTextReplacement("1234")
        compose.onNodeWithTag("secure-ok").performClick()
        compose.onNodeWithText("شماره کارت باید ۱۶ رقم باشد.").assertIsDisplayed()
    }
}
