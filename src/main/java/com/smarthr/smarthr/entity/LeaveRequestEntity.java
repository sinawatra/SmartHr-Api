/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.entity;


/**
 *
 * @author sinawatrarith
 */
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // The employee making the leave request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = false)
    private EmployeeDetails employee;

    // The employee approving/rejecting the request (nullable since it's empty when pending)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", referencedColumnName = "id")
    private EmployeeDetails approver;

    // Assuming you have a LeaveType entity for leave_type_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", referencedColumnName = "id", nullable = false)
    private LeaveTypeEntity leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    // Optional: Use EnumType.STRING if you map status to a Java Enum
    @Column(name = "status", length = 50)
    private String status; 

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters omitted for brevity
}