package com.smarthr.smarthr.employee.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.role.entity.RoleEntity;
import com.smarthr.smarthr.employee.entity.EmployementStatus;

/**
 * Employee lookups. Every query excludes soft-deleted employees, except
 * {@link #findAllById} (used to resolve names for history) and
 * {@link #existsByUsernameIncludingDeleted}.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeDetails, Long> {

    @Override
    @Query("select e from EmployeeDetails e where e.id = :id and e.deleted = false")
    Optional<EmployeeDetails> findById(@Param("id") Long id);

    @Override
    @Query("select count(e) > 0 from EmployeeDetails e where e.id = :id and e.deleted = false")
    boolean existsById(@Param("id") Long id);

    @Override
    @Query("select e from EmployeeDetails e where e.deleted = false")
    List<EmployeeDetails> findAll();

    @Override
    @Query("select e from EmployeeDetails e where e.deleted = false")
    Page<EmployeeDetails> findAll(Pageable pageable);

    @Query("select e from EmployeeDetails e where e.username = :username and e.deleted = false")
    Optional<EmployeeDetails> findByUsername(@Param("username") String username);

    @Query("select e from EmployeeDetails e where e.email = :email and e.deleted = false")
    Optional<EmployeeDetails> findByEmail(@Param("email") String email);

    @Query("select count(e) > 0 from EmployeeDetails e where e.username = :username and e.deleted = false")
    boolean existsByUsername(@Param("username") String username);

    @Query("select count(e) > 0 from EmployeeDetails e where e.email = :email and e.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    // Includes soft-deleted employees: their username still holds the unique constraint
    @Query("select count(e) > 0 from EmployeeDetails e where e.username = :username")
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);

    @Query("select e from EmployeeDetails e where e.role = :role and e.deleted = false")
    List<EmployeeDetails> findByRole(@Param("role") RoleEntity role);

    @Query("select e from EmployeeDetails e where e.employeeStatus = :status and e.deleted = false")
    Page<EmployeeDetails> findByEmployeeStatus(@Param("status") EmployementStatus employeeStatus, Pageable pageable);

    @Query("select count(e) from EmployeeDetails e where e.employeeStatus = :status and e.deleted = false")
    long countByEmployeeStatus(@Param("status") EmployementStatus employeeStatus);

    @Query("select count(e) from EmployeeDetails e where e.employeeStatus = :status"
            + " and e.probationEndDate between :from and :to and e.deleted = false")
    long countByEmployeeStatusAndProbationEndDateBetween(@Param("status") EmployementStatus employeeStatus,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select count(e) from EmployeeDetails e where e.employeeStatus = :status"
            + " and e.probationEndDate < :date and e.deleted = false")
    long countByEmployeeStatusAndProbationEndDateBefore(@Param("status") EmployementStatus employeeStatus,
            @Param("date") LocalDate date);

    // Notification recipients
    @Query("select e from EmployeeDetails e where e.role.name = :roleName and e.deleted = false")
    List<EmployeeDetails> findByRole_Name(@Param("roleName") String roleName);

    @Query("select e from EmployeeDetails e where e.companyId = :companyId and e.deleted = false")
    List<EmployeeDetails> findByCompanyId(@Param("companyId") Integer companyId);

    @Query("select e from EmployeeDetails e where e.department.id = :departmentId and e.deleted = false")
    List<EmployeeDetails> findByDepartment_Id(@Param("departmentId") Long departmentId);

    @Query("select e from EmployeeDetails e where e.employeeStatus = :status"
            + " and e.probationEndDate = :date and e.deleted = false")
    List<EmployeeDetails> findByEmployeeStatusAndProbationEndDate(@Param("status") EmployementStatus employeeStatus,
            @Param("date") LocalDate probationEndDate);
}
