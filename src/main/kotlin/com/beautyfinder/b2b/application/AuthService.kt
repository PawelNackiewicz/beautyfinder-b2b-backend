package com.beautyfinder.b2b.application

import com.beautyfinder.b2b.api.AuthResponse
import com.beautyfinder.b2b.api.LoginRequest
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.infrastructure.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.jwt.expiration}") private val jwtExpiration: Long,
) {

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        val token = jwtService.generateToken(user.id!!, user.salonId, user.role.name)

        return AuthResponse(
            token = token,
            role = user.role.name,
            salonId = user.salonId,
            expiresIn = jwtExpiration,
        )
    }
}
