package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class SavedDestinationRulesTest {
    @Test
    fun typeAndQueryFilteringPreserveRecordIdentityAndOrder() {
        val records = SavedDestinationFixture.records
        assertEquals(
            listOf("loan-2"),
            SavedDestinationFixture.visible(records, "LOAN", " 171248107569 ").map { it.id },
        )
        val accounts = SavedDestinationFixture.visible(records, "ACCOUNT", "")
        assertEquals(records.filter { it.type == "ACCOUNT" }, accounts)
        assertTrue(accounts.none { it.type == "LOAN" })
        assertTrue(SavedDestinationFixture.visible(records, "CARD", "not-a-record").isEmpty())
        assertEquals(records.size, records.map { it.id }.distinct().size)
    }

    @Test
    fun localValidationRejectsMissingAndDuplicateDestinationsWithoutRejectingUnchangedEdit() {
        val records = SavedDestinationFixture.records
        val account = records.first { it.type == "ACCOUNT" }
        assertNotNull(
            SavedDestinationFixture.validation(records, null, "ACCOUNT", "", account.destination)
        )
        assertNotNull(
            SavedDestinationFixture.validation(
                records,
                null,
                "ACCOUNT",
                "نام نمونه",
                account.destination,
            )
        )
        assertNull(
            SavedDestinationFixture.validation(
                records,
                account.id,
                account.type,
                account.name,
                account.destination,
            )
        )
        assertNotNull(
            SavedDestinationFixture.validation(records, null, "LOAN", "نام نمونه", "DEMO-new")
        )
        assertNotNull(
            SavedDestinationFixture.validation(records, null, "ACCOUNT", "نام نمونه", "123")
        )
        assertNotNull(
            SavedDestinationFixture.validation(records, null, "WALLET", "نام نمونه", "123")
        )
    }

    @Test
    fun creationKeepsUniqueIdentityAfterAnEditAndUnrelatedDeletion() {
        val original = SavedDestinationFixture.records
        val firstId = SavedDestinationFixture.nextId(original)
        val created = SavedDestination(firstId, "ACCOUNT", "local", "0281707721676")
        val edited = created.copy(destination = "0162718460824")
        val afterDeletion = original.dropLast(1) + edited
        val nextId = SavedDestinationFixture.nextId(afterDeletion)
        assertNotEquals(firstId, nextId)
        assertFalse(afterDeletion.any { it.id == nextId })
        val added = afterDeletion + created.copy(id = nextId)
        assertEquals(added.size, added.map { it.id }.distinct().size)
    }
}
