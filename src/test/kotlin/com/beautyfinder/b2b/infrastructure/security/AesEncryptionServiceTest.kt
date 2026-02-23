package com.beautyfinder.b2b.infrastructure.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import javax.crypto.AEADBadTagException

class AesEncryptionServiceTest {

    // 32-byte test key encoded as base64
    private val testKeyBase64 = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 42).toByte() })
    private val service = AesEncryptionService(testKeyBase64)

    @Test
    fun `encrypt_decrypt_roundtrip`() {
        val plaintext = "Hello, World! Zażółć gęślą jaźń."
        val encrypted = service.encrypt(plaintext)
        val decrypted = service.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt_producesDifferentCiphertext_forSameInput`() {
        val plaintext = "same input"
        val encrypted1 = service.encrypt(plaintext)
        val encrypted2 = service.encrypt(plaintext)
        assertNotEquals(encrypted1, encrypted2, "Two encryptions should produce different ciphertext due to random IV")
    }

    @Test
    fun `decrypt_tamperedCiphertext_throwsException`() {
        val plaintext = "sensitive data"
        val encrypted = service.encrypt(plaintext)
        val bytes = Base64.getDecoder().decode(encrypted)
        // Tamper with a byte in the middle
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 0xFF).toByte()
        val tampered = Base64.getEncoder().encodeToString(bytes)

        assertThrows<AEADBadTagException> {
            service.decrypt(tampered)
        }
    }

    @Test
    fun `hash_deterministic`() {
        val input = "test input"
        val hash1 = service.hash(input)
        val hash2 = service.hash(input)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hash_differentInputs_differentOutputs`() {
        val hash1 = service.hash("input1")
        val hash2 = service.hash("input2")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `encrypt_emptyString`() {
        assertDoesNotThrow {
            val encrypted = service.encrypt("")
            val decrypted = service.decrypt(encrypted)
            assertEquals("", decrypted)
        }
    }

    @Test
    fun `encrypt_largePayload`() {
        val largePayload = "A".repeat(10_000) // 10KB
        val encrypted = service.encrypt(largePayload)
        val decrypted = service.decrypt(encrypted)
        assertEquals(largePayload, decrypted)
    }
}
