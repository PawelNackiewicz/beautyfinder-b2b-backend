package com.beautyfinder.b2b.application.client

import com.beautyfinder.b2b.domain.client.BlacklistEntry
import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.client.ClientAlreadyExistsException
import com.beautyfinder.b2b.domain.client.ClientNotFoundException
import com.beautyfinder.b2b.domain.client.ClientStatus
import com.beautyfinder.b2b.domain.client.ConsentType
import com.beautyfinder.b2b.domain.client.EncryptionService
import com.beautyfinder.b2b.domain.client.GdprConsent
import com.beautyfinder.b2b.domain.client.GdprConsentAlreadyGrantedException
import com.beautyfinder.b2b.domain.client.InsufficientLoyaltyPointsException
import com.beautyfinder.b2b.domain.client.LoyaltyBalance
import com.beautyfinder.b2b.domain.client.LoyaltyTransaction
import com.beautyfinder.b2b.domain.client.LoyaltyTransactionType
import com.beautyfinder.b2b.domain.client.SensitiveDataNotFoundException
import com.beautyfinder.b2b.domain.client.SensitiveDataPayload
import com.beautyfinder.b2b.domain.client.normalizePhone
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.BlacklistRepository
import com.beautyfinder.b2b.infrastructure.ClientRepository
import com.beautyfinder.b2b.infrastructure.ClientSensitiveDataRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.GdprConsentRepository
import com.beautyfinder.b2b.infrastructure.LoyaltyBalanceRepository
import com.beautyfinder.b2b.infrastructure.LoyaltyTransactionRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceVariantRepository
import com.beautyfinder.b2b.domain.client.ClientSensitiveData
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class ClientService(
    private val clientRepository: ClientRepository,
    private val gdprConsentRepository: GdprConsentRepository,
    private val blacklistRepository: BlacklistRepository,
    private val loyaltyBalanceRepository: LoyaltyBalanceRepository,
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository,
    private val clientSensitiveDataRepository: ClientSensitiveDataRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val serviceVariantRepository: ServiceVariantRepository,
    private val serviceRepository: ServiceRepository,
    private val encryptionService: EncryptionService,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(ClientService::class.java)

    fun searchClients(query: ClientSearchQuery, salonId: UUID, pageable: Pageable): Page<ClientSummaryDto> {
        val page = clientRepository.searchClients(salonId, query.phrase, query.status, pageable)
        return page.map { toSummaryDto(it, salonId) }
    }

    fun getClientCard(clientId: UUID, salonId: UUID): ClientCardDto {
        val client = findClient(clientId, salonId)
        val summary = toSummaryDto(client, salonId)

        val consents = gdprConsentRepository.findAllByClientIdAndSalonId(clientId, salonId)
            .map { toGdprConsentDto(it) }

        val blacklistEntry = blacklistRepository.findByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId)
            ?.let { toBlacklistEntryDto(it) }

        val loyaltyBalance = loyaltyBalanceRepository.findByClientIdAndSalonId(clientId, salonId)
            ?.let { toLoyaltyBalanceDto(it) }
            ?: LoyaltyBalanceDto(0, 0, 0)

        val recentAppointments = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(
            salonId,
            OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC).plusYears(1),
        )
            .filter { it.clientId == clientId }
            .sortedByDescending { it.startAt }
            .take(10)
            .map { appt ->
                val variant = serviceVariantRepository.findById(appt.variantId).orElse(null)
                val service = variant?.let { serviceRepository.findById(it.serviceId).orElse(null) }
                val employee = employeeRepository.findById(appt.employeeId).orElse(null)
                AppointmentSummaryDto(
                    id = appt.id!!,
                    startAt = appt.startAt,
                    endAt = appt.endAt,
                    status = appt.status.name,
                    serviceName = service?.name,
                    employeeName = employee?.displayName,
                    finalPrice = appt.finalPrice,
                )
            }

        val stats = buildStats(client, recentAppointments)

        return ClientCardDto(
            client = summary,
            consents = consents,
            blacklistEntry = blacklistEntry,
            loyaltyBalance = loyaltyBalance,
            recentAppointments = recentAppointments,
            stats = stats,
        )
    }

    @Transactional
    fun createClient(request: CreateClientRequest, salonId: UUID): ClientSummaryDto {
        val normalizedPhone = request.phone?.normalizePhone()

        if (normalizedPhone != null && clientRepository.existsByPhoneAndSalonId(normalizedPhone, salonId)) {
            throw ClientAlreadyExistsException(normalizedPhone, salonId)
        }
        if (request.email != null && clientRepository.existsByEmailAndSalonId(request.email, salonId)) {
            throw ClientAlreadyExistsException(request.email, salonId)
        }

        val client = Client(
            salonId = salonId,
            firstName = request.firstName,
            lastName = request.lastName,
            phone = normalizedPhone,
            email = request.email,
            birthDate = request.birthDate,
            status = ClientStatus.ACTIVE,
            source = "MANUAL",
            notes = request.notes,
            preferredEmployeeId = request.preferredEmployeeId,
        )
        val saved = clientRepository.save(client)

        loyaltyBalanceRepository.save(
            LoyaltyBalance(clientId = saved.id!!, salonId = salonId)
        )

        return toSummaryDto(saved, salonId)
    }

    @Transactional
    fun updateClient(clientId: UUID, request: UpdateClientRequest, salonId: UUID): ClientSummaryDto {
        val client = findClient(clientId, salonId)

        request.firstName?.let { client.firstName = it }
        request.lastName?.let { client.lastName = it }

        if (request.phone != null) {
            val normalizedPhone = request.phone.normalizePhone()
            if (clientRepository.existsByPhoneAndSalonIdAndIdNot(normalizedPhone, salonId, clientId)) {
                throw ClientAlreadyExistsException(normalizedPhone, salonId)
            }
            client.phone = normalizedPhone
        }

        if (request.email != null) {
            if (clientRepository.existsByEmailAndSalonIdAndIdNot(request.email, salonId, clientId)) {
                throw ClientAlreadyExistsException(request.email, salonId)
            }
            client.email = request.email
        }

        request.birthDate?.let { client.birthDate = it }
        request.notes?.let { client.notes = it }
        request.preferredEmployeeId?.let { client.preferredEmployeeId = it }

        val saved = clientRepository.save(client)
        return toSummaryDto(saved, salonId)
    }

    @com.beautyfinder.b2b.application.audit.Audited(
        action = com.beautyfinder.b2b.domain.audit.AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED,
        resourceType = "CLIENT",
    )
    fun getSensitiveData(@com.beautyfinder.b2b.application.audit.AuditResourceId clientId: UUID, salonId: UUID): SensitiveDataPayload {
        findClient(clientId, salonId)

        val sensitiveData = clientSensitiveDataRepository.findByClientId(clientId)
            ?: throw SensitiveDataNotFoundException(clientId)

        val json = encryptionService.decrypt(sensitiveData.encryptedData)

        log.warn("Sensitive data accessed for client {} by salon {}", clientId, salonId)

        return objectMapper.readValue(json, SensitiveDataPayload::class.java)
    }

    @Transactional
    @com.beautyfinder.b2b.application.audit.Audited(
        action = com.beautyfinder.b2b.domain.audit.AuditAction.CLIENT_SENSITIVE_DATA_UPDATED,
        resourceType = "CLIENT",
    )
    fun upsertSensitiveData(@com.beautyfinder.b2b.application.audit.AuditResourceId clientId: UUID, payload: SensitiveDataPayload, salonId: UUID) {
        findClient(clientId, salonId)

        val json = objectMapper.writeValueAsString(payload)
        val encrypted = encryptionService.encrypt(json)
        val hash = encryptionService.hash(json)

        val existing = clientSensitiveDataRepository.findByClientId(clientId)
        if (existing != null) {
            existing.encryptedData = encrypted
            existing.dataHash = hash
            clientSensitiveDataRepository.save(existing)
        } else {
            clientSensitiveDataRepository.save(
                ClientSensitiveData(
                    clientId = clientId,
                    encryptedData = encrypted,
                    dataHash = hash,
                )
            )
        }
    }

    @Transactional
    @com.beautyfinder.b2b.application.audit.Audited(
        action = com.beautyfinder.b2b.domain.audit.AuditAction.GDPR_CONSENT_RECORDED,
        resourceType = "CLIENT",
    )
    fun recordGdprConsent(@com.beautyfinder.b2b.application.audit.AuditResourceId clientId: UUID, request: GdprConsentRequest, salonId: UUID): GdprConsentDto {
        findClient(clientId, salonId)

        if (request.granted) {
            val existing = gdprConsentRepository.findByClientIdAndConsentTypeAndGrantedTrue(clientId, request.consentType)
            if (existing != null) {
                val notExpired = existing.expiresAt == null || existing.expiresAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))
                if (notExpired) {
                    throw GdprConsentAlreadyGrantedException(clientId, request.consentType)
                }
            }
        }

        val consent = GdprConsent(
            clientId = clientId,
            salonId = salonId,
            consentType = request.consentType,
            granted = request.granted,
            grantedAt = OffsetDateTime.now(ZoneOffset.UTC),
            ipAddress = request.ipAddress,
            consentVersion = request.consentVersion,
            expiresAt = request.expiresAt,
        )
        val saved = gdprConsentRepository.save(consent)
        return toGdprConsentDto(saved)
    }

    @Transactional
    fun revokeGdprConsent(clientId: UUID, consentType: ConsentType, salonId: UUID) {
        findClient(clientId, salonId)

        gdprConsentRepository.save(
            GdprConsent(
                clientId = clientId,
                salonId = salonId,
                consentType = consentType,
                granted = false,
                grantedAt = OffsetDateTime.now(ZoneOffset.UTC),
                consentVersion = "revoked",
            )
        )
    }

    @Transactional
    @com.beautyfinder.b2b.application.audit.Audited(
        action = com.beautyfinder.b2b.domain.audit.AuditAction.CLIENT_BLACKLISTED,
        resourceType = "CLIENT",
    )
    fun addToBlacklist(@com.beautyfinder.b2b.application.audit.AuditResourceId clientId: UUID, reason: String, createdBy: UUID, salonId: UUID) {
        val client = findClient(clientId, salonId)

        val existing = blacklistRepository.findByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId)
        if (existing != null) {
            return // already blacklisted
        }

        blacklistRepository.save(
            BlacklistEntry(
                clientId = clientId,
                salonId = salonId,
                reason = reason,
                createdBy = createdBy,
            )
        )

        client.status = ClientStatus.BLOCKED
        clientRepository.save(client)
    }

    @Transactional
    fun removeFromBlacklist(clientId: UUID, removedBy: UUID, salonId: UUID) {
        val client = findClient(clientId, salonId)

        val entry = blacklistRepository.findByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId) ?: return

        entry.removedAt = OffsetDateTime.now(ZoneOffset.UTC)
        entry.removedBy = removedBy
        blacklistRepository.save(entry)

        client.status = ClientStatus.ACTIVE
        clientRepository.save(client)
    }

    @Transactional
    fun addLoyaltyPoints(
        clientId: UUID,
        points: Int,
        type: LoyaltyTransactionType,
        appointmentId: UUID?,
        note: String?,
        salonId: UUID,
    ): LoyaltyBalance {
        findClient(clientId, salonId)

        val balance = loyaltyBalanceRepository.findByClientIdAndSalonId(clientId, salonId)
            ?: loyaltyBalanceRepository.save(LoyaltyBalance(clientId = clientId, salonId = salonId))

        if (type == LoyaltyTransactionType.REDEMPTION && balance.points + points < 0) {
            throw InsufficientLoyaltyPointsException(
                required = kotlin.math.abs(points),
                available = balance.points,
            )
        }

        balance.points += points
        if (points > 0) {
            balance.totalEarned += points
        } else {
            balance.totalRedeemed += kotlin.math.abs(points)
        }

        loyaltyTransactionRepository.save(
            LoyaltyTransaction(
                clientId = clientId,
                salonId = salonId,
                points = points,
                type = type,
                appointmentId = appointmentId,
                note = note,
                balanceAfter = balance.points,
            )
        )

        return loyaltyBalanceRepository.save(balance)
    }

    @Transactional
    fun importClientsFromCsv(inputStream: InputStream, salonId: UUID): CsvImportResultDto {
        val format = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()

        val parser = CSVParser(InputStreamReader(inputStream, Charsets.UTF_8), format)
        val records = parser.records

        val errors = mutableListOf<CsvRowError>()
        val validClients = mutableListOf<Client>()
        val phonesInFile = mutableSetOf<String>()
        var skipped = 0

        for ((index, record) in records.withIndex()) {
            val rowNumber = index + 2 // 1-indexed + header row
            val rawData = record.toList().joinToString(",")

            try {
                val firstName = record.get("firstName")?.trim()
                val lastName = record.get("lastName")?.trim()
                val phone = record.get("phone")?.trim()?.takeIf { it.isNotBlank() }?.normalizePhone()
                val email = record.get("email")?.trim()?.takeIf { it.isNotBlank() }
                val birthDateStr = record.get("birthDate")?.trim()?.takeIf { it.isNotBlank() }
                val notes = record.get("notes")?.trim()?.takeIf { it.isNotBlank() }

                if (firstName.isNullOrBlank() || lastName.isNullOrBlank()) {
                    errors.add(CsvRowError(rowNumber, rawData, "firstName and lastName are required"))
                    continue
                }

                if (phone == null && email == null) {
                    errors.add(CsvRowError(rowNumber, rawData, "phone or email is required"))
                    continue
                }

                val birthDate = birthDateStr?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        errors.add(CsvRowError(rowNumber, rawData, "Invalid date format: $it (expected yyyy-MM-dd)"))
                        return@let null
                    }
                }
                if (birthDateStr != null && birthDate == null) continue

                // Duplicate phone in file
                if (phone != null && !phonesInFile.add(phone)) {
                    errors.add(CsvRowError(rowNumber, rawData, "Duplicate phone in file: $phone"))
                    continue
                }

                // Duplicate phone in DB
                if (phone != null && clientRepository.existsByPhoneAndSalonId(phone, salonId)) {
                    skipped++
                    continue
                }

                validClients.add(
                    Client(
                        salonId = salonId,
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        email = email,
                        birthDate = birthDate,
                        status = ClientStatus.ACTIVE,
                        source = "IMPORT",
                        notes = notes,
                    )
                )
            } catch (e: Exception) {
                errors.add(CsvRowError(rowNumber, rawData, "Parse error: ${e.message}"))
            }
        }

        if (validClients.isNotEmpty()) {
            clientRepository.saveAll(validClients)
        }

        return CsvImportResultDto(
            total = records.size,
            imported = validClients.size,
            skipped = skipped,
            errors = errors,
        )
    }

    fun getLoyaltyDetails(clientId: UUID, salonId: UUID): Pair<LoyaltyBalanceDto, List<LoyaltyTransactionDto>> {
        findClient(clientId, salonId)

        val balance = loyaltyBalanceRepository.findByClientIdAndSalonId(clientId, salonId)
            ?.let { toLoyaltyBalanceDto(it) }
            ?: LoyaltyBalanceDto(0, 0, 0)

        val transactions = loyaltyTransactionRepository
            .findAllByClientIdAndSalonIdOrderByCreatedAtDesc(clientId, salonId, PageRequest.of(0, 20))
            .content
            .map { toLoyaltyTransactionDto(it) }

        return balance to transactions
    }

    // -- mapping helpers --

    private fun findClient(clientId: UUID, salonId: UUID): Client =
        clientRepository.findByIdAndSalonId(clientId, salonId)
            ?: throw ClientNotFoundException(clientId)

    private fun toSummaryDto(client: Client, salonId: UUID): ClientSummaryDto {
        val loyaltyPoints = loyaltyBalanceRepository.findByClientIdAndSalonId(client.id!!, salonId)?.points ?: 0
        val isBlacklisted = blacklistRepository.existsByClientIdAndSalonIdAndRemovedAtIsNull(client.id!!, salonId)

        return ClientSummaryDto(
            id = client.id!!,
            salonId = client.salonId,
            firstName = client.firstName,
            lastName = client.lastName,
            phone = client.phone,
            email = client.email,
            status = client.status,
            totalVisits = client.totalVisits,
            totalSpent = client.totalSpent,
            lastVisitAt = client.lastVisitAt,
            loyaltyPoints = loyaltyPoints,
            isBlacklisted = isBlacklisted,
        )
    }

    private fun toGdprConsentDto(consent: GdprConsent) = GdprConsentDto(
        id = consent.id!!,
        consentType = consent.consentType,
        granted = consent.granted,
        grantedAt = consent.grantedAt,
        expiresAt = consent.expiresAt,
        consentVersion = consent.consentVersion,
    )

    private fun toBlacklistEntryDto(entry: BlacklistEntry) = BlacklistEntryDto(
        id = entry.id!!,
        reason = entry.reason,
        createdAt = entry.createdAt,
        isActive = entry.isActive,
    )

    private fun toLoyaltyBalanceDto(balance: LoyaltyBalance) = LoyaltyBalanceDto(
        points = balance.points,
        totalEarned = balance.totalEarned,
        totalRedeemed = balance.totalRedeemed,
    )

    private fun toLoyaltyTransactionDto(tx: LoyaltyTransaction) = LoyaltyTransactionDto(
        id = tx.id!!,
        points = tx.points,
        type = tx.type,
        appointmentId = tx.appointmentId,
        note = tx.note,
        balanceAfter = tx.balanceAfter,
        createdAt = tx.createdAt,
    )

    private fun buildStats(client: Client, recentAppointments: List<AppointmentSummaryDto>): ClientStatsDto {
        val totalSpent = client.totalSpent
        val totalVisits = client.totalVisits
        val avgValue = if (totalVisits > 0) totalSpent.divide(BigDecimal(totalVisits), 2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO

        val favoriteService = recentAppointments
            .mapNotNull { it.serviceName }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val favoriteEmployee = recentAppointments
            .mapNotNull { it.employeeName }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val firstVisitAt = recentAppointments.minByOrNull { it.startAt }?.startAt

        return ClientStatsDto(
            totalVisits = totalVisits,
            totalSpent = totalSpent,
            avgVisitValue = avgValue,
            firstVisitAt = firstVisitAt,
            lastVisitAt = client.lastVisitAt,
            favoriteServiceName = favoriteService,
            favoriteEmployeeName = favoriteEmployee,
        )
    }
}
