package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeRepository : JpaRepository<Employee, UUID> {
    fun findAllBySalonIdAndStatusNotOrderByDisplayNameAsc(salonId: UUID, status: EmployeeStatus): List<Employee>
    fun findAllBySalonIdAndStatusOrderByDisplayNameAsc(salonId: UUID, status: EmployeeStatus): List<Employee>
    fun findAllBySalonIdOrderByDisplayNameAsc(salonId: UUID): List<Employee>
    fun findByIdAndSalonId(id: UUID, salonId: UUID): Employee?
    fun findByUserIdAndSalonId(userId: UUID, salonId: UUID): Employee?
    fun existsByUserIdAndSalonIdAndStatusNot(userId: UUID, salonId: UUID, status: EmployeeStatus): Boolean
    fun findAllBySalonId(salonId: UUID): List<Employee>
}
