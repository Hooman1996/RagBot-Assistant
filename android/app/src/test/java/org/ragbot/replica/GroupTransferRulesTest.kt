package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class GroupTransferRulesTest {
    private fun file(key: String) = GroupTransferFixture.files.first { it.key == key }

    private fun rows(key: String) = GroupTransferRules.parse(file(key).sheet)

    @Test
    fun parserSkipsHeaderAndBlankRowsAndKeepsRecoveredColumns() {
        val parsed =
            GroupTransferRules.parse(
                listOf(
                    listOf("ردیف", "شبا", "مبلغ", "بابت", "شناسه", "شرح"),
                    listOf("1", "IR920000000000000000000002", "150000", "بابت", "12", "شرح"),
                    listOf(null, " ", "", null, "", ""),
                    listOf("3", "650000000000000000000003", "250000"),
                )
            )
        assertEquals(listOf(1L, 3L), parsed.map { it.id })
        assertEquals(
            GroupRow(1, "920000000000000000000002", "150000", "بابت", "12", "شرح"),
            parsed[0],
        )
        assertEquals(GroupRow(3, "650000000000000000000003", "250000"), parsed[1])
    }

    @Test
    fun fileRecordCountMustBeBetweenOneAndFiveHundred() {
        assertNull(GroupTransferRules.fileError(rows("valid")))
        assertNotNull(GroupTransferRules.fileError(rows("empty")))
        assertEquals(501, rows("large").size)
        assertNotNull(GroupTransferRules.fileError(rows("large")))
        assertNull(GroupTransferRules.fileError(rows("large").take(500)))
    }

    @Test
    fun invalidFixtureRowsAreFlaggedAndTotalsAreExactRials() {
        val invalid = rows("invalid")
        assertEquals(
            listOf(2L, 4L, 5L),
            invalid.filter { GroupTransferRules.rowError(it) != null }.map { it.id },
        )
        assertEquals(800_000L, GroupTransferRules.total(invalid.filter { it.id != 4L }))
        assertEquals(500_000L, GroupTransferRules.total(rows("valid")))
        val huge = GroupRow(1, amount = Long.MAX_VALUE.toString())
        assertNull(GroupTransferRules.total(listOf(huge, huge)))
        listOf("-1", "1.5", "۱۰۰٬۰۰۰", "").forEach {
            assertNotNull(it, GroupTransferRules.rowError(rows("valid")[0].copy(amount = it)))
        }
        assertNull(GroupTransferRules.rowError(rows("valid")[0].copy(amount = "۱۰۰۰۰۰")))
        assertNotNull(
            GroupTransferRules.rowError(rows("valid")[0].copy(paymentId = "1".repeat(31)))
        )
    }

    @Test
    fun acceptRequiresValidRowsAndMoreThanOneDestination() {
        val valid = rows("valid")
        assertNull(GroupTransferRules.acceptError(valid))
        assertEquals(
            GroupTransferRules.SINGLE_WARNING,
            GroupTransferRules.acceptError(valid.take(1)),
        )
        assertNotNull(GroupTransferRules.acceptError(rows("invalid")))
        assertNull(
            GroupTransferRules.acceptError(
                rows("invalid").filter { GroupTransferRules.rowError(it) == null }
            )
        )
    }

    @Test
    fun firstPageNeedsKnownSourceAndPositiveTotal() {
        assertNull(GroupTransferRules.firstPageError(GroupTransferDraft(total = "500000")))
        assertNotNull(GroupTransferRules.firstPageError(GroupTransferDraft(total = "0")))
        assertNotNull(
            GroupTransferRules.firstPageError(GroupTransferDraft(sourceId = "x", total = "1"))
        )
    }
}
