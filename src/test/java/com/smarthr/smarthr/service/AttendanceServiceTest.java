package com.smarthr.smarthr.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.smarthr.smarthr.entity.AttendanceEntity;
import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.repository.AttendanceRepository;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.request.ClockInRequest;
import com.smarthr.smarthr.request.ClockOutRequest;
import com.smarthr.smarthr.response.AttendanceResponse;
import com.smarthr.smarthr.response.PagedResponse;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private EmployeeDetails mockEmployee;
    private AttendanceEntity mockAttendance;

    @BeforeEach
    void setUp() {
        mockEmployee = EmployeeDetails.builder()
                .id(1L)
                .username("john_doe")
                .firstName("John")
                .lastName("Doe")
                .email("john@smarthr.com")
                .build();

        mockAttendance = AttendanceEntity.builder()
                .id(100L)
                .employee(mockEmployee)
                .clockIn(LocalDateTime.now().minusHours(8))
                .status("PRESENT")
                .notes("Morning shift")
                .build();
    }

    @Test
    void testClockIn_Success() {
        ClockInRequest request = ClockInRequest.builder()
                .employeeId(1L)
                .notes("Arrived on time")
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(AttendanceEntity.class))).thenAnswer(invocation -> {
            AttendanceEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        AttendanceResponse response = attendanceService.clockIn(request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(1L, response.getEmployeeId());
        assertEquals("John Doe", response.getEmployeeName());
        assertEquals("PRESENT", response.getStatus());
        assertEquals("Arrived on time", response.getNotes());
        verify(attendanceRepository).save(any(AttendanceEntity.class));
    }

    @Test
    void testClockIn_AlreadyClockedIn() {
        ClockInRequest request = ClockInRequest.builder()
                .employeeId(1L)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.of(mockAttendance));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> attendanceService.clockIn(request));

        assertTrue(exception.getMessage().contains("Already clocked in"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void testClockIn_AlreadyClockedInToday() {
        ClockInRequest request = ClockInRequest.builder()
                .employeeId(1L)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.empty());
        when(attendanceRepository.existsByEmployeeIdAndClockInBetween(eq(1L), any(), any()))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> attendanceService.clockIn(request));

        assertTrue(exception.getMessage().contains("already clocked in today"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void testClockIn_PreviousDayOpenSession_AllowsNewClockIn() {
        // Employee forgot to clock out yesterday; the stale session must not block today's clock-in.
        AttendanceEntity staleSession = AttendanceEntity.builder()
                .id(99L)
                .employee(mockEmployee)
                .clockIn(LocalDateTime.now().minusDays(1))
                .status("PRESENT")
                .build();

        ClockInRequest request = ClockInRequest.builder()
                .employeeId(1L)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.of(staleSession));
        when(attendanceRepository.existsByEmployeeIdAndClockInBetween(eq(1L), any(), any()))
                .thenReturn(false);
        when(attendanceRepository.save(any(AttendanceEntity.class))).thenAnswer(invocation -> {
            AttendanceEntity entity = invocation.getArgument(0);
            entity.setId(102L);
            return entity;
        });

        AttendanceResponse response = attendanceService.clockIn(request);

        assertNotNull(response);
        assertEquals(102L, response.getId());
        assertEquals("PRESENT", response.getStatus());
        verify(attendanceRepository).save(any(AttendanceEntity.class));
    }

    @Test
    void testClockIn_EmployeeNotFound() {
        ClockInRequest request = ClockInRequest.builder()
                .employeeId(999L)
                .build();

        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> attendanceService.clockIn(request));

        assertTrue(exception.getMessage().contains("Employee not found with ID: 999"));
    }

    @Test
    void testClockOut_Success() {
        ClockOutRequest request = ClockOutRequest.builder()
                .employeeId(1L)
                .notes("Leaving for the day")
                .build();

        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.of(mockAttendance));
        when(attendanceRepository.save(any(AttendanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponse response = attendanceService.clockOut(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getClockOut());
        assertNotNull(response.getWorkDurationMinutes());
        assertTrue(response.getNotes().contains("Leaving for the day"));
        verify(attendanceRepository).save(mockAttendance);
    }

    @Test
    void testClockOut_NoActiveSession() {
        ClockOutRequest request = ClockOutRequest.builder()
                .employeeId(1L)
                .build();

        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> attendanceService.clockOut(request));

        assertTrue(exception.getMessage().contains("No active clock-in record found"));
    }

    @Test
    void testGetActiveSession() {
        when(attendanceRepository.findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(1L))
                .thenReturn(Optional.of(mockAttendance));

        Optional<AttendanceResponse> response = attendanceService.getActiveSession(1L);

        assertTrue(response.isPresent());
        assertEquals(100L, response.get().getId());
        assertEquals("PRESENT", response.get().getStatus());
    }

    @Test
    void testGetAttendanceHistory() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(eq(1L), any(), any()))
                .thenReturn(List.of(mockAttendance));

        List<AttendanceResponse> history = attendanceService.getAttendanceHistory(1L, startDate, endDate);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(100L, history.get(0).getId());
    }

    @Test
    void testGetAttendanceHistory_Paged() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<AttendanceEntity> pageMock = new PageImpl<>(List.of(mockAttendance), pageable, 1);

        when(attendanceRepository.findByEmployeeIdAndClockInBetween(eq(1L), any(), any(), eq(pageable)))
                .thenReturn(pageMock);

        PagedResponse<AttendanceResponse> history = attendanceService.getAttendanceHistory(1L, startDate, endDate, pageable);

        assertNotNull(history);
        assertEquals(1, history.getContent().size());
        assertEquals(0, history.getPageNumber());
        assertEquals(10, history.getPageSize());
        assertEquals(1, history.getTotalElements());
    }
}
