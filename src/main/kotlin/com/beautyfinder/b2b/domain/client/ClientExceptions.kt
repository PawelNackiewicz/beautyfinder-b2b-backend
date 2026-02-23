package com.beautyfinder.b2b.domain.client

import java.util.UUID

open class ClientDomainException(message: String) : RuntimeException(message)

class ClientNotFoundException(id: UUID) :
    ClientDomainException("Client not found: $id")

class ClientAlreadyExistsException(contact: String, salonId: UUID) :
    ClientDomainException("Client with this contact '$contact' already exists in salon $salonId")

class ClientBlockedException(id: UUID, reason: String) :
    ClientDomainException("Client is blacklisted: $reason")

class InsufficientLoyaltyPointsException(required: Int, available: Int) :
    ClientDomainException("Insufficient loyalty points: required=$required, available=$available")

class GdprConsentAlreadyGrantedException(clientId: UUID, consentType: ConsentType) :
    ClientDomainException("GDPR consent $consentType already granted for client $clientId")

class SensitiveDataNotFoundException(clientId: UUID) :
    ClientDomainException("Sensitive data not found for client $clientId")

class CsvImportException(rowNumber: Int, message: String) :
    ClientDomainException("CSV import error at row $rowNumber: $message")
