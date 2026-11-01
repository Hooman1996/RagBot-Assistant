package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class VehicleRulesTest {
    @Test
    fun serviceKeysOpenTheirRecoveredSavedList() {
        assertEquals(
            VehicleDestinations.PLATES,
            VehicleRules.target("AUTHENTICATED_HOME_ANNUAL_TOLL"),
        )
        assertEquals(
            VehicleDestinations.CONTACTS,
            VehicleRules.target("AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY"),
        )
        assertEquals(
            VehicleDestinations.CONTACTS,
            VehicleRules.target("AUTHENTICATED_HOME_DRIVING_LICENSE_INQUIRY"),
        )
        assertEquals(
            VehicleDestinations.LICENCES,
            VehicleRules.target("AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY"),
        )
        assertEquals(
            VehicleDestinations.HISTORY_MENU,
            VehicleRules.target("AUTHENTICATED_HOME_PAYMENT_TOLL_HISTORY"),
        )
        VehicleFixture.menu.forEach {
            assertTrue(VehicleRules.target(it.first) in VehicleDestinations.implemented)
        }
    }

    @Test
    fun pollutionAreaShowsAutomobilePlatesOnlyAndDeletionHidesARecord() {
        assertEquals(
            listOf("plate-1", "plate-2"),
            VehicleRules.records("plate", "AUTHENTICATED_HOME_ANNUAL_TOLL", emptySet()).map {
                it.id
            },
        )
        assertEquals(
            listOf("plate-1"),
            VehicleRules.records("plate", "AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY", emptySet())
                .map { it.id },
        )
        assertTrue(VehicleRules.records("licence", "", setOf("licence-1")).isEmpty())
    }

    @Test
    fun ownershipAndPlatePartsFollowTheRecoveredTypes() {
        val (mine, others) = VehicleRules.records("plate", "", emptySet())
        assertEquals("(خودم)", VehicleRules.ownership(mine))
        assertEquals("(دیگران)", VehicleRules.ownership(others))
        assertEquals(4, mine.plate.size)
        assertFalse(others.automobile)
        assertEquals(2, others.plate.size)
        assertFalse("S-262" in HomeDestinations.implemented)
    }
}
