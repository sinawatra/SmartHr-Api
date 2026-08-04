package com.smarthr.smarthr.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.entity.AttendanceEntity;

/**
 * Repository interface for AttendanceEntity.
 *
 * @author sinawatrarith
 */
@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    // 1. Find the current active shift (clocked in, but clock_out is NULL)
    Optional<AttendanceEntity> findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(Long employeeId);

    // 2. Fetch all attendance records for a specific employee
    List<AttendanceEntity> findByEmployeeIdOrderByClockInDesc(Long employeeId);

    // 3. Fetch attendance history for an employee within a date/time range
    List<AttendanceEntity> findByEmployeeIdAndClockInBetweenOrderByClockInDesc(
            Long employeeId, 
            LocalDateTime start, 
            LocalDateTime end
    );

    Page<AttendanceEntity> findByEmployeeIdAndClockInBetween(
            Long employeeId, 
            LocalDateTime start, 
            LocalDateTime end,
            Pageable pageable
    );

    Page<AttendanceEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    // 4. Fetch all attendance records across all employees within a date range (for HR/Admin reports)
    List<AttendanceEntity> findByClockInBetweenOrderByClockInDesc(
            LocalDateTime start, 
            LocalDateTime end
    );

    Page<AttendanceEntity> findByClockInBetween(
            LocalDateTime start, 
            LocalDateTime end,
            Pageable pageable
    );

    // 5. Check if an employee has already clocked in today
    boolean existsByEmployeeIdAndClockInBetween(
            Long employeeId, 
            LocalDateTime startOfDay, 
            LocalDateTime endOfDay
    );

    // 6. Calculate total work duration in minutes for an employee in a given period
    @Query("SELECT SUM(a.workDurationMinutes) FROM AttendanceEntity a " +
           "WHERE a.employee.id = :employeeId " +
           "AND a.clockIn BETWEEN :startDate AND :endDate")
    Long sumWorkDurationMinutesByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate
    );
}