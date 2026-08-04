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

@Entity
@Table(name = "onboarding_tasks")
public class OnboardingTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Foreign key pointing to the main Onboarding workflow entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_id", referencedColumnName = "id", nullable = false)
    private OnboardingEntity onboarding;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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