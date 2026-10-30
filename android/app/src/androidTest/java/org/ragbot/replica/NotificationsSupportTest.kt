package org.ragbot.replica

import android.content.pm.PackageManager
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationsSupportTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun systemBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }

    private fun openNotifications() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("header-NOTIFICATIONS").performClick()
        compose.onNodeWithTag("S-075").assertIsDisplayed()
    }

    @Test
    fun homeSupportControlsOpenAuthenticatedFaqAndRestoreTheirOrigin() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("menu-support").performScrollTo()
        val scrollBefore =
            compose
                .onNodeWithTag("home-scroll")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        compose.onNodeWithTag("menu-support").performClick()
        compose.onNodeWithTag("S-077").assertIsDisplayed()
        compose.onNodeWithTag("faq-variant").assertTextContains("کاربر واردشده", substring = true)
        compose.onNodeWithTag("faq-support").assertDoesNotExist()
        compose.onNodeWithTag("faq-item-0").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("faq-answer-0", useUnmergedTree = true).assertIsDisplayed()
        back()
        compose.onNodeWithTag("control-HOME").assertIsSelected()
        assertEquals(
            scrollBefore,
            compose
                .onNodeWithTag("home-scroll")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value(),
            0f,
        )
        compose.onNodeWithTag("control-CARD").performClick()
        compose.onNodeWithTag("header-SUPPORT").performClick()
        compose.onNodeWithTag("S-077").assertIsDisplayed()
        compose.onNodeWithTag("S-076").assertDoesNotExist()
        compose.onNodeWithTag("S-080").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        back()
        compose.onNodeWithTag("control-CARD").assertIsSelected()
        compose.onNodeWithTag("header-NOTIFICATIONS").performClick()
        compose.onNodeWithTag("S-075").assertIsDisplayed()
        back()
        compose.onNodeWithTag("control-CARD").assertIsSelected()
    }

    @Test
    fun notificationsReadDeleteCancellationAndHeaderCountSurviveRecreation() {
        compose.onNodeWithTag("demo-entry").performClick()
        compose.onNodeWithTag("notification-marker", useUnmergedTree = true).assertExists()
        compose
            .onNodeWithTag("header-NOTIFICATIONS")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "5 اعلان خوانده‌نشده",
                )
            )
            .performClick()
        compose.onNodeWithTag("notification-unread-notice-1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("notification-item-notice-1").performClick()
        compose.onNodeWithTag("notification-detail").assertIsDisplayed()
        compose.onNodeWithTag("notification-share").performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("اشتراک‌گذاری", substring = true)
        back()
        compose.onNodeWithTag("notification-detail").assertIsDisplayed()
        systemBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("notification-detail").fetchSemanticsNodes().isEmpty()
        }
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("S-075").assertIsDisplayed()
        compose
            .onNodeWithTag("notification-unread-notice-1", useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onNodeWithTag("notification-delete-notice-1").performClick()
        compose.onNodeWithTag("notification-delete-cancel").performClick()
        compose.waitUntil(5_000) { compose.activity.hasWindowFocus() }
        compose.onNodeWithTag("notification-item-notice-1").assertIsDisplayed()
        compose.onNodeWithTag("notification-delete-notice-1").performClick()
        // Send Back to the active window and verify the underlying page survives.
        compose.onNodeWithTag("notification-delete-sheet").assertIsDisplayed()
        compose.waitUntil(5_000) { !compose.activity.hasWindowFocus() }
        systemBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("notification-delete-sheet").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("notification-delete-sheet").assertDoesNotExist()
        compose.onNodeWithTag("notification-item-notice-1").assertIsDisplayed()
        compose.onNodeWithTag("notification-delete-notice-1").performClick()
        compose.onNodeWithTag("notification-delete-confirm").performClick()
        compose.onNodeWithTag("notification-item-notice-1").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("notification-item-notice-1").assertDoesNotExist()
        back()
        compose.onNodeWithTag("control-HOME").assertIsSelected()
        compose
            .onNodeWithTag("header-NOTIFICATIONS")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "4 اعلان خوانده‌نشده",
                )
            )
    }

    @Test
    fun bulkNotificationChangesAreLocalAndCanceledDeletionKeepsRecords() {
        openNotifications()
        compose.onNodeWithTag("notification-read-all").performClick()
        back()
        compose.onNodeWithTag("notification-marker", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("header-NOTIFICATIONS").performClick()
        compose.onNodeWithTag("notification-delete-all").performClick()
        compose.onNodeWithTag("notification-delete-cancel").performClick()
        compose.onNodeWithTag("notification-item-notice-1").assertIsDisplayed()
        compose.onNodeWithTag("notification-delete-all").performClick()
        compose.onNodeWithTag("notification-delete-confirm").performClick()
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("data-empty").assertIsDisplayed()
        back()
        compose
            .onNodeWithTag("header-NOTIFICATIONS")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "0 اعلان خوانده‌نشده",
                )
            )
    }

    @Test
    fun notificationEmptyLoadingAndErrorRetryAreInspectable() {
        listOf("empty", "loading", "error").forEach { scenario ->
            compose.onNodeWithTag("scenario-$scenario").performClick()
            openNotifications()
            compose.onNodeWithTag("data-$scenario").assertIsDisplayed()
            if (scenario == "error") {
                compose.onNodeWithText("تلاش مجدد").performClick()
                compose.onNodeWithTag("notification-item-notice-1").assertIsDisplayed()
            }
            back()
            back()
            compose.onNodeWithTag("demo-entry").assertIsDisplayed()
        }
    }

    @Test
    fun notificationPaginationFiltersSelectionAndDetailRestoreTheirContext() {
        openNotifications()
        compose.onNodeWithTag("notification-list").performScrollToIndex(2)
        compose.waitForIdle()
        compose
            .onNodeWithTag("notification-list")
            .performScrollToNode(hasTestTag("notification-item-notice-6"))
        compose.onNodeWithTag("notification-item-notice-6").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("notification-detail").assertIsDisplayed()
        compose
            .onNode(
                hasText("DEMO-6", substring = true) and
                    hasAnyAncestor(hasTestTag("notification-detail")),
                useUnmergedTree = true,
            )
            .assertExists()
        compose.onNodeWithTag("notification-detail-dismiss").performScrollTo().performClick()
        compose.onNodeWithTag("notification-item-notice-6").assertIsDisplayed()
        compose.onNodeWithTag("notification-filter-info").performClick()
        compose.onNodeWithTag("notification-list").performScrollToIndex(0)
        compose.onNodeWithTag("notification-select").performClick()
        compose.onNodeWithTag("notification-select-notice-1").performClick().assertIsOn()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("notification-filter-info").assertIsSelected()
        compose.onNodeWithTag("notification-select-notice-1").assertIsOn()
        compose.onNodeWithTag("notification-delete-selected").performClick()
        compose.onNodeWithTag("notification-delete-cancel").performClick()
        compose.onNodeWithTag("notification-select-notice-1").assertIsOn()
        compose.onNodeWithTag("notification-delete-selected").performClick()
        compose.onNodeWithTag("notification-delete-confirm").performClick()
        compose.onNodeWithTag("notification-item-notice-1").assertDoesNotExist()
        compose.onNodeWithTag("notification-item-notice-3").assertIsDisplayed()
    }

    @Test
    fun faqWithoutLoginExpandsAndTheLocalIdentityGateDoesNotRestoreOtp() {
        compose.onNodeWithTag("demo-S-078").performScrollTo().performClick()
        compose.onNodeWithTag("S-078").assertIsDisplayed()
        compose.onNodeWithTag("faq-variant").assertTextContains("بدون ورود", substring = true)
        compose.onNodeWithTag("faq-answer-0", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("faq-item-0").performClick()
        compose.onNodeWithTag("faq-answer-0", useUnmergedTree = true).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("faq-answer-0", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("faq-support").performClick()
        compose.onNodeWithTag("support-mobile").performTextReplacement("123")
        compose.onNodeWithTag("support-identity-continue").performClick()
        compose.onNodeWithTag("support-otp").assertDoesNotExist()
        compose.onNodeWithTag("support-mobile").performTextReplacement("09120000000")
        compose.onNodeWithTag("support-identity-continue").performClick()
        compose.onNodeWithTag("support-otp").performTextReplacement("12345")
        compose.activityRule.scenario.recreate()
        compose
            .onNodeWithTag("support-otp")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
            )
        compose.onNodeWithTag("support-otp-continue").performClick()
        compose.onNodeWithTag("S-081").assertDoesNotExist()
        compose.onNodeWithTag("support-otp").performTextReplacement("12345")
        compose.onNodeWithTag("support-otp-continue").performClick()
        compose.onNodeWithTag("S-081").assertIsDisplayed()
        compose.onNodeWithTag("support-send").assertIsNotEnabled()
        compose.onNodeWithTag("support-draft").performTextReplacement("پیش‌نویس پیام نمونه")
        compose.onNodeWithTag("support-send").performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("ارسال نشد", substring = true)
        back()
        compose.onNodeWithTag("message-tab-history").performClick()
        compose.onNodeWithTag("ticket-DEMO-1002").performClick()
        compose.onNodeWithTag("S-081").assertIsDisplayed()
        compose.onNodeWithTag("support-ticket-DEMO-1002").assertIsDisplayed()
        compose.onNodeWithTag("message-tab-history").performClick()
        compose.onNodeWithTag("tickets-more").performScrollTo().performClick()
        compose.onNodeWithTag("ticket-DEMO-1003").performScrollTo().performClick()
        compose.onNodeWithTag("support-ticket-DEMO-1003").assertIsDisplayed()
        compose.onNodeWithTag("message-tab-history").performClick()
        compose.onNodeWithTag("ticket-DEMO-1003").assertIsDisplayed()
        compose.onNodeWithTag("message-tab-compose").performClick()
        compose.onNodeWithTag("support-draft").assertTextContains("پیش‌نویس پیام نمونه")
        compose.onNodeWithTag("support-ticket-DEMO-1003").assertDoesNotExist()
        back()
        compose.onNodeWithTag("S-078").assertIsDisplayed()
    }

    @Test
    fun ticketsKeepTheirIdentityAndLocalDraftsAcrossBackAndRecreation() {
        val draft = "پیش‌نویس ویرایش DEMO-1001"
        compose.onNodeWithTag("demo-S-080").performScrollTo().performClick()
        compose.onNodeWithTag("ticket-DEMO-1001").performClick()
        compose.onNodeWithTag("S-082").assertIsDisplayed()
        compose.onNodeWithTag("support-ticket-DEMO-1001").assertIsDisplayed()
        compose.onNodeWithTag("support-edit").performClick()
        compose.onNodeWithTag("support-draft").performTextReplacement("کوتا")
        compose.onNodeWithTag("support-send").assertIsNotEnabled()
        compose.onNodeWithTag("support-draft").performTextReplacement(draft)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("support-draft").assertTextContains(draft)
        compose.onNodeWithTag("support-send").performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("ارسال نشد", substring = true)
        back()
        back()
        compose.onNodeWithTag("S-080").assertIsDisplayed()
        compose.onNodeWithTag("ticket-DEMO-1001").performClick()
        compose.onNodeWithTag("support-edit").performClick()
        compose.onNodeWithTag("support-draft").assertTextContains(draft)
        back()
        compose.onNodeWithTag("tickets-more").performScrollTo().performClick()
        compose.onNodeWithTag("ticket-DEMO-1003").performScrollTo().performClick()
        compose.onNodeWithTag("support-ticket-DEMO-1003").assertIsDisplayed()
        compose.onNodeWithTag("support-draft").assertDoesNotExist()
        compose.onNodeWithTag("support-send").assertDoesNotExist()
        back()
        compose.onNodeWithTag("ticket-DEMO-1003").assertIsDisplayed()
    }

    @Test
    fun newTicketComposerRetainsItsOwnDraftWithoutSelectingAnExistingTicket() {
        val draft = "پیش‌نویس پیام جدید بدون تیکت"
        compose.onNodeWithTag("demo-S-082").performScrollTo().performClick()
        compose.onNodeWithTag("S-082").assertIsDisplayed()
        SupportFixture.tickets.forEach { ticket ->
            compose.onNodeWithTag("support-ticket-${ticket.key}").assertDoesNotExist()
        }
        compose.onNodeWithTag("support-draft").performTextReplacement("کوتا")
        compose.onNodeWithTag("support-send").assertIsNotEnabled()
        compose.onNodeWithTag("support-draft").performTextReplacement(draft)
        back()
        back()
        compose.onNodeWithTag("demo-S-082").performScrollTo().performClick()
        compose.onNodeWithTag("support-draft").assertTextContains(draft)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("support-draft").assertTextContains(draft)
        compose.onNodeWithTag("support-send").performClick()
        compose.onNodeWithTag("unavailable").assertTextContains("ارسال نشد", substring = true)
        back()
        compose.onNodeWithTag("support-draft").assertTextContains(draft)
        back()
        back()
        compose.onNodeWithTag("demo-S-080").performScrollTo().performClick()
        compose.onNodeWithTag("ticket-DEMO-1001").performClick()
        compose.onNodeWithTag("support-ticket-DEMO-1001").assertIsDisplayed()
        compose.onNodeWithTag("support-edit").performClick()
        compose
            .onNodeWithTag("support-draft")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString(SupportFixture.tickets.first().question),
                )
            )
    }

    @Test
    fun suggestionValidationAndDismissalKeepTheLocalDraft() {
        val draft = "پیشنهاد ساختگی برای بازبینی ظاهر برنامه"
        compose.onNodeWithTag("demo-S-079").performScrollTo().performClick()
        compose.onNodeWithTag("suggestion-submit").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("suggestion-description").performTextReplacement(draft)
        compose.onNodeWithTag("suggestion-rating-GOOD").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("suggestion-description").assertTextContains(draft)
        compose.onNodeWithTag("suggestion-rating-GOOD").assertIsSelected()
        compose.onNodeWithTag("suggestion-submit").performScrollTo().performClick()
        compose.onNodeWithTag("support-preview-result").assertTextContains("محلی", substring = true)
        compose.onNodeWithTag("support-preview-cancel").performClick()
        compose.onNodeWithTag("suggestion-description").assertTextContains(draft)
        compose.onNodeWithTag("suggestion-submit").performScrollTo().performClick()
        compose.onNodeWithTag("support-preview-confirm").performClick()
        compose.onNodeWithTag("S-079").assertDoesNotExist()
        compose.onNodeWithTag("control-HOME").assertIsSelected()
        back()
        compose.onNodeWithTag("demo-S-079").performScrollTo().performClick()
        compose.onNodeWithTag("suggestion-description").assertTextContains(draft)
    }

    @Test
    fun chatbotValidatesAndRestoresInputWhileAnsweringStaysUnavailable() {
        val question = "پرسش ساختگی برای بررسی صفحه"
        compose.onNodeWithTag("demo-S-076").performScrollTo().performClick()
        compose.onNodeWithTag("S-076").assertIsDisplayed()
        compose.onNodeWithTag("chatbot-draft").performTextReplacement("متن")
        compose.onNodeWithTag("chatbot-send").assertIsNotEnabled()
        compose.onNodeWithTag("chatbot-draft").performTextReplacement(question)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("chatbot-draft").assertTextContains(question)
        compose.onNodeWithTag("chatbot-send").performClick()
        compose
            .onNodeWithTag("unavailable")
            .assertTextContains("پاسخ تولید نمی‌شود", substring = true)
        back()
        compose.onNodeWithTag("chatbot-draft").assertTextContains(question)
        compose.onAllNodesWithText(question, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun supportListsExposeEmptyLoadingAndErrorRetry() {
        listOf("empty", "loading", "error").forEach { scenario ->
            compose.onNodeWithTag("scenario-$scenario").performClick()
            compose.onNodeWithTag("demo-S-080").performScrollTo().performClick()
            compose.onNodeWithTag("support-$scenario").assertIsDisplayed()
            if (scenario == "error") {
                compose.onNodeWithTag("support-retry").performClick()
                compose.onNodeWithTag("ticket-DEMO-1001").assertIsDisplayed()
            }
            back()
            back()
        }
    }

    @Test
    fun previewAddsNoNetworkNotificationStorageOrBackgroundComponents() {
        val info =
            compose.activity.packageManager.getPackageInfo(
                compose.activity.packageName,
                PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS,
            )
        // Existing AndroidX initialization/profile components are part of the pinned baseline.
        assertEquals(
            listOf("org.ragbot.replica.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"),
            info.requestedPermissions.orEmpty().toList(),
        )
        assertTrue(info.services.isNullOrEmpty())
        assertEquals(
            listOf("androidx.profileinstaller.ProfileInstallReceiver"),
            info.receivers.orEmpty().map { it.name },
        )
        assertEquals("android.permission.DUMP", info.receivers.orEmpty().single().permission)
        assertEquals(
            listOf("androidx.startup.InitializationProvider"),
            info.providers.orEmpty().map { it.name },
        )
        assertFalse(info.providers.orEmpty().single().exported)
        assertEquals(
            listOf(MainActivity::class.java.name),
            info.activities.orEmpty().map { it.name },
        )
    }
}
