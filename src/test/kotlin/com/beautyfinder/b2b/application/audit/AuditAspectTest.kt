package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.AuditContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.UUID

class AuditAspectTest {

    private val auditLogService = mockk<AuditLogService>(relaxed = true)
    private val aspect = AuditAspect(auditLogService)

    private val salonId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockkObject(TenantContext)
        mockkObject(AuditContextHolder)
        every { TenantContext.getSalonId() } returns salonId
        every { AuditContextHolder.get() } returns null
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TenantContext)
        unmockkObject(AuditContextHolder)
    }

    @Test
    fun `auditMethod logs entry after method execution`() {
        val joinPoint = mockJoinPoint(result = "ok", args = arrayOf())
        val audited = buildAudited(AuditAction.CLIENT_CREATED, "CLIENT")

        aspect.auditMethod(joinPoint, audited)

        verify(exactly = 1) { auditLogService.log(any()) }
    }

    @Test
    fun `auditMethod returns original result`() {
        val expected = "result-value"
        val joinPoint = mockJoinPoint(result = expected, args = arrayOf())
        val audited = buildAudited(AuditAction.CLIENT_CREATED, "CLIENT")

        val result = aspect.auditMethod(joinPoint, audited)

        assertEquals(expected, result)
    }

    @Test
    fun `auditMethod extracts salonId from TenantContext`() {
        val slot = slot<AuditLogEntry>()
        every { auditLogService.log(capture(slot)) } returns Unit

        val joinPoint = mockJoinPoint(result = null, args = arrayOf())
        val audited = buildAudited(AuditAction.CLIENT_CREATED, "CLIENT")

        aspect.auditMethod(joinPoint, audited)

        assertEquals(salonId, slot.captured.salonId)
    }

    @Test
    fun `auditMethod falls back to salonId arg when TenantContext missing`() {
        val argSalonId = UUID.randomUUID()
        every { TenantContext.getSalonId() } throws IllegalStateException("no tenant")

        val slot = slot<AuditLogEntry>()
        every { auditLogService.log(capture(slot)) } returns Unit

        val joinPoint = mockJoinPoint(
            result = null,
            args = arrayOf(argSalonId),
            paramNames = arrayOf("salonId"),
        )
        val audited = buildAudited(AuditAction.CLIENT_CREATED, "CLIENT")

        aspect.auditMethod(joinPoint, audited)

        assertEquals(argSalonId, slot.captured.salonId)
    }

    @Test
    fun `auditMethod does not propagate logging errors`() {
        every { auditLogService.log(any()) } throws RuntimeException("log failed")

        val joinPoint = mockJoinPoint(result = "ok", args = arrayOf())
        val audited = buildAudited(AuditAction.CLIENT_CREATED, "CLIENT")

        // Should not throw
        val result = aspect.auditMethod(joinPoint, audited)
        assertEquals("ok", result)
    }

    @Test
    fun `auditMethod extracts resourceId from result with id field`() {
        val resourceId = UUID.randomUUID()
        val resultDto = ResultWithId(id = resourceId, name = "test")

        val slot = slot<AuditLogEntry>()
        every { auditLogService.log(capture(slot)) } returns Unit

        val joinPoint = mockJoinPoint(result = resultDto, args = arrayOf())
        val audited = buildAudited(AuditAction.CLIENT_CREATED, "CLIENT")

        aspect.auditMethod(joinPoint, audited)

        assertEquals(resourceId, slot.captured.resourceId)
    }

    private fun mockJoinPoint(
        result: Any?,
        args: Array<Any?>,
        paramNames: Array<String> = emptyArray(),
    ): ProceedingJoinPoint {
        val joinPoint = mockk<ProceedingJoinPoint>()
        val signature = mockk<MethodSignature>()
        val method = mockk<Method>()

        every { joinPoint.proceed() } returns result
        every { joinPoint.args } returns args
        every { joinPoint.signature } returns signature
        every { signature.declaringTypeName } returns "TestClass"
        every { signature.name } returns "testMethod"
        every { signature.method } returns method
        every { signature.parameterNames } returns paramNames
        every { method.parameters } returns args.indices.map { mockk<Parameter>(relaxed = true) }.toTypedArray()

        return joinPoint
    }

    private fun buildAudited(action: AuditAction, resourceType: String): Audited {
        val audited = mockk<Audited>()
        every { audited.action } returns action
        every { audited.resourceType } returns resourceType
        every { audited.description } returns ""
        return audited
    }

    data class ResultWithId(val id: UUID, val name: String)
}
