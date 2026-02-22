package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.ServiceVariant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceVariantRepository : JpaRepository<ServiceVariant, UUID>
