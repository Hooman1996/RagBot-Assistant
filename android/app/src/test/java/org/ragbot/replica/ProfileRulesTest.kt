package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class ProfileRulesTest {
    @Test
    fun credentialRulesFollowTheRecoveredHints() {
        assertTrue(ProfileRules.password("Abcdef1!"))
        assertFalse(ProfileRules.password("abcdef1!"))
        assertFalse(ProfileRules.password("Abcdefg1"))
        assertFalse(ProfileRules.password("Ab1!"))
        assertFalse(ProfileRules.password("Abcdef1!#"))
        assertTrue(ProfileRules.username("user.name1"))
        assertFalse(ProfileRules.username(".username"))
        assertFalse(ProfileRules.username("user"))
        assertTrue(ProfileRules.mobile("۰۹۱۲۰۰۰۰۰۰۰"))
        assertFalse(ProfileRules.mobile("0812000000"))
        assertTrue(ProfileRules.email("a@b.ir"))
        assertFalse(ProfileRules.email("a@b"))
        assertTrue(ProfileRules.postalCode("۱۲۳۴۵۶۷۸۹۰"))
        assertFalse(ProfileRules.postalCode("123"))
        assertTrue(ProfileRules.englishName("Ali Reza"))
        assertFalse(ProfileRules.englishName("علی"))
        assertEquals(
            setOf("current", "new", "retype"),
            credentialErrors("password", "", "x", "y").keys,
        )
        assertEquals(emptyMap<String, String>(), credentialErrors("email", "a@b.ir", "", ""))
    }

    @Test
    fun profileAndSettingsMenusOpenOnlyRegisteredPages() {
        val all = (ProfileFixture.menu + ProfileFixture.settings).mapNotNull { it.destination }
        assertTrue(HomeDestinations.implemented.containsAll(all))
        assertTrue(ProfileFixture.menu.any { it.destination == null })
        assertTrue(HomeDestinations.implemented.containsAll(ProfileDestinations.implemented))
    }
}
