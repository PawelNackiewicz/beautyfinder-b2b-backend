package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.BlacklistEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BlacklistRepository : JpaRepository<BlacklistEntry, UUID> {

    fun findByClientIdAndSalonIdAndRemovedAtIsNull(clientId: UUID, salonId: UUID): BlacklistEntry?

    fun existsByClientIdAndSalonIdAndRemovedAtIsNull(clientId: UUID, salonId: UUID): Boolean
}
