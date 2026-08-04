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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "onboarding")
public class OnboardingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Foreign key pointing to the employee undergoing onboarding
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = false)
    private EmployeeDetails employee;

    @Column(name = "overall_status", length = 50)
    private String overallStatus; // e.g., "Pending", "In Progress", "Completed"

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Inverse side of the relationship to access all tasks under this onboarding record
    @OneToMany(mappedBy = "onboarding", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OnboardingTaskEntity> tasks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // // Helper methods to keep the bi-directional relationship in sync
    // public void addTask(OnboardingTaskEntity task) {
    //     tasks.add(task);
    //     task.setOnboarding(this);
    // }

    // public void removeTask(OnboardingTaskEntity task) {
    //     tasks.remove(task);
    //     task.setOnboarding(null);
    // }

    // Getters and Setters omitted for brevity
}
