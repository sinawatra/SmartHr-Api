package com.smarthr.smarthr.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smarthr.smarthr.attendance.dto.AttendanceScoreResponse;
import com.smarthr.smarthr.attendance.entity.AttendanceEntity;
import com.smarthr.smarthr.attendance.repository.AttendanceRepository;
import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.company.entity.WorkSchedule;
import com.smarthr.smarthr.company.service.WorkScheduleService;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.leave.entity.LeaveRequestEntity;
import com.smarthr.smarthr.leave.entity.LeaveStatus;
import com.smarthr.smarthr.leave.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceScoreServiceTest {

    // Week of Monday 2026-10-05 to Friday 2026-10-09; "now" is Friday 18:00
    private static final LocalDate MON = LocalDate.of(2026, 10, 5);
    private static final LocalDate TUE = MON.plusDays(1);
    private static final LocalDate WED = MON.plusDays(2);
    private static final LocalDate THU = MON.plusDays(3);
    private static final LocalDate FRI = MON.plusDays(4);

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private WorkScheduleService workScheduleService;

    private AttendanceScoreService scoreService;
    private EmployeeDetails employee;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FRI.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        scoreService = new AttendanceScoreService(attendanceRepository, leaveRequestRepository, employeeRepository,
                workScheduleService, clock);

        employee = EmployeeDetails.builder()
                .id(1L)
                .username("john_doe")
                .firstName("John")
                .lastName("Doe")
                .hiredate(LocalDate.of(2025, 1, 1))
                .build();

        lenient().when(workScheduleService.forEmployee(any())).thenReturn(WorkSchedule.defaults());
        lenient().when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        authenticateAs("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getScore_AppliesEachDeduction() {
        AttendanceEntity monday = attendance(MON.atTime(8, 0), null, 0L);                  // missed clock-out: -2
        AttendanceEntity tuesday = attendance(TUE.atTime(8, 15), TUE.atTime(17, 0), 15L);  // late < 30: -1
        AttendanceEntity friday = attendance(FRI.atTime(8, 40), null, null);               // late >= 30 (recalculated): -2
        LeaveRequestEntity wednesdayLeave = approvedLeave(WED, WED);
        // Thursday: no clock-in, no leave → absent: -5

        when(attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(eq(1L), any(), any()))
                .thenReturn(List.of(friday, tuesday, monday));
        when(leaveRequestRepository.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), eq(LeaveStatus.APPROVED), any(), any()))
                .thenReturn(List.of(wednesdayLeave));

        AttendanceScoreResponse score = scoreService.getScore(1L, MON, FRI);

        assertEquals(90, score.getScore());
        assertEquals(5, score.getSuggestedRating());
        assertEquals(5, score.getWorkingDays());
        assertEquals(3, score.getPresentDays());
        assertEquals(2, score.getLateDays());
        assertEquals(55, score.getTotalLateMinutes());
        assertEquals(1, score.getMissedClockOutDays());
        assertEquals(1, score.getAbsentDays());
        assertEquals(1, score.getLeaveDays());
        assertEquals(3, score.getLateDeduction());
        assertEquals(2, score.getMissedClockOutDeduction());
        assertEquals(5, score.getAbsenceDeduction());
        assertEquals(0, score.getBonus());
        assertEquals(75.0, score.getAttendanceRate());
        assertEquals(33.3, score.getPunctualityRate());
    }

    @Test
    void getScore_TodayWithoutClockIn_IsNotCountedAsAbsent() {
        when(attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), eq(LeaveStatus.APPROVED), any(), any()))
                .thenReturn(List.of());

        // The end date is in the future; only Mon-Thu are scored.
        AttendanceScoreResponse score = scoreService.getScore(1L, MON, MON.plusDays(20));

        assertEquals(4, score.getWorkingDays());
        assertEquals(4, score.getAbsentDays());
        assertEquals(80, score.getScore());
        assertEquals(4, score.getSuggestedRating());
    }

    @Test
    void getScore_PerfectWeek_GetsBonusCappedAt100() {
        List<AttendanceEntity> week = List.of(MON, TUE, WED, THU, FRI).stream()
                .map(d -> attendance(d.atTime(7, 55), d.atTime(17, 5), 0L))
                .toList();
        when(attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(eq(1L), any(), any()))
                .thenReturn(week);
        when(leaveRequestRepository.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), eq(LeaveStatus.APPROVED), any(), any()))
                .thenReturn(List.of());

        AttendanceScoreResponse score = scoreService.getScore(1L, MON, FRI);

        assertEquals(5, score.getBonus());
        assertEquals(100, score.getScore());
        assertEquals(100.0, score.getAttendanceRate());
        assertEquals(100.0, score.getPunctualityRate());
    }

    @Test
    void getScore_OtherEmployeeWhoIsNotManager_IsDenied() {
        authenticateAs("jane", "USER");
        EmployeeDetails jane = EmployeeDetails.builder().id(2L).username("jane").build();
        when(employeeRepository.findByUsername("jane")).thenReturn(Optional.of(jane));

        assertThrows(AccessDeniedException.class, () -> scoreService.getScore(1L, MON, FRI));
    }

    @Test
    void getScore_LineManager_IsAllowed() {
        authenticateAs("boss", "LINE_MANAGER");
        employee.setManagerId(2);
        EmployeeDetails boss = EmployeeDetails.builder().id(2L).username("boss").build();
        when(employeeRepository.findByUsername("boss")).thenReturn(Optional.of(boss));
        when(attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), eq(LeaveStatus.APPROVED), any(), any()))
                .thenReturn(List.of());

        assertNotNull(scoreService.getScore(1L, MON, FRI));
    }

    @Test
    void getScores_SortsByScoreAndSkipsInactiveEmployees() {
        EmployeeDetails absentee = EmployeeDetails.builder().id(2L).username("absentee").firstName("Abe").lastName("Sent").build();
        EmployeeDetails resigned = EmployeeDetails.builder().id(3L).username("gone")
                .employeeStatus(EmployementStatus.Resigned).build();
        when(employeeRepository.findAll()).thenReturn(List.of(absentee, employee, resigned));

        List<AttendanceEntity> johnsMonth = MON.withDayOfMonth(1).datesUntil(FRI.plusDays(1))
                .map(d -> attendance(d.atTime(8, 0), d.atTime(17, 0), 0L))
                .toList();
        when(attendanceRepository.findByClockInBetweenOrderByClockInDesc(any(), any())).thenReturn(johnsMonth);
        when(leaveRequestRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(LeaveStatus.APPROVED), any(), any()))
                .thenReturn(List.of());

        PagedResponse<AttendanceScoreResponse> page = scoreService.getScores(YearMonth.of(2026, 10), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals(1L, page.getContent().get(0).getEmployeeId());
        assertEquals(100, page.getContent().get(0).getScore());
        assertEquals(2L, page.getContent().get(1).getEmployeeId());
        assertTrue(page.getContent().get(1).getScore() < 100);
    }

    @Test
    void toRating_MapsScoreBands() {
        assertEquals(5, AttendanceScoreService.toRating(90));
        assertEquals(4, AttendanceScoreService.toRating(89));
        assertEquals(3, AttendanceScoreService.toRating(70));
        assertEquals(2, AttendanceScoreService.toRating(60));
        assertEquals(1, AttendanceScoreService.toRating(59));
    }

    private AttendanceEntity attendance(LocalDateTime clockIn, LocalDateTime clockOut, Long lateMinutes) {
        return AttendanceEntity.builder()
                .employee(employee)
                .clockIn(clockIn)
                .clockOut(clockOut)
                .lateMinutes(lateMinutes)
                .build();
    }

    private LeaveRequestEntity approvedLeave(LocalDate start, LocalDate end) {
        LeaveRequestEntity leave = new LeaveRequestEntity();
        leave.setEmployee(employee);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setStatus(LeaveStatus.APPROVED);
        return leave;
    }

    private void authenticateAs(String username, String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(authority))));
    }
}
