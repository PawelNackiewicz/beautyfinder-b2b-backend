package com.beautyfinder.b2b.infrastructure.service

import com.beautyfinder.b2b.application.service.ServiceVariantDto
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ServiceCacheService {

    private data class CacheEntry(val dto: ServiceVariantDto, val expiresAt: Instant)

    private val cache = ConcurrentHashMap<UUID, CacheEntry>()

    companion object {
        private const val TTL_MINUTES = 10L
    }

    fun getVariant(variantId: UUID): ServiceVariantDto? {
        val entry = cache[variantId] ?: return null
        if (Instant.now().isAfter(entry.expiresAt)) {
            cache.remove(variantId)
            return null
        }
        return entry.dto
    }

    fun putVariant(variantId: UUID, dto: ServiceVariantDto) {
        cache[variantId] = CacheEntry(dto, Instant.now().plusSeconds(TTL_MINUTES * 60))
    }

    fun evictVariant(variantId: UUID) {
        cache.remove(variantId)
    }

    fun evictAllForSalon(salonId: UUID) {
        cache.entries.removeIf { it.value.dto.salonId == salonId }
    }
}
