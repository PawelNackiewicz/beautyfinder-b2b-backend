package com.beautyfinder.b2b.domain.audit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangedFieldsTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `of creates correct changes`() {
        val changed = ChangedFields.of(
            "name" to ("Old" to "New"),
            "price" to (50 to 100),
        )
        assertEquals(2, changed.changes.size)
        assertEquals("name", changed.changes[0].field)
        assertEquals("Old", changed.changes[0].oldValue)
        assertEquals("New", changed.changes[0].newValue)
    }

    @Test
    fun `toJson serializes correctly`() {
        val changed = ChangedFields.of("name" to ("Old" to "New"))
        val json = changed.toJson()
        val parsed = objectMapper.readTree(json)
        assertTrue(parsed.isArray)
        assertEquals("name", parsed[0]["field"].asText())
        assertEquals("Old", parsed[0]["oldValue"].asText())
        assertEquals("New", parsed[0]["newValue"].asText())
    }

    @Test
    fun `toJson handles null values`() {
        val changed = ChangedFields.of("name" to (null to "New"))
        val json = changed.toJson()
        val parsed = objectMapper.readTree(json)
        assertTrue(parsed[0]["oldValue"].isNull)
        assertEquals("New", parsed[0]["newValue"].asText())
    }

    @Test
    fun `empty returns empty list`() {
        val changed = ChangedFields.empty()
        assertEquals(0, changed.changes.size)
    }

    @Test
    fun `of multiple changes all present`() {
        val changed = ChangedFields.of(
            "a" to (1 to 2),
            "b" to ("x" to "y"),
            "c" to (true to false),
        )
        assertEquals(3, changed.changes.size)
        assertEquals("a", changed.changes[0].field)
        assertEquals("b", changed.changes[1].field)
        assertEquals("c", changed.changes[2].field)
    }
}
