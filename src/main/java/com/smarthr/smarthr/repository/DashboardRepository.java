package com.smarthr.smarthr.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.enumeration.EmployementStatus;
import com.smarthr.smarthr.enumeration.LeaveStatus;

/**
 * Read-only aggregate queries backing the HR dashboard.
 */
public interface DashboardRepository extends Repository<EmployeeDetails, Long> {

    @Query("SELECT COUNT(e) FROM EmployeeDetails e")
    long countTotalEmployees();

    @Query("SELECT COUNT(e) FROM EmployeeDetails e WHERE e.employeeStatus IS NULL OR e.employeeStatus NOT IN (:statuses)")
    long countActiveEmployees(@Param("statuses") Collection<EmployementStatus> statuses);

    @Query("SELECT COUNT(DISTINCT a.employee.id) FROM AttendanceEntity a WHERE a.clockIn BETWEEN :start AND :end")
    long countPresentBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT l.employee.id) FROM LeaveRequestEntity l "
            + "WHERE l.status = :status AND :onDate BETWEEN l.startDate AND l.endDate")
    long countOnLeaveOn(@Param("status") LeaveStatus status, @Param("onDate") LocalDate onDate);

    @Query("SELECT COUNT(l) FROM LeaveRequestEntity l WHERE l.status = :status")
    long countLeaveRequestsByStatus(@Param("status") LeaveStatus status);

    @Query("SELECT COUNT(e) FROM EmployeeDetails e "
            + "WHERE e.probationEndDate IS NOT NULL AND e.probationEndDate BETWEEN :from AND :to")
    long countProbationEndingBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
