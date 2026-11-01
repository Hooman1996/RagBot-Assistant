package org.ragbot.replica

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehiclesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
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

    private fun menu() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-vehicle").performScrollTo().performClick()
        shown("S-253")
    }

    @Test
    fun plateListShowsLtrPlateFragmentsFiltersPollutionAndDeletesAfterConfirm() {
        menu()
        click("vehicle-menu-AUTHENTICATED_HOME_ANNUAL_TOLL")
        shown("S-256")
        compose.onNodeWithTag("vehicle-plate-plate-2", useUnmergedTree = true).assertExists()
        val plate = compose.onNodeWithTag("vehicle-plate-plate-1", useUnmergedTree = true)
        val first = compose.onNodeWithText("12", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val region =
            compose.onNodeWithText("ایران 67", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // The plate reads left to right inside the RTL page.
        assert(first.left < region.left)
        plate.assertExists()
        compose.onNodeWithText("(خودم)", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("(دیگران)", useUnmergedTree = true).assertExists()
        // FR-17: selecting a plate or adding one opens the annual toll inquiry.
        click("vehicle-plate-1")
        shown("S-241")
        back()
        click("vehicle-new")
        shown("S-241")
        back()
        click("vehicle-delete-plate-2")
        compose.onNodeWithText("انصراف").performClick()
        compose.onNodeWithTag("vehicle-plate-2").assertExists()
        click("vehicle-delete-plate-2")
        compose.onNodeWithTag("vehicle-delete-confirm").performClick()
        compose.onNodeWithTag("vehicle-plate-2").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("vehicle-plate-2").assertDoesNotExist()
        back()
        click("vehicle-menu-AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY")
        compose.onNodeWithTag("vehicle-plate-1").assertExists()
        compose.onNodeWithTag("vehicle-plate-2").assertDoesNotExist()
    }

    @Test
    fun contactAndLicenceListsOpenTheirInquiryHistoryAndNestedBackReturns() {
        menu()
        click("vehicle-menu-AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY")
        shown("S-246")
        compose.onNodeWithText("مخاطب خدمت را انتخاب کنید.").assertExists()
        click("vehicle-history-contact-2")
        shown("S-252")
        compose.onNodeWithText("0000000035", useUnmergedTree = true).assertExists()
        click("vehicle-inquiry-result")
        unavailable("نتیجه استعلام پلاک‌های فعال")
        back()
        shown("S-246")
        back()
        click("vehicle-menu-AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY")
        shown("S-248")
        click("vehicle-delete-licence-1")
        compose.onNodeWithTag("vehicle-delete-confirm").performClick()
        compose.onNodeWithTag("vehicle-empty").assertExists()
        compose.onNodeWithText("گواهینامه جدید").assertExists()
        back()
        shown("S-253")
    }

    @Test
    fun paymentHistoryMenuAndScenarioStatesStayLocal() {
        menu()
        click("vehicle-menu-AUTHENTICATED_HOME_PAYMENT_TOLL_HISTORY")
        shown("S-254")
        click("vehicle-history-AUTHENTICATED_HOME_FREEWAY_TOLL_HISTORY")
        unavailable("سوابق آزادراه")
        back()
        back()
        back()
        for (state in listOf("empty", "error")) {
            click("scenario-$state")
            compose.onNodeWithTag("demo-entry").performClick()
            compose.onNodeWithTag("menu-vehicle").performScrollTo().performClick()
            compose.onNodeWithTag("data-$state").assertIsDisplayed()
            if (state == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("vehicle-menu-AUTHENTICATED_HOME_ANNUAL_TOLL").assertExists()
            } else {
                back()
                back()
            }
        }
    }
}
