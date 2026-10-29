package org.ragbot.replica

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun enter() {
        compose.onNodeWithTag("demo-entry").performClick()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun scrollTo(tag: String) = compose.onNodeWithTag(tag).performScrollTo()

    @Test
    fun demoEntryIsCredentialFreeAndRendersPersianRtlHome() {
        compose.onNodeWithText("نسخه نمایشی").assertIsDisplayed()
        enter()
        compose.onNodeWithTag("S-026").assertIsDisplayed()
        compose.onNodeWithTag("F-066").assertExists()
        compose.onNodeWithText("شماره حساب ${HomeFixture.accountNumber}").assertIsDisplayed()
        val home =
            compose.onNodeWithTag("control-HOME").assertIsSelected().getUnclippedBoundsInRoot()
        val profile =
            compose
                .onNodeWithTag("control-PROFILE")
                .assertIsNotSelected()
                .getUnclippedBoundsInRoot()
        assertTrue("Home must be rightmost in RTL", home.left > profile.right)
        val account = compose.onNodeWithTag("account-number").getUnclippedBoundsInRoot()
        val increase = compose.onNodeWithText("افزایش موجودی").getUnclippedBoundsInRoot()
        assertTrue("Account must be left of increase-balance", account.right < increase.left)
        compose.onNodeWithTag("header-NOTIFICATIONS").assertHasClickAction()
        compose.onNodeWithTag("header-SUPPORT").assertHasClickAction()
        compose.runOnIdle {
            val window = compose.activity.window
            assertFalse(
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars
            )
        }
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            compose.activity.checkSelfPermission(Manifest.permission.INTERNET),
        )
    }

    @Test
    fun menuGroupsContainRequiredItemsInRtlOrder() {
        enter()
        val groups = listOf(HomeFixture.top, HomeFixture.quick) + HomeFixture.services.chunked(4)
        groups.forEach { row ->
            row.forEach { item ->
                scrollTo("menu-${item.key}")
                    .assertContentDescriptionEquals(item.title)
                    .assertHasClickAction()
                    .assertIsDisplayed()
            }
            row.zipWithNext().forEach { (right, left) ->
                assertTrue(
                    "${right.title} must precede ${left.title} in RTL",
                    compose
                        .onNodeWithTag("menu-${right.key}")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .center
                        .x >
                        compose
                            .onNodeWithTag("menu-${left.key}")
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .center
                            .x,
                )
            }
        }
    }

    @Test
    fun lowerServicesAndSyntheticHistoryAreReachableAboveBottomNavigation() {
        enter()
        scrollTo("menu-branches").assertIsDisplayed()
        scrollTo("edit-layout").assertIsDisplayed()
        scrollTo("transaction-preview-2").assertIsDisplayed()
        val row = compose.onNodeWithTag("transaction-preview-2").getUnclippedBoundsInRoot()
        val navigation = compose.onNodeWithTag("control-HOME").getUnclippedBoundsInRoot()
        assertTrue("History cannot be covered by bottom navigation", row.bottom <= navigation.top)
        compose.onNodeWithText("۷۵۰٬۰۰۰ ریال", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun unavailableDestinationKeepsScrollAndBalanceStateThroughRecreation() {
        enter()
        compose.onNodeWithTag("balance").performClick()
        compose.onNodeWithTag("balance").assertTextContains("۱۲٬۵۰۰٬۰۰۰ ریال", substring = true)
        scrollTo("menu-branches")
        // A recreated Activity may have a different Compose root origin; compare screen pixels.
        val before = compose.onNodeWithTag("menu-branches").fetchSemanticsNode().positionOnScreen.y
        val beforeScroll =
            compose
                .onNodeWithTag("home-scroll")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        compose.onNodeWithTag("menu-branches").performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("شعب", substring = true)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.onNodeWithText("بازگشت به خانه").performClick()
        assertEquals(
            before,
            compose.onNodeWithTag("menu-branches").fetchSemanticsNode().positionOnScreen.y,
            1f,
        )
        assertEquals(
            beforeScroll,
            compose
                .onNodeWithTag("home-scroll")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value(),
            0f,
        )
        compose.onNodeWithTag("balance").assertTextContains("۱۲٬۵۰۰٬۰۰۰ ریال", substring = true)
        compose.onNodeWithTag("control-HOME").assertIsSelected()
    }

    @Test
    fun shellControlsNeverPretendToNavigate() {
        enter()
        listOf(
                "control-PROFILE",
                "control-QR",
                "header-SEARCH",
                "header-SHORTCUTS",
                "header-LOGOUT",
                "header-SUPPORT",
                "header-NOTIFICATIONS",
            )
            .forEach {
                compose.onNodeWithTag(it).performClick()
                compose.onNodeWithTag("unavailable").assertIsDisplayed()
                back()
                compose.onNodeWithTag("unavailable").assertDoesNotExist()
                compose.onNodeWithTag("control-HOME").assertIsSelected()
            }
        compose.onNodeWithTag("control-HOME").performClick()
        compose.onNodeWithTag("unavailable").assertDoesNotExist()
    }

    @Test
    fun expansionBalanceBackAndRecreationKeepLocalState() {
        enter()
        compose.onNodeWithTag("balance").performClick()
        compose.onNodeWithTag("expand-account").performClick()
        compose.onNodeWithText("صاحب حساب: ${HomeFixture.accountOwner}").assertIsDisplayed()
        compose.onNodeWithText("مدیریت حساب").performClick()
        back()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("صاحب حساب: ${HomeFixture.accountOwner}").assertIsDisplayed()
        compose.onNodeWithTag("balance").assertTextContains("عدم نمایش", substring = true)
        back()
        compose.onNodeWithText("صاحب حساب: ${HomeFixture.accountOwner}").assertDoesNotExist()
        compose.onNodeWithTag("balance").performClick()
        compose.onNodeWithText("مشاهده موجودی").assertIsDisplayed()
        back()
        compose.onNodeWithTag("demo-entry").assertIsDisplayed()
    }

    private fun reorderFirstTwoServices() {
        val from = compose.onNodeWithTag("edit-cheque").fetchSemanticsNode().boundsInRoot.center
        val to = compose.onNodeWithTag("edit-loan").fetchSemanticsNode().boundsInRoot.center
        compose.onNodeWithTag("edit-cheque").performTouchInput {
            val localStart = center
            val localEnd = localStart + Offset(to.x - from.x, to.y - from.y)
            swipe(localStart, localEnd, 700)
        }
    }

    @Test
    fun editLayoutDragAppliesOnlyServiceOrderAndSurvivesRecreation() {
        enter()
        scrollTo("edit-layout").performClick()
        compose.onNodeWithTag("apply-order").assertIsNotEnabled()
        reorderFirstTwoServices()
        compose.onNodeWithTag("apply-order").assertIsEnabled().performClick()
        compose.onNodeWithTag("layout-editor").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        scrollTo("menu-loan")
        assertTrue(
            compose.onNodeWithTag("menu-loan").fetchSemanticsNode().boundsInRoot.center.x >
                compose.onNodeWithTag("menu-cheque").fetchSemanticsNode().boundsInRoot.center.x
        )
        scrollTo("menu-wallet").assertIsDisplayed()
        assertTrue(
            compose.onNodeWithTag("menu-wallet").fetchSemanticsNode().boundsInRoot.center.x >
                compose.onNodeWithTag("menu-loyalty").fetchSemanticsNode().boundsInRoot.center.x
        )
    }

    @Test
    fun editLayoutCancelAndBackDiscardDraft() {
        enter()
        scrollTo("edit-layout").performClick()
        reorderFirstTwoServices()
        compose.onNodeWithTag("edit-cheque").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.onNodeWithText("بازگشت به خانه").performClick()
        compose.onNodeWithTag("apply-order").assertIsEnabled()
        compose.onNodeWithText("انصراف").performClick()
        scrollTo("menu-cheque")
        assertTrue(
            compose.onNodeWithTag("menu-cheque").fetchSemanticsNode().boundsInRoot.center.x >
                compose.onNodeWithTag("menu-loan").fetchSemanticsNode().boundsInRoot.center.x
        )
        scrollTo("edit-layout").performClick()
        reorderFirstTwoServices()
        back()
        compose.onNodeWithTag("layout-editor").assertDoesNotExist()
        scrollTo("menu-cheque")
        assertTrue(
            compose.onNodeWithTag("menu-cheque").fetchSemanticsNode().boundsInRoot.center.x >
                compose.onNodeWithTag("menu-loan").fetchSemanticsNode().boundsInRoot.center.x
        )
    }
}
