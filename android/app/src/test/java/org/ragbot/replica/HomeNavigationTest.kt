package org.ragbot.replica

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HomeNavigationTest {
    @Test
    fun registryUsesExistingGraphIdsAndRejectsUnimplementedDestinations() {
        val graph = File("../../app_profiles/navigation-graph.json").readText()
        val ids =
            Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(graph).map { it.groupValues[1] }.toSet()
        val navigation = HomeNavigation()
        assertEquals(
            setOf(
                "S-026",
                "F-066",
                "S-063",
                "S-067",
                "S-085",
                "S-086",
                "S-090",
                "S-091",
                "S-014",
                "S-016",
                "S-012",
                "S-068",
                "S-119",
                "S-120",
                "S-125",
                "S-126",
                "S-130",
                "S-131",
                "S-134",
                "S-123",
                "S-124",
                "S-127",
                "S-128",
                "S-129",
                "S-118",
                "S-121",
                "S-122",
                "S-135",
                "S-136",
            ),
            HomeDestinations.implemented,
        )
        assertTrue(ids.containsAll(HomeDestinations.implemented))
        HomeDestinations.implemented.forEach { assertTrue(navigation.navigate(it)) }
        // Wallet-linked cards (FR-04c) and credit repayment (FR-11) are not implemented.
        listOf("S-137", "S-141", "S-132", "unknown", "submit", "authorize").forEach {
            assertFalse(navigation.navigate(it))
            assertEquals("S-026", navigation.destination)
        }
    }

    @Test
    fun unsupportedControlsLeaveHomeSelectedAndExplainTheLimit() {
        val navigation = HomeNavigation()
        assertNull(navigation.unavailable(HomeControl.HOME))
        HomeControl.entries
            .filter { it != HomeControl.HOME && it != HomeControl.BALANCE }
            .forEach {
                assertTrue(navigation.unavailable(it)!!.contains(it.title))
                assertTrue(navigation.unavailable(it)!!.contains("در خانه می‌مانید"))
                assertEquals(HomeDestinations.HOME, navigation.destination)
            }
    }
}
