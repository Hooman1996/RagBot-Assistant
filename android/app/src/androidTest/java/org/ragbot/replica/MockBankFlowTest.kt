package org.ragbot.replica

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Each test launches the app itself, so that a mock file can change before it starts. */
@RunWith(AndroidJUnit4::class)
class MockBankFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun override(name: String) =
        File(context.getExternalFilesDir(MockData.FOLDER), "$name.json")

    private fun bundled(name: String) =
        context.assets.open("mock/$name.json").bufferedReader().use { it.readText() }

    @After
    fun removeOverrides() {
        context.getExternalFilesDir(MockData.FOLDER)?.listFiles()?.forEach { it.delete() }
    }

    private fun launch() = ActivityScenario.launch(MainActivity::class.java)

    private fun click(tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun tap(tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performClick()
    }

    private fun fill(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(value)
    }

    private fun cardTransfer(amount: String) {
        tap("demo-entry")
        click("menu-card-transfer")
        click("card-transfer-source-picker")
        tap("card-transfer-source-card-1")
        click("card-transfer-favorites")
        tap("card-transfer-contact-reza-card")
        fill("card-transfer-amount", amount)
        click("card-transfer-review")
        compose.onNodeWithTag("card-transfer-review-holder").assertTextContains("رضا", substring = true)
        fill("card-transfer-cvv2", "123")
        fill("card-transfer-expiry", "0812")
        fill("card-transfer-password", "12345")
        click("card-transfer-submit")
    }

    @Test
    fun aCardTransferToRezaEndsInAReceiptAndAppearsInHistory() {
        launch()
        cardTransfer("250000")
        compose.onNodeWithTag("S-093").assertIsDisplayed()
        compose.onNodeWithTag("bank-receipt").assertExists()
        click("bank-receipt-done")
        compose.onNodeWithTag("S-026").assertIsDisplayed()
        click("menu-history")
        val trace = MockBank.transactions.first().key
        compose.onNodeWithTag("history-$trace").assertExists()
    }

    @Test
    fun anAmountOverTheBalanceFailsWithAClearMessageAndStaysOnTheForm() {
        launch()
        cardTransfer("99999999")
        compose
            .onNodeWithTag("bank-failure")
            .assertTextContains(MockData.current.messages.getValue("insufficient-balance"))
        tap("bank-failure-close")
        compose.onNodeWithTag("S-176").assertIsDisplayed()
    }

    @Test
    fun anEditedContactShowsInThePicker() {
        override("contacts")
            .writeText(
                bundled("contacts")
                    .replace(
                        "\"people\": [",
                        "\"people\": [{\"id\": \"sara\", \"name\": \"سارا\", " +
                            "\"card\": \"0000000000000109\", \"account\": \"0000000000099\", " +
                            "\"iban\": \"IR070000000000000000000077\", " +
                            "\"wallet\": \"0000000000000999\"},",
                    )
            )
        launch()
        tap("demo-entry")
        click("menu-card-transfer")
        click("card-transfer-favorites")
        compose.onNodeWithTag("card-transfer-contact-sara-card").assertExists()
    }

    @Test
    fun aForcedOutcomeOverridesTheNormalResult() {
        override("scenarios")
            .writeText(
                bundled("scenarios")
                    .replace(
                        "\"forced\": []",
                        "\"forced\": [{\"operation\": \"card-transfer\", " +
                            "\"outcome\": \"service-unavailable\"}]",
                    )
            )
        launch()
        cardTransfer("1000")
        compose
            .onNodeWithTag("bank-failure")
            .assertTextContains(MockData.current.messages.getValue("service-unavailable"))
    }

    @Test
    fun aBadFileShowsAnErrorPageNamingItInsteadOfCrashing() {
        override("bills").writeText("{\"unit\": \"IRR\", \"bills\": [{\"amount\": 1.5}]}")
        launch()
        compose.onNodeWithTag("mock-data-error").assertExists()
        compose.onNodeWithTag("mock-data-error-file").assertTextContains("bills.json")
        assertTrue(override("bills").delete())
    }
}
