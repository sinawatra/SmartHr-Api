/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smarthr.smarthr.entity.LeaveTypeEntity;

/**
 *
 * @author sinawatrarith
 */
@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveTypeEntity, Integer> {

}