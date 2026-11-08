package org.ragbot.replica.bridge

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.ragbot.replica.MainActivity
import org.ragbot.replica.MockBank

/**
 * The demo walk's journeys through the executor, from the app's front page to the review. Each
 * ends on the review page with the money untouched: nothing here ever confirms.
 */
@RunWith(AndroidJUnit4::class)
class BridgeJourneysTest {
    private lateinit var app: ActivityScenario<MainActivity>
    private lateinit var rig: BridgeRig

    @Before
    fun open() {
        app = ActivityScenario.launch(MainActivity::class.java)
        rig = BridgeRig()
    }

    @After
    fun moneyIsUntouched() {
        assertEquals(12_500_000L, MockBank.balance("account-1"))
        assertTrue(MockBank.transactions.none { it.key.startsWith("bank-") })
        app.close()
    }

    @Test
    fun cardToCardToReza() {
        rig.reach("S-174")
        rig.fill(
            "S-174",
            "card-transfer-source" to text("card-1"),
            "card-transfer-destination" to text("0000000000000091"),
            "card-transfer-amount" to rials(250_000),
        )
        val seen = rig.executor.observer.observe()
        val destination = seen.fields.first { it.ref == "card-transfer-destination" }
        assertEquals("****0091", destination.value)
        val review = rig.expect(rig.review(), "S-176")
        assertEquals(1L, review.observation.draft?.review_version)
    }

    @Test
    fun billPayment() {
        rig.reach("S-209")
        rig.fill(
            "S-209",
            "bill-id" to text("9000000000001"),
            "bill-pay-id" to text("1250001"),
        )
        rig.expect(rig.review(), "S-207")
    }

    @Test
    fun mobileCharge() {
        rig.reach("S-211")
        rig.fill(
            "S-211",
            "topup-phone" to text("09010000000"),
            "topup-amount" to text("200000"),
        )
        rig.expect(rig.review(), "S-212")
    }

    /** The toll's fee and results sit behind the user's OTP, so its form is where it stops. */
    @Test
    fun trafficToll() {
        rig.reach("S-275")
        rig.fill(
            "S-275",
            "toll-plate-0" to text("12"),
            "toll-letter" to text("ب"),
            "toll-plate-2" to text("345"),
            "toll-plate-3" to text("67"),
        )
        rig.expect(rig.review(), "S-275")
    }

    /** The user gives the three consents. The executor stops at each, and at the review. */
    @Test
    fun loanRequest() {
        rig.reach("S-287")
        rig.fill("S-287", "svc-product" to text("personal"))
        rig.reach("S-308")
        rig.fill("S-308", "svc-amount" to rials(120_000_000), "svc-months" to text("12"))
        rig.reach("S-286")
        rig.fill("S-286", "svc-job" to text("کارمند"))
        rig.reach("S-335")
        rig.fill(
            "S-335",
            "svc-guarantor" to text("رضا"),
            "svc-guarantorCode" to text("0000000019"),
        )
        rig.reach("S-285")
        consent("svc-agree-yes", "S-334")
        consent("svc-note-issue", "S-378")
        consent("svc-sign-sign", "S-336")
        rig.expect(rig.review(), "S-336")
    }

    @Test
    fun aDisabledControlStopsTheRoute() {
        val result = rig.navigate("S-128")
        assertEquals(listOf("control_disabled:color-ok"), result.needs)
        assertEquals("S-123", result.observation.screen)
    }

    @Test
    fun anEmptyFormStopsAndNamesItsFields() {
        rig.reach("S-209")
        val result = rig.review()
        assertEquals("blocked", result.outcome)
        assertEquals(listOf("bill-id", "bill-pay-id"), result.needs)
        assertEquals("S-209", result.observation.screen)
    }

    @Test
    fun theUsersOwnEditMakesTheDraftStale() {
        rig.reach("S-209")
        rig.fill("S-209", "bill-id" to text("9000000000001"))
        val before = rig.executor.observer.draftVersion
        rig.controls.setText("bill-id", "9000000000002")
        rig.executor.observer.let { observer ->
            val changed =
                Actions(rig.controls).waitUntil { observer.observe().draft?.draft_version != before }
            assertTrue(changed)
        }
        val seen = rig.executor.observer.observe().observation_version
        val stale =
            rig.executor.execute(
                Command(
                    1,
                    1,
                    "task-1",
                    1,
                    "stale-draft",
                    BridgeConfig.DEVICE_ID,
                    BridgeConfig.DEVICE_ID,
                    "show_review",
                    seen,
                    before,
                    "2100-01-01T00:00:00Z",
                )
            )
        assertEquals("rejected_stale", stale.outcome)
        assertEquals("stale_draft", stale.error_code)
    }

    /** The executor refuses to pass [tag]'s page until the user has tapped it. */
    private fun consent(tag: String, next: String) {
        assertEquals("blocked", rig.navigate(next).outcome)
        rig.userTaps(tag)
        rig.reach(next)
    }
}
