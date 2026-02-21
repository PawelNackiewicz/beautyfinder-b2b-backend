package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.Client
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ClientRepository : JpaRepository<Client, UUID> {
    fun findAllBySalonId(salonId: UUID): List<Client>
}
