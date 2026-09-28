package com.smarthr.smarthr.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.RoleEntity;
import com.smarthr.smarthr.enumeration.EmployementStatus;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeDetails, Long> {
    Optional<EmployeeDetails> findByUsername(String username);
    Optional<EmployeeDetails> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<EmployeeDetails> findByRole(RoleEntity role);
    Page<EmployeeDetails> findByEmployeeStatus(EmployementStatus employeeStatus, Pageable pageable);
    long countByEmployeeStatus(EmployementStatus employeeStatus);
    long countByEmployeeStatusAndProbationEndDateBetween(EmployementStatus employeeStatus, LocalDate from, LocalDate to);
    long countByEmployeeStatusAndProbationEndDateBefore(EmployementStatus employeeStatus, LocalDate date);
}
