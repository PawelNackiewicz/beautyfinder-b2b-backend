package com.beautyfinder.b2b.domain.service

import java.util.UUID

open class ServiceDomainException(message: String) : RuntimeException(message)

class ServiceNotFoundException(id: UUID) :
    ServiceDomainException("Service not found: $id")

class ServiceVariantNotFoundException(id: UUID) :
    ServiceDomainException("Service variant not found: $id")

class ServiceCategoryNotFoundException(id: UUID) :
    ServiceDomainException("Service category not found: $id")

class DuplicateServiceNameException(name: String, salonId: UUID) :
    ServiceDomainException("Service '$name' already exists in salon $salonId")

class DuplicateCategoryNameException(name: String, salonId: UUID) :
    ServiceDomainException("Category '$name' already exists in salon $salonId")

class CannotArchiveServiceWithActiveAppointmentsException(serviceId: UUID, appointmentCount: Long) :
    ServiceDomainException("Service $serviceId has $appointmentCount future appointments")

class InvalidDurationException(minutes: Int) :
    ServiceDomainException("Duration must be multiple of 5, between 5 and 480 minutes. Got: $minutes")

class InvalidPriceException(message: String) :
    ServiceDomainException(message)

class ServiceNotInSalonException(serviceId: UUID, salonId: UUID) :
    ServiceDomainException("Service $serviceId does not belong to salon $salonId")

class VariantNotInSalonException(variantId: UUID, salonId: UUID) :
    ServiceDomainException("Variant $variantId does not belong to salon $salonId")
