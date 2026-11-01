package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class TollRulesTest {
    private val plate = TollDraft(TollService.TRAFFIC, listOf("12", "ب", "345", "67"))

    @Test
    fun plateStringsHaveTheRecoveredLengths() {
        assertEquals("120234567", TollRules.plateNumber(plate))
        assertEquals(
            "12345678",
            TollRules.plateNumber(
                TollDraft(plate = listOf("123", "45678", "", ""), automobile = false)
            ),
        )
        assertEquals(
            mapOf("plate" to TollRules.PLATE_ERROR),
            TollRules.errors(plate.copy(plate = listOf("12", "", "345", "67"))),
        )
        // Recovered: the pollution inquiry accepts automobile plates only.
        assertEquals(
            setOf("plate"),
            TollRules.errors(
                    TollDraft(
                        TollService.POLLUTION,
                        listOf("123", "45678", "", ""),
                        automobile = false,
                    )
                )
                .keys,
        )
    }

    @Test
    fun othersNeedAValidNationalCodeAndMobileAndAnnualNeedsAVin() {
        assertTrue(TollRules.nationalCode("0000000019"))
        assertFalse(TollRules.nationalCode("0000000018"))
        assertFalse(TollRules.nationalCode("1111111111"))
        assertFalse(TollRules.nationalCode("000000019"))
        val others = plate.copy(mine = false)
        assertEquals(
            mapOf("code" to PaymentRules.MANDATORY, "phone" to PaymentRules.MANDATORY),
            TollRules.errors(others),
        )
        assertEquals(
            mapOf("code" to PaymentRules.INVALID, "phone" to PaymentRules.INVALID),
            TollRules.errors(others.copy(code = "0000000018", phone = "0812345678")),
        )
        assertTrue(
            TollRules.errors(others.copy(code = "۰۰۰۰۰۰۰۰۱۹", phone = "09123456789")).isEmpty()
        )
        assertEquals(
            mapOf("vin" to PaymentRules.MANDATORY),
            TollRules.errors(plate.copy(service = TollService.ANNUAL)),
        )
    }

    @Test
    fun duesTotalsFollowSelectionAndAnnualAddsItsCommission() {
        assertEquals(0L, TollRules.total(plate))
        assertEquals(
            325_000L,
            TollRules.total(plate.copy(selected = listOf("traffic-2", "traffic-3"))),
        )
        assertEquals(
            450_000L + 45_000L + 20_000L + 500_000L + TollFixture.COMMISSION,
            TollRules.total(plate.copy(service = TollService.ANNUAL)),
        )
        // Any other plate has no dues.
        assertTrue(TollRules.items(plate.copy(plate = listOf("11", "ب", "345", "67"))).isEmpty())
    }

    @Test
    fun onlyPollutionLacksAConfirmationAndMenuKeysMapToServices() {
        assertNull(TollService.POLLUTION.confirm)
        assertEquals(TollService.FREEWAY, TollService.ofKey("AUTHENTICATED_HOME_FREEWAY_TOLL"))
        assertNull(TollService.ofKey("AUTHENTICATED_HOME_TRANSFER_TOLL"))
        assertEquals(12, TollDestinations.implemented.size)
    }
}
