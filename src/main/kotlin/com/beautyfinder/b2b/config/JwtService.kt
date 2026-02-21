package com.beautyfinder.b2b.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration}") private val expiration: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: UUID, salonId: UUID, role: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("salonId", salonId.toString())
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        try {
            extractAllClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    fun extractUserId(token: String): UUID =
        UUID.fromString(extractAllClaims(token).subject)

    fun extractSalonId(token: String): UUID =
        UUID.fromString(extractAllClaims(token)["salonId"] as String)

    fun extractRole(token: String): String =
        extractAllClaims(token)["role"] as String

    private fun extractAllClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
