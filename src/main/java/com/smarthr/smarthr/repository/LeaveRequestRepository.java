/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package com.smarthr.smarthr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smarthr.smarthr.entity.LeaveRequestEntity;

/**
 *
 * @author sinawatrarith
 */
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {
    java.util.List<LeaveRequestEntity> findByEmployeeId(Long employeeId);
}
