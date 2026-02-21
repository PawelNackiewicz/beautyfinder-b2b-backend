package com.beautyfinder.b2b.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private val jwtService = JwtService(
        secret = "test-secret-key-that-is-at-least-256-bits-long!!",
        expiration = 86400000,
    )

    private val userId = UUID.randomUUID()
    private val salonId = UUID.randomUUID()
    private val role = "OWNER"

    @Test
    fun `generateToken returns non-empty string`() {
        val token = jwtService.generateToken(userId, salonId, role)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `validateToken returns true for fresh token`() {
        val token = jwtService.generateToken(userId, salonId, role)
        assertTrue(jwtService.validateToken(token))
    }

    @Test
    fun `validateToken returns false for invalid token`() {
        assertFalse(jwtService.validateToken("invalid.token.here"))
    }

    @Test
    fun `extractSalonId returns correct salonId from token`() {
        val token = jwtService.generateToken(userId, salonId, role)
        assertEquals(salonId, jwtService.extractSalonId(token))
    }

    @Test
    fun `extractUserId returns correct userId from token`() {
        val token = jwtService.generateToken(userId, salonId, role)
        assertEquals(userId, jwtService.extractUserId(token))
    }

    @Test
    fun `extractRole returns correct role from token`() {
        val token = jwtService.generateToken(userId, salonId, role)
        assertEquals(role, jwtService.extractRole(token))
    }
}
