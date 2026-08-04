/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smarthr.smarthr.enumeration.Status;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/**
 *
 * @author sinawatrarith
 */
public class ProbationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @OneToMany
    @JoinColumn(name = "employee_id", referencedColumnName = "id")
    private EmployeeDetails employee;

    private LocalDate startDate;
    private LocalDate endDate;
    private Status status; // e.g., "active", "completed", "extended"
    private String review_notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
