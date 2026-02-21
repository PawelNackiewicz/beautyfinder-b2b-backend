package com.beautyfinder.b2b.config

import java.util.UUID

object TenantContext {
    private val currentTenant = ThreadLocal<UUID>()

    fun setSalonId(salonId: UUID) {
        currentTenant.set(salonId)
    }

    fun getSalonId(): UUID =
        currentTenant.get() ?: throw IllegalStateException("Tenant context not set")

    fun clear() {
        currentTenant.remove()
    }
}
