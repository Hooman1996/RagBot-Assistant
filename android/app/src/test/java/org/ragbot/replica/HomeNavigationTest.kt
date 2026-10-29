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
        assertEquals(setOf("S-026", "F-066"), HomeDestinations.implemented)
        assertTrue(ids.containsAll(HomeDestinations.implemented))
        HomeDestinations.implemented.forEach { assertTrue(navigation.navigate(it)) }
        listOf("S-085", "S-125", "unknown", "submit", "authorize").forEach {
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
