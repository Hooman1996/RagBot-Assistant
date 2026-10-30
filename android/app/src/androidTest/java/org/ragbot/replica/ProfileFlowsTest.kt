package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileFlowsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun openProfile() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("control-PROFILE").performClick()
    }

    private fun pick(key: String, option: String) {
        compose.onNodeWithTag("pick-$key").performScrollTo().performClick()
        compose.onNodeWithTag("option-$option").performClick()
    }

    @Test
    fun profileTabIsHostedAndUnsupportedRowsStayOnTheTab() {
        openProfile()
        compose.onNodeWithTag("control-PROFILE").assertIsSelected()
        compose.onNodeWithTag("profile-tab").assertIsDisplayed()
        compose.onNodeWithText("آخرین ورود", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("profile-limits").performScrollTo().performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        compose.onNodeWithTag("profile-tab").assertIsDisplayed()
        compose.onNodeWithTag("profile-versions").performScrollTo().performClick()
        compose.onNodeWithTag("S-074").assertIsDisplayed()
        compose.onNodeWithTag("version-۵.۱.۰").assertIsDisplayed()
        back()
        compose.onNodeWithTag("profile-invite").performScrollTo().performClick()
        compose.onNodeWithTag("invite-copy").performClick()
        compose.onNodeWithText("با موفقیت کپی شد.").assertIsDisplayed()
    }

    @Test
    fun addressesAddValidateDeleteAndReachTheCardRequestFlow() {
        openProfile()
        compose.onNodeWithTag("profile-addresses").performScrollTo().performClick()
        compose.onNodeWithTag("S-464").assertIsDisplayed()
        compose.onNodeWithTag("saved-address-1").assertIsDisplayed()
        compose.onNodeWithTag("address-add").performScrollTo().performClick()
        compose.onNodeWithTag("S-463").assertIsDisplayed()
        compose.onNodeWithTag("field-title").performTextReplacement("دفتر")
        compose.onNodeWithTag("field-postal").performTextReplacement("123")
        compose.onNodeWithTag("address-save").performScrollTo().performClick()
        compose.onNodeWithText("کد پستی 10 رقمی را وارد کنید").assertExists()
        compose.onNodeWithText("لطفا استان مورد نظر را انتخاب کنید.").assertExists()
        pick("addressProvince", "اصفهان")
        pick("addressCity", "کاشان")
        compose.onNodeWithTag("field-postal").performTextReplacement("1234567890")
        compose.onNodeWithTag("field-address").performTextReplacement("خیابان نمونه")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("pick-addressCity").assertTextContains("کاشان")
        compose.onNodeWithTag("address-save").performScrollTo().performClick()
        compose.onNodeWithTag("saved-address-local-1234567890").assertIsDisplayed()
        compose.onNodeWithTag("delete-address-1").performClick()
        compose.onNodeWithTag("delete-ok").performClick()
        compose.onNodeWithTag("saved-address-1").assertDoesNotExist()
        back()
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("card-action-issue").performScrollTo().performClick()
        compose.onNodeWithTag("delivery-post").performClick()
        compose.onNodeWithTag("first-ok").performScrollTo().performClick()
        compose.onNodeWithTag("address-address-local-1234567890").assertIsDisplayed()
        compose.onNodeWithTag("address-address-1").assertDoesNotExist()
    }

    @Test
    fun additionalInformationValidatesAndPersistsLocally() {
        openProfile()
        compose.onNodeWithTag("profile-additional").performScrollTo().performClick()
        compose.onNodeWithTag("S-064").assertIsDisplayed()
        compose.onNodeWithTag("field-firstNameEn").performTextReplacement("علی")
        compose.onNodeWithTag("additional-ok").performScrollTo().performClick()
        compose.onAllNodesWithText("لطفا با حروف انگلیسی وارد کنید.").assertCountEquals(3)
        compose.onNodeWithTag("field-firstNameEn").performTextReplacement("Preview")
        compose.onNodeWithTag("field-lastNameEn").performTextReplacement("User")
        compose.onNodeWithTag("field-fathersName").performTextReplacement("Sample")
        compose.onNodeWithTag("field-issueDate").performTextReplacement("1400/01/01")
        pick("province", "تهران")
        pick("city", "ری")
        pick("marital", "مجرد")
        pick("education", "کارشناسی")
        pick("job", "کارمند")
        pick("salary", "کمتر از ۱۰۰ میلیون ریال")
        compose.onNodeWithTag("additional-ok").performScrollTo().performClick()
        compose.onNodeWithTag("additional-done").performClick()
        compose.onNodeWithTag("profile-additional").performScrollTo().performClick()
        compose.onNodeWithTag("field-firstNameEn").assertTextContains("Preview")
        compose.onNodeWithTag("pick-city").assertTextContains("ری")
    }

    @Test
    fun credentialChangeUsesOtpAndSettingsToggleLocally() {
        openProfile()
        compose.onNodeWithTag("profile-edit").performClick()
        compose.onNodeWithTag("S-065").assertIsDisplayed()
        compose.onNodeWithTag("profile-photo").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        compose.onNodeWithTag("edit-username").performClick()
        compose.onNodeWithTag("field-username").performTextReplacement(".bad")
        compose.onNodeWithTag("edit-ok").performClick()
        compose.onNodeWithText("نام کاربری معتبر نیست.").assertIsDisplayed()
        compose.onNodeWithTag("field-username").performTextReplacement("preview.user")
        compose.onNodeWithTag("edit-ok").performClick()
        compose.onNodeWithTag("otp-code").performTextReplacement("12345")
        compose.onNodeWithTag("otp-ok").performClick()
        compose.onNodeWithTag("security-result").assertTextContains("هیچ تغییری", substring = true)
        compose.onNodeWithTag("security-done").performClick()
        compose.onNodeWithTag("profile-settings").performScrollTo().performClick()
        compose.onNodeWithTag("S-025").assertIsDisplayed()
        compose.onNodeWithTag("settings-services").performClick()
        compose.onNodeWithTag("facility-انتقال وجه").assertIsOn().performClick()
        compose.onNodeWithTag("facility-ok").assertIsNotEnabled()
        compose.onNodeWithTag("facility-password").performTextReplacement("x")
        compose.onNodeWithTag("facility-ok").performClick()
        compose.onNodeWithTag("facility-انتقال وجه").assertIsOff()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("facility-انتقال وجه").assertIsOff()
        back()
        compose.onNodeWithTag("settings-biometric").performClick()
        compose.onNodeWithTag("biometric").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
    }

    @Test
    fun notificationReviewEntryAsksBeforeTurningOff() {
        compose.onNodeWithTag("demo-S-071").performClick()
        compose.onNodeWithTag("S-071").assertIsDisplayed()
        compose.onNodeWithTag("notify-اعلان پیشنهادها").assertIsOn().performClick()
        compose.onNodeWithTag("notify-off").performClick()
        compose.onNodeWithTag("notify-اعلان پیشنهادها").assertIsOff().performClick()
        compose.onNodeWithTag("notify-اعلان پیشنهادها").assertIsOn()
    }
}
