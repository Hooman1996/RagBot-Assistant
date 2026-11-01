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
class DigitalChequesTest {
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

    private fun menu(key: String) {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-cheque").performScrollTo().performClick()
        click("cheque-menu-AUTHENTICATED_HOME_CHEQUE_$key")
    }

    @Test
    fun activationStopsBeforeTheExternalAppInBothStates() {
        menu("CHAKAD_ACTIVATION")
        shown("S-157")
        compose.onNodeWithTag("digital-active").assertDoesNotExist()
        click("digital-activate")
        compose.onNodeWithText("هنگام دانلود اپلیکیشن هامون، به نماد زیر توجه کنید.").assertExists()
        compose.onNodeWithText("انصراف").performClick()
        compose.onNodeWithTag("digital-confirm-sheet").assertDoesNotExist()
        click("digital-activate")
        compose.onNodeWithTag("digital-confirm").performClick()
        unavailable("امضا با برنامهٔ هامون")
        back()
        back()
        back()
        click("demo-digital-active")
        shown("S-157")
        compose.onNodeWithTag("digital-active").assertExists()
        click("digital-deactivate")
        compose.onNodeWithText("آیا مایل به غیرفعال‌سازی خدمت هستید؟").assertExists()
        compose.onNodeWithTag("digital-confirm").performClick()
        unavailable("غیرفعال‌سازی خدمت")
        click("digital-update")
        compose.onNodeWithTag("digital-confirm").performClick()
        unavailable("امضا با برنامهٔ هامون")
    }

    @Test
    fun cartableSearchDetailAndFixedTransferReachTheConfirmationWithoutSending() {
        menu("CHAKAD_CARTABLE_DIGITAL_CHEQUE")
        shown("S-159")
        compose.onNodeWithTag("digital-search").performTextReplacement("999")
        compose.onNodeWithTag("digital-empty").assertExists()
        compose.onNodeWithTag("digital-search").performTextReplacement("202")
        click("digital-0000000000000202")
        shown("S-158")
        compose.onNodeWithText("در انتظار پذیرش").assertExists()
        click("digital-transfer")
        shown("S-151")
        compose.onNodeWithTag("digital-sayad").assertIsNotEnabled()
        click("digital-bounce")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("digital-bounce").assertIsOn()
        click("digital-continue")
        shown("S-152")
        compose.onNodeWithText("دارندهٔ نمایشی").assertExists()
        compose.onNodeWithText("بله").assertExists()
        compose.onNodeWithText("۵۰٬۰۰۰ ریال").assertExists()
        click("payment-submit")
        unavailable("واگذاری چک دیجیتال")
        click("payment-cancel")
        shown("S-151")
        back()
        shown("S-158")
    }

    @Test
    fun menuTransferRequiresAPickedSayadId() {
        menu("FACILITIES_TRANSFER_DIGITAL_CHEQUE")
        shown("S-151")
        click("digital-continue")
        compose.onNodeWithTag("digital-sayad-error").assertExists()
        click("digital-sayad")
        compose.onNodeWithTag("digital-sayad-search").performTextReplacement("999")
        compose.onNodeWithTag("digital-sayad-none").assertExists()
        compose.onNodeWithTag("digital-sayad-search").performTextReplacement("201")
        dialog("digital-sayad-0000000000000201")
        compose.onNodeWithTag("digital-sayad-error").assertDoesNotExist()
        click("digital-continue")
        shown("S-152")
        compose.onNodeWithText("خیر").assertExists()
        compose.onNodeWithText("IR000000000000000000000201").assertExists()
    }

    @Test
    fun scenarioStatesStayLocal() {
        for (state in listOf("empty", "loading", "error")) {
            click("scenario-$state")
            click("demo-digital-active")
            compose.onNodeWithTag("data-$state").assertIsDisplayed()
            if (state == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("digital-active").assertExists()
            } else {
                back()
                back()
            }
        }
    }
}
