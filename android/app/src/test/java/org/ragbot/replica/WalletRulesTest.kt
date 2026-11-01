package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class WalletRulesTest {
    private fun row(type: WalletTranType, credit: Boolean) =
        WalletStatementRow(
            "row",
            100_000L,
            credit,
            1_000_000L,
            type,
            "نوع",
            "1405/08/01",
            "10:15",
            "TRACE",
            "TX",
            "0000000000001",
            "طرف",
            "یادداشت",
        )

    @Test
    fun moreThanFiveWalletsShowOnlyTheFirstFour() {
        val wallet = WalletFixture.wallets.first()
        val five = List(5) { wallet.copy(number = "$it") }
        assertEquals(five, WalletRules.shown(five))
        val six = five + wallet.copy(number = "5")
        assertEquals(listOf("0", "1", "2", "3"), WalletRules.shown(six).map { it.number })
    }

    @Test
    fun pickersListOnlyActiveWallets() {
        assertEquals(
            listOf("0000000000101", "0000000000102"),
            WalletRules.active(WalletFixture.wallets).map { it.number },
        )
        assertEquals(3, WalletRules.shown(WalletFixture.wallets).size)
    }

    @Test
    fun qrContentIsTheRecoveredJsonWithAnOptionalAmount() {
        assertEquals(
            "{\n  \"amount\": null,\n  \"destination\": \"0000000000101\"\n}",
            WalletRules.qrContent("0000000000101", null),
        )
        assertEquals(
            "{\n  \"amount\": \"250000\",\n  \"destination\": \"0000000000101\"\n}",
            WalletRules.qrContent("0000000000101", "250000"),
        )
    }

    @Test
    fun detailRowsFollowTheTransactionType() {
        assertEquals(
            listOf("مبدا:", "نام", "کد رهگیری:", "یادداشت:"),
            WalletRules.detailFields(row(WalletTranType.CHARGE, true)).map { it.first },
        )
        assertEquals(
            "مقصد:",
            WalletRules.detailFields(row(WalletTranType.TRANSFER, false)).first().first,
        )
        assertEquals(
            listOf("نام", "کد رهگیری:", "یادداشت:"),
            WalletRules.detailFields(row(WalletTranType.WALLET_BILL_PAYMENT, false)).map {
                it.first
            },
        )
        assertEquals(
            listOf("کد رهگیری:", "یادداشت:"),
            WalletRules.detailFields(row(WalletTranType.IPG, false)).map { it.first },
        )
        assertEquals(
            listOf("مقصد:", "نام", "کد رهگیری:"),
            WalletRules.detailFields(row(WalletTranType.WALLET_MOBILE_CREDIT, false)).map {
                it.first
            },
        )
    }

    @Test
    fun menuItemsOpenOnlyRecoveredTargets() {
        assertEquals(
            listOf("S-115", "S-110", "S-112", "S-100", "S-109", "S-102"),
            WalletFixture.menu.mapNotNull { it.destination },
        )
        // The charge action opens its payment-type sheet instead of a page.
        assertEquals(listOf(null, "S-106", "S-116"), WalletFixture.actions.map { it.destination })
        assertTrue(WalletFixture.history.all { it.destination == AccountDestinations.HISTORY })
        assertEquals(10, WalletRules.PAGE_SIZE)
    }
}
