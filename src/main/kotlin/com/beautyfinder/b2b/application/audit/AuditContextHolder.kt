package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditContext

object AuditContextHolder {
    private val context = ThreadLocal<AuditContext?>()

    fun set(ctx: AuditContext) {
        context.set(ctx)
    }

    fun get(): AuditContext? = context.get()

    fun clear() {
        context.remove()
    }
}
