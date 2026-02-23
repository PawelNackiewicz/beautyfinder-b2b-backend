package com.beautyfinder.b2b.domain.client

import com.beautyfinder.b2b.domain.BaseEntity
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "client_sensitive_data")
class ClientSensitiveData(
    @Column(name = "client_id", nullable = false, unique = true)
    val clientId: UUID,

    @Column(name = "encrypted_data", nullable = false, length = 4000)
    var encryptedData: String,

    @Column(name = "data_hash")
    var dataHash: String? = null,
) : BaseEntity()

@JsonIgnoreProperties(ignoreUnknown = true)
data class SensitiveDataPayload(
    val pesel: String? = null,
    val idNumber: String? = null,
    val medicalNotes: String? = null,
    val allergyInfo: String? = null,
    val customFields: Map<String, String> = emptyMap(),
)
