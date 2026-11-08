package org.ragbot.replica.bridge

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.ragbot.replica.MainActivity
import org.ragbot.replica.MockBank

/** One journey per service group, each ending where the user's own step begins. */
@RunWith(AndroidJUnit4::class)
class BridgeServicesTest {
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
        app.close()
    }

    @Test
    fun internalTransfer() {
        rig.reach("S-192")
        rig.fill(
            "S-192",
            "account-transfer-source" to text("account-1"),
            "account-transfer-destination" to text("0000000000077"),
            "account-transfer-amount" to rials(500_000),
            "account-transfer-reason" to text("0"),
        )
        rig.expect(rig.review(), "S-173")
    }

    @Test
    fun charity() {
        rig.reach("S-284", "charity" to "1")
        rig.fill("S-284", "charity-amount" to rials(100_000), "charity-source" to text("account-1"))
        rig.expect(rig.review(), "S-282")
    }

    /** A row key on a step the user's confirm would follow still stops before the user's step. */
    @Test
    fun loanRepaymentChoosesItsRowAndSourceAndStopsAtTheReview() {
        rig.reach("S-306", "loan" to "loan-personal")
        rig.fill("S-306", "loan-installment" to text("1"))
        rig.reach("S-331", "loan-source" to "account-1")
    }

    @Test
    fun loanRepaymentChoosesItsLoanRow() {
        rig.reach("S-306", "loan" to "loan-personal")
    }

    @Test
    fun walletToWallet() {
        rig.reach("S-116")
        rig.fill(
            "S-116",
            "wallet-op-source" to text("0000000000101"),
            "wallet-op-destination" to text("0000000000000201"),
            "wallet-op-amount" to rials(50_000),
        )
        rig.expect(rig.review(), "S-117")
    }

    /** The amount page holds the user's gateway step, so it is where the executor stops. */
    @Test
    fun accountFunding() {
        rig.reach("S-222")
        rig.fill("S-222", "funding-amount" to rials(1_000_000))
        rig.expect(rig.review(), "S-222")
    }

    @Test
    fun chequebookChoosesPaperOrDigital() {
        val paper = "AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_PAPER_CHECKBOOK"
        rig.reach("S-143", "cheque-kind" to paper)
        rig.fill("S-143", "cheque-account" to text("account-1"))
    }

    @Test
    fun sayadInquiry() {
        rig.reach("S-164")
        rig.fill("S-164", "svc-kind" to text("holder"))
        rig.reach("S-162")
        rig.fill("S-162", "svc-sayad" to text("0000000000000321"))
        rig.expect(rig.review(), "S-162")
    }

    @Test
    fun drivingViolations() {
        rig.reach("S-244")
        rig.fill("S-244", "svc-record" to text("plate-1"))
        rig.reach("S-245")
    }

    @Test
    fun paidCreditInquiryChoosesItsMenuEntry() {
        rig.reach("S-442", "inquiry-kind" to "bounced-cheques")
        rig.fill("S-442", "svc-source" to text("account-1"))
        rig.expect(rig.review(), "S-442")
    }

    @Test
    fun accountOpening() {
        rig.reach("S-062")
        rig.fill("S-062", "svc-kind" to text("saving"))
        rig.reach("S-054")
    }

    @Test
    fun wageAdvance() {
        rig.reach("S-414")
        rig.reach("S-419")
        rig.fill(
            "S-419",
            "svc-amount" to rials(1_000_000),
            "svc-target" to text("account-1"),
        )
        rig.reach("S-417")
    }

    @Test
    fun creditProduct() {
        rig.reach("S-407")
        rig.reach("S-406")
    }

    @Test
    fun rewardsTransfer() {
        rig.reach("S-428")
        rig.fill(
            "S-428",
            "svc-mobile" to text("09010000000"),
            "svc-points" to text("100"),
        )
        rig.expect(rig.review(), "S-427")
    }

    @Test
    fun financeBudget() {
        rig.reach("S-434")
    }

    @Test
    fun identifierConversion() {
        rig.reach("S-201")
    }
}
