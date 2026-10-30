package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class ShortcutRulesTest {
    @Test
    fun searchUsesCandidatesWhileBlankQueryShowsOnlyOrderedPins() {
        val pins = listOf("preview-statement", "preview-accounts")
        assertEquals(pins, ShortcutFixture.visible("", pins).map { it.id })
        assertEquals(pins, ShortcutFixture.visible("  ", pins).map { it.id })
        assertEquals(listOf("preview-cards"), ShortcutFixture.visible(" کارت ", pins).map { it.id })
        assertTrue(ShortcutFixture.visible("missing-service", pins).isEmpty())
    }

    @Test
    fun pinTogglePreservesOrderPreventsDuplicatesAndRejectsUnknownIds() {
        val pins = ShortcutFixture.initialPinnedIds
        val added = ShortcutFixture.toggle(pins, "preview-cards")
        assertEquals(pins + "preview-cards", added)
        assertEquals(pins, ShortcutFixture.toggle(added, "preview-cards"))
        assertEquals(pins, ShortcutFixture.toggle(pins, "unknown"))
        assertEquals(pins.drop(1), ShortcutFixture.toggle(pins, pins.first()))
        assertEquals(
            ShortcutFixture.services.size,
            ShortcutFixture.services.map { it.id }.distinct().size,
        )
    }
}
