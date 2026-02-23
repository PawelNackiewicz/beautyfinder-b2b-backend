package com.beautyfinder.b2b.infrastructure.salon

import com.beautyfinder.b2b.application.salon.SalonSettingsDto
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CachedEntry<T>(
    val value: T,
    val cachedAt: Instant,
    val ttlSeconds: Long = 300,
) {
    val isExpired: Boolean
        get() = Instant.now().isAfter(cachedAt.plusSeconds(ttlSeconds))
}

@Component
class SalonCacheService {

    private val cache = ConcurrentHashMap<UUID, CachedEntry<SalonSettingsDto>>()

    fun get(salonId: UUID): SalonSettingsDto? {
        val entry = cache[salonId] ?: return null
        if (entry.isExpired) {
            cache.remove(salonId)
            return null
        }
        return entry.value
    }

    fun put(salonId: UUID, dto: SalonSettingsDto) {
        cache[salonId] = CachedEntry(value = dto, cachedAt = Instant.now())
    }

    fun evict(salonId: UUID) {
        cache.remove(salonId)
    }

    fun evictAll() {
        cache.clear()
    }
}
