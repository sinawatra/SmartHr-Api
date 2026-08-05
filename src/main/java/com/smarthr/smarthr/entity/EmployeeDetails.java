package com.smarthr.smarthr.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing employee details including authentication credentials.
 */
@Entity
@Table(name = "employeeDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false) // Maps to role_id in DB
    private RoleEntity role; 

    private Integer companyId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private DepartmentEntity departmentId;
    private Integer managerId;

    public Integer getRoleId() {
        return role != null ? role.getId() : null;
    }
    private String employeeCode;
    private String firstName;
    private String lastName; 
    
    private String email;
    
    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "employment_status")
    private String employeeStatus;

    @Column(name = "hire_date")
    private LocalDate hiredate;
    private LocalDate probationEndDate;
    private String profileImage;
    private LocalDate endDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
