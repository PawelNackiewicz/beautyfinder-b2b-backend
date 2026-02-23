package com.beautyfinder.b2b.application

import com.beautyfinder.b2b.api.AuthResponse
import com.beautyfinder.b2b.api.LoginRequest
import com.beautyfinder.b2b.application.audit.AuditLogEntry
import com.beautyfinder.b2b.application.audit.AuditLogService
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.infrastructure.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val auditLogService: AuditLogService,
    @Value("\${app.jwt.expiration}") private val jwtExpiration: Long,
) {

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            auditLogService.log(
                AuditLogEntry(
                    salonId = user.salonId,
                    action = AuditAction.LOGIN_FAILED,
                    resourceType = "USER",
                    resourceId = user.id,
                    resourceDescription = request.email,
                ),
            )
            throw IllegalArgumentException("Invalid email or password")
        }

        val token = jwtService.generateToken(user.id!!, user.salonId, user.role.name)

        auditLogService.log(
            AuditLogEntry(
                salonId = user.salonId,
                action = AuditAction.LOGIN_SUCCESS,
                resourceType = "USER",
                resourceId = user.id,
                resourceDescription = request.email,
            ),
        )

        return AuthResponse(
            token = token,
            role = user.role.name,
            salonId = user.salonId,
            expiresIn = jwtExpiration,
        )
    }
}
