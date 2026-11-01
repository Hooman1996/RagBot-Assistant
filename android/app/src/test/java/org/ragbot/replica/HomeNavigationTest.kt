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
                "S-066",
                "S-084",
                "S-197",
                "S-198",
                "S-093",
                "S-097",
                "S-098",
                "S-173",
                "S-174",
                "S-175",
                "S-176",
                "S-191",
                "S-192",
                "S-193",
                "S-087",
                "S-088",
                "S-089",
                "S-096",
                "S-182",
                "S-184",
                "S-185",
                "S-186",
                "S-187",
                "S-188",
                "S-189",
                "S-190",
                "S-177",
                "S-178",
                "S-179",
                "S-180",
                "S-181",
                "S-200",
                "S-092",
                "S-132",
                "S-133",
                "S-205",
                "S-206",
                "S-207",
                "S-208",
                "S-209",
                "S-210",
                "S-401",
                "S-402",
                "S-403",
                "S-404",
                "S-405",
                "S-094",
                "S-095",
                "S-211",
                "S-212",
                "S-213",
                "S-214",
                "S-215",
                "S-216",
                "S-282",
                "S-283",
                "S-284",
                "S-110",
                "S-111",
                "S-112",
                "S-113",
                "S-114",
                "S-115",
                "S-324",
                "S-338",
                "S-306",
                "S-331",
                "S-307",
                "S-309",
                "S-201",
                "S-202",
                "S-203",
                "S-204",
                "S-011",
                "S-116",
                "S-117",
                "S-100",
                "S-101",
                "S-106",
                "S-107",
                "S-104",
                "S-105",
                "S-102",
                "S-103",
                "S-109",
                "S-099",
            ),
            HomeDestinations.implemented,
        )
        assertTrue(ids.containsAll(HomeDestinations.implemented))
        HomeDestinations.implemented.forEach { assertTrue(navigation.navigate(it)) }
        // Host container S-138 and received-card confirmation S-004 are not pages.
        // S-183 is a wage-advance receipt, not part of the group-transfer journey.
        listOf(
                "S-183",
                // S-199 hosts only the S-200 repetitive-transactions route.
                "S-199",
                "S-194",
                "S-195",
                "S-138",
                "S-083",
                "S-196",
                "S-004",
                // Loan application host, recovered picker and deferred calculation are not pages
                // here.
                "S-288",
                "S-330",
                "S-308",
                // The fast-payment QR scanner is deferred by D-021.
                "S-108",
                "unknown",
                "submit",
                "authorize",
            )
            .forEach {
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
                        HomeControl.SEARCH,
                        HomeControl.SHORTCUTS,
                    )
            }
            .forEach {
                assertTrue(navigation.unavailable(it)!!.contains(it.title))
                assertTrue(navigation.unavailable(it)!!.contains("در خانه می‌مانید"))
                assertEquals(HomeDestinations.HOME, navigation.destination)
            }
    }
}
