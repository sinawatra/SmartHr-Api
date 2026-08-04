package com.smarthr.smarthr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.RoleEntity;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeDetails, Long> {
    Optional<EmployeeDetails> findByUsername(String username);
    Optional<EmployeeDetails> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<EmployeeDetails> findByRole(RoleEntity role);
}
