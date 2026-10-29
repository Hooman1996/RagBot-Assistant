package org.ragbot.replica

import android.Manifest
import android.content.pm.PackageManager
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

    @Test
    fun demoEntryIsCredentialFreeAndRendersPersianRtlHome() {
        compose.onNodeWithText("نسخه نمایشی").assertIsDisplayed()
        enter()
        compose.onNodeWithTag("S-026").assertIsDisplayed()
        compose.onNodeWithTag("F-066").assertExists()
        compose.onNodeWithText("شماره حساب —").assertIsDisplayed()
        compose.onNodeWithContentDescription("خدمات؛ فهرست پیش‌نمایش خالی است").assertExists()
        val home =
            compose.onNodeWithTag("control-HOME").assertIsSelected().getUnclippedBoundsInRoot()
        val profile =
            compose
                .onNodeWithTag("control-PROFILE")
                .assertIsNotSelected()
                .getUnclippedBoundsInRoot()
        assertTrue("Home must be on the right in RTL", home.left > profile.right)
        compose.onNodeWithTag("control-QR").assertHasClickAction()
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
    fun unavailableControlsNeverPretendToNavigate() {
        enter()
        listOf(
                "control-ACCOUNT",
                "control-CARD",
                "control-PROFILE",
                "control-QR",
                "header-SEARCH",
                "header-SHORTCUTS",
                "header-LOGOUT",
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
    fun backAndRecreationPreservePreviewState() {
        enter()
        compose.onNodeWithTag("expand-account").performClick()
        compose.onNodeWithText("صاحب حساب: —").assertIsDisplayed()
        compose.onNodeWithText("مدیریت حساب").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        back()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("صاحب حساب: —").assertIsDisplayed()
        back()
        compose.onNodeWithText("صاحب حساب: —").assertDoesNotExist()
        back()
        compose.onNodeWithTag("demo-entry").assertIsDisplayed()
    }

    @Test
    fun missingAccountDoesNotInventBalanceOrServiceData() {
        enter()
        compose.onNodeWithText("مشاهده موجودی").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
        compose.onNodeWithText("بازگشت به خانه").performClick()
        compose.onNodeWithText("افزایش موجودی").performClick()
        compose.onNodeWithTag("unavailable").assertIsDisplayed()
    }
}
