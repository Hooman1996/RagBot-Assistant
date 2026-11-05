package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class MockDataTest {
    private fun error(text: String) =
        assertThrows(MockDataError::class.java) { Json.parse("x.json", text)["a"].long() }

    @Test
    fun theFilesLoadWithTheirExactAmounts() {
        val files = MockData.current
        assertEquals("رضا", files.people.single().name)
        assertEquals(12_500_000L, files.accounts.first().balance)
        assertEquals(4, files.destinations.count { it.name == "رضا" })
    }

    @Test
    fun aFractionIsRejectedNotRounded() {
        assertTrue(error("{\"a\": 1.5}").message!!.isNotEmpty())
    }

    @Test
    fun aMissingFieldAndAWrongTypeNameThePlace() {
        assertTrue("x.json.a" in error("{}").message!!)
        assertTrue("x.json.a" in error("{\"a\": \"1\"}").message!!)
    }

    @Test
    fun parsingKeepsPersianTextEscapesAndNulls() {
        val json = Json.parse("x.json", "{\"a\": [\"رضا\\n\", null], \"b\": true}")
        assertEquals(listOf("رضا\n", null), json["a"].list { if (it.isNull) null else it.string() })
        assertTrue(json.bool("b"))
    }
}
