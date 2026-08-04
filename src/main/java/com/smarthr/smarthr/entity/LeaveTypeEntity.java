/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 *
 * @author sinawatrarith
 */

import jakarta.persistence.*;

@Entity
@Table(name = "leave_types")
public class LeaveTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // e.g., "ANNUAL", "SICK", "MATERNITY", "UNPAID"
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    // Maximum allowed days per year for this leave type
    @Column(name = "max_days", nullable = false)
    private Integer maxDays; // Using Integer wrapper instead of primitive int

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public LeaveTypeEntity() {
    }

    public LeaveTypeEntity(String name, Integer maxDays, String description) {
        this.name = name;
        this.maxDays = maxDays;
        this.description = description;
    }

    // Getters and Setters omitted for brevity
}