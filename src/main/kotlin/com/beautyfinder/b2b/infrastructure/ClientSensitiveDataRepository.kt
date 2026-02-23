package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.ClientSensitiveData
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ClientSensitiveDataRepository : JpaRepository<ClientSensitiveData, UUID> {

    fun findByClientId(clientId: UUID): ClientSensitiveData?
}
