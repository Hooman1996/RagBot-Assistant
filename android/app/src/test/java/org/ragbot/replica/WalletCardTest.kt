package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class WalletCardTest {
    @Test
    fun walletGroupHasItsOwnCardAndRowsOpenImplementedWalletPages() {
        val wallets = CardFixture.of(CardKind.WALLET)
        assertEquals(listOf("card-8"), wallets.map { it.key })
        assertEquals(2_400_000L, wallets.single().cardBalance)
        assertNull(wallets.single().accountBalance)
        val rows = CardFixture.actions.getValue(CardKind.WALLET).associateBy { it.key }
        assertEquals(WalletDestinations.SEND_TYPE, rows.getValue("wallet-issue").destination)
        assertEquals(WalletDestinations.PIN, rows.getValue("wallet-pin").destination)
        assertTrue(HomeDestinations.implemented.containsAll(WalletDestinations.implemented))
        assertFalse("S-138" in HomeDestinations.implemented)
        assertEquals(120_000L, WALLET_CARD_FEE)
    }
}
