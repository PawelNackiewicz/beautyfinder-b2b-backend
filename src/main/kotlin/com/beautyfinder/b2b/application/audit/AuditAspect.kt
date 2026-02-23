package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.config.TenantContext
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Aspect
@Component
class AuditAspect(
    private val auditLogService: AuditLogService,
) {

    private val log = LoggerFactory.getLogger(AuditAspect::class.java)

    @Around("@annotation(audited)")
    fun auditMethod(joinPoint: ProceedingJoinPoint, audited: Audited): Any? {
        val result = joinPoint.proceed()

        try {
            val salonId = try {
                TenantContext.getSalonId()
            } catch (_: Exception) {
                extractSalonIdFromArgs(joinPoint) ?: return result
            }

            val resourceId = extractResourceId(joinPoint) ?: extractIdFromResult(result)

            auditLogService.log(
                AuditLogEntry(
                    salonId = salonId,
                    action = audited.action,
                    resourceType = audited.resourceType,
                    resourceId = resourceId,
                    resourceDescription = audited.description.ifEmpty { null },
                ),
            )
        } catch (e: Exception) {
            log.error("Audit aspect failed for {}.{}", joinPoint.signature.declaringTypeName, joinPoint.signature.name, e)
        }

        return result
    }

    private fun extractResourceId(joinPoint: ProceedingJoinPoint): UUID? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val parameters = method.parameters
        val args = joinPoint.args

        for (i in parameters.indices) {
            if (parameters[i].isAnnotationPresent(AuditResourceId::class.java)) {
                val arg = args[i]
                if (arg is UUID) return arg
            }
        }
        return null
    }

    private fun extractIdFromResult(result: Any?): UUID? {
        if (result == null) return null
        return try {
            val idField = result::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.get(result) as? UUID
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSalonIdFromArgs(joinPoint: ProceedingJoinPoint): UUID? {
        val signature = joinPoint.signature as MethodSignature
        val paramNames = signature.parameterNames
        val args = joinPoint.args

        for (i in paramNames.indices) {
            if (paramNames[i] == "salonId" && args[i] is UUID) {
                return args[i] as UUID
            }
        }
        return null
    }
}
