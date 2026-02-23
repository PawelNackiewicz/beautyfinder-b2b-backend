package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(10)
class AuditContextFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val auth = SecurityContextHolder.getContext().authentication

            val actorId = auth?.principal?.let {
                try { UUID.fromString(it.toString()) } catch (_: Exception) { null }
            }
            val actorRole = auth?.authorities?.firstOrNull()?.authority?.removePrefix("ROLE_")

            val ipAddress = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                ?: request.remoteAddr
            val userAgent = request.getHeader("User-Agent")

            val salonId = try {
                com.beautyfinder.b2b.config.TenantContext.getSalonId()
            } catch (_: Exception) { null }

            if (salonId != null) {
                AuditContextHolder.set(
                    AuditContext(
                        salonId = salonId,
                        actorId = actorId,
                        actorEmail = null,
                        actorRole = actorRole,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                    ),
                )
            }

            filterChain.doFilter(request, response)
        } finally {
            AuditContextHolder.clear()
        }
    }
}
