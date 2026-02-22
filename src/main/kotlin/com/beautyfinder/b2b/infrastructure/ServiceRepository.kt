package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.Service
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceRepository : JpaRepository<Service, UUID>
