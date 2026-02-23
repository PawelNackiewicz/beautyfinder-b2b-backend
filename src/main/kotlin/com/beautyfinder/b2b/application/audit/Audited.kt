package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Audited(
    val action: AuditAction,
    val resourceType: String,
    val description: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuditResourceId
