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
                "S-137",
                "S-139",
                "S-140",
                "S-141",
                "S-064",
                "S-065",
                "S-069",
                "S-025",
                "S-070",
                "S-071",
                "S-072",
                "S-073",
                "S-074",
                "S-463",
                "S-464",
                "S-075",
                "S-076",
                "S-077",
                "S-078",
                "S-079",
                "S-080",
                "S-081",
                "S-082",
            ),
            HomeDestinations.implemented,
        )
        assertTrue(ids.containsAll(HomeDestinations.implemented))
        HomeDestinations.implemented.forEach { assertTrue(navigation.navigate(it)) }
        // Host container S-138, received-card confirmation S-004 and repayment S-132 are not pages.
        listOf("S-138", "S-004", "S-132", "unknown", "submit", "authorize").forEach {
            assertFalse(navigation.navigate(it))
            assertEquals("S-026", navigation.destination)
        }
    }

    @Test
    fun unsupportedControlsLeaveHomeSelectedAndExplainTheLimit() {
        val navigation = HomeNavigation()
        assertNull(navigation.unavailable(HomeControl.HOME))
        HomeControl.entries
            .filter {
                it !in
                    setOf(
                        HomeControl.HOME,
                        HomeControl.BALANCE,
                        HomeControl.NOTIFICATIONS,
                        HomeControl.SUPPORT,
                    )
            }
            .forEach {
                assertTrue(navigation.unavailable(it)!!.contains(it.title))
                assertTrue(navigation.unavailable(it)!!.contains("در خانه می‌مانید"))
                assertEquals(HomeDestinations.HOME, navigation.destination)
            }
    }
}
