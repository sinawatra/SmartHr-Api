/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package com.smarthr.smarthr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smarthr.smarthr.entity.LeaveRequestEntity;
import com.smarthr.smarthr.enumeration.LeaveStatus;

/**
 *
 * @author sinawatrarith
 */
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {
    java.util.List<LeaveRequestEntity> findByEmployeeId(Long employeeId);
    Page<LeaveRequestEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    // Requests raised by the direct reports of a given manager, filtered by status
    // (used by the LINE_MANAGER approval queue).
    Page<LeaveRequestEntity> findByEmployee_ManagerIdAndStatus(Integer managerId, LeaveStatus status, Pageable pageable);

    // All requests in a given status (ADMIN-wide approval queue).
    Page<LeaveRequestEntity> findByStatus(LeaveStatus status, Pageable pageable);
}
