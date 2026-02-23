package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.config.TenantContext
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class AuditContextFilterTest {

    private val filter = AuditContextFilter()
    private val salonId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockkObject(TenantContext)
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TenantContext)
        SecurityContextHolder.clearContext()
        AuditContextHolder.clear()
    }

    @Test
    fun `sets audit context with authenticated user`() {
        val actorId = UUID.randomUUID()
        every { TenantContext.getSalonId() } returns salonId

        val auth = UsernamePasswordAuthenticationToken(
            actorId.toString(), null, listOf(SimpleGrantedAuthority("ROLE_OWNER")),
        )
        SecurityContextHolder.getContext().authentication = auth

        val request = MockHttpServletRequest()
        request.remoteAddr = "192.168.1.1"
        request.addHeader("User-Agent", "TestBrowser/1.0")

        var capturedContext: com.beautyfinder.b2b.domain.audit.AuditContext? = null
        val chain = FilterChain { _, _ -> capturedContext = AuditContextHolder.get() }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertNotNull(capturedContext)
        assertEquals(actorId, capturedContext!!.actorId)
        assertEquals("OWNER", capturedContext!!.actorRole)
        assertEquals("192.168.1.1", capturedContext!!.ipAddress)
        assertEquals("TestBrowser/1.0", capturedContext!!.userAgent)
    }

    @Test
    fun `uses X-Forwarded-For header for IP`() {
        every { TenantContext.getSalonId() } returns salonId
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, emptyList())

        val request = MockHttpServletRequest()
        request.remoteAddr = "10.0.0.1"
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18")

        var capturedContext: com.beautyfinder.b2b.domain.audit.AuditContext? = null
        val chain = FilterChain { _, _ -> capturedContext = AuditContextHolder.get() }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertNotNull(capturedContext)
        assertEquals("203.0.113.50", capturedContext!!.ipAddress)
    }

    @Test
    fun `clears context after filter chain`() {
        every { TenantContext.getSalonId() } returns salonId
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, emptyList())

        val request = MockHttpServletRequest()
        val chain = FilterChain { _, _ -> }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertNull(AuditContextHolder.get())
    }

    @Test
    fun `clears context even when exception in chain`() {
        every { TenantContext.getSalonId() } returns salonId
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, emptyList())

        val request = MockHttpServletRequest()
        val chain = FilterChain { _, _ -> throw RuntimeException("error") }

        try {
            filter.doFilter(request, MockHttpServletResponse(), chain)
        } catch (_: RuntimeException) {
            // expected
        }

        assertNull(AuditContextHolder.get())
    }

    @Test
    fun `does not set context when no tenant`() {
        every { TenantContext.getSalonId() } throws IllegalStateException("no tenant")

        val request = MockHttpServletRequest()
        var capturedContext: com.beautyfinder.b2b.domain.audit.AuditContext? = null
        val chain = FilterChain { _, _ -> capturedContext = AuditContextHolder.get() }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertNull(capturedContext)
    }
}
