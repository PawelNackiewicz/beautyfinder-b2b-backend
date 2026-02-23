package com.beautyfinder.b2b.domain.billing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class InvoiceNumberTest {

    @Test
    fun `generate correctFormat`() {
        val number = InvoiceNumber.generate(UUID.randomUUID(), 2024, 3, 7)
        assertEquals("BF/2024/03/007", number.value)
    }

    @Test
    fun `generate singleDigitMonth padded`() {
        val number = InvoiceNumber.generate(UUID.randomUUID(), 2024, 3, 1)
        assertEquals("BF/2024/03/001", number.value)
    }

    @Test
    fun `generate sequencePadded`() {
        val number = InvoiceNumber.generate(UUID.randomUUID(), 2024, 12, 1)
        assertEquals("BF/2024/12/001", number.value)
    }

    @Test
    fun `parse validNumber returnsObject`() {
        val number = InvoiceNumber.parse("BF/2024/03/007")
        assertEquals("BF/2024/03/007", number.value)
    }

    @Test
    fun `parse invalidFormat throwsException`() {
        assertThrows<IllegalArgumentException> {
            InvoiceNumber.parse("INVALID/NUMBER")
        }
    }

    @Test
    fun `parse missingPadding throwsException`() {
        assertThrows<IllegalArgumentException> {
            InvoiceNumber.parse("BF/2024/3/7")
        }
    }
}
