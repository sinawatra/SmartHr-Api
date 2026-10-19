package com.smarthr.smarthr.evaluation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.evaluation.dto.ConfirmProbationRequest;
import com.smarthr.smarthr.evaluation.dto.EvaluationResponse;
import com.smarthr.smarthr.evaluation.entity.EvaluationEntity;
import com.smarthr.smarthr.evaluation.entity.EvaluationStatus;
import com.smarthr.smarthr.evaluation.entity.ProbationRecommendation;
import com.smarthr.smarthr.evaluation.repository.EvaluationCriteriaRepository;
import com.smarthr.smarthr.evaluation.repository.EvaluationRepository;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    // 2026-10-19
    private final Clock clock = Clock.fixed(
            LocalDateTime.of(2026, 10, 19, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private EvaluationService evaluationService;

    private EmployeeDetails employee;
    private EmployeeDetails manager;
    private EmployeeDetails hr;
    private EvaluationEntity evaluation;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationService(employeeRepository, evaluationCriteriaRepository, evaluationRepository, clock);

        manager = EmployeeDetails.builder().id(2L).username("manager").firstName("Mary").lastName("Manager").build();
        hr = EmployeeDetails.builder().id(3L).username("hr").firstName("Harry").lastName("HR").build();
        employee = EmployeeDetails.builder()
                .id(1L)
                .username("john_doe")
                .firstName("John")
                .lastName("Doe")
                .employeeStatus(EmployementStatus.Probation)
                .probationEndDate(LocalDate.of(2026, 10, 31))
                .build();
        evaluation = EvaluationEntity.builder()
                .id(10L)
                .employee(employee)
                .evaluator(manager)
                .status(EvaluationStatus.SUBMITTED)
                .probationRecommendation(ProbationRecommendation.PASS)
                .items(List.of())
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "hr", null, List.of(new SimpleGrantedAuthority("HR"))));
        lenient().when(employeeRepository.findByUsername("hr")).thenReturn(Optional.of(hr));
        lenient().when(evaluationRepository.findById(10L)).thenReturn(Optional.of(evaluation));
        lenient().when(evaluationRepository.save(any(EvaluationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConfirmProbation_Pass() {
        EvaluationResponse response = evaluationService.confirmProbation(10L, ConfirmProbationRequest.builder()
                .decision(ProbationRecommendation.PASS)
                .comment("Well done")
                .build());

        assertEquals(EmployementStatus.FullStaff, employee.getEmployeeStatus());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals("PASS", response.getProbationRecommendation());
        assertEquals("PASS", response.getHrDecision());
        assertEquals("Well done", response.getHrComment());
        assertEquals(3L, response.getHrDecidedById());
        assertEquals(LocalDateTime.of(2026, 10, 19, 10, 0), response.getHrDecidedAt());
        verify(employeeRepository).save(employee);
    }

    @Test
    void testConfirmProbation_Fail_DefaultsEndDateToToday() {
        EvaluationResponse response = evaluationService.confirmProbation(10L, ConfirmProbationRequest.builder()
                .decision(ProbationRecommendation.FAIL)
                .build());

        assertEquals(EmployementStatus.Terminated, employee.getEmployeeStatus());
        assertEquals(LocalDate.of(2026, 10, 19), employee.getEndDate());
        // HR may override the manager's recommendation
        assertEquals("PASS", response.getProbationRecommendation());
        assertEquals("FAIL", response.getHrDecision());
    }

    @Test
    void testConfirmProbation_Delay_ExtendsProbation() {
        evaluationService.confirmProbation(10L, ConfirmProbationRequest.builder()
                .decision(ProbationRecommendation.DELAY)
                .newProbationEndDate(LocalDate.of(2027, 1, 31))
                .build());

        assertEquals(EmployementStatus.Probation, employee.getEmployeeStatus());
        assertEquals(LocalDate.of(2027, 1, 31), employee.getProbationEndDate());
        assertEquals(EvaluationStatus.CONFIRMED, evaluation.getStatus());
    }

    @Test
    void testConfirmProbation_Delay_RequiresLaterEndDate() {
        assertThrows(IllegalArgumentException.class, () -> evaluationService.confirmProbation(10L,
                ConfirmProbationRequest.builder().decision(ProbationRecommendation.DELAY).build()));
        assertThrows(IllegalArgumentException.class, () -> evaluationService.confirmProbation(10L,
                ConfirmProbationRequest.builder()
                        .decision(ProbationRecommendation.DELAY)
                        .newProbationEndDate(LocalDate.of(2026, 10, 31))
                        .build()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void testConfirmProbation_AlreadyConfirmed() {
        evaluation.setStatus(EvaluationStatus.CONFIRMED);
        evaluation.setHrDecision(ProbationRecommendation.PASS);

        assertThrows(IllegalStateException.class, () -> evaluationService.confirmProbation(10L,
                ConfirmProbationRequest.builder().decision(ProbationRecommendation.FAIL).build()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void testConfirmProbation_NotAProbationEvaluation() {
        evaluation.setProbationRecommendation(null);

        assertThrows(IllegalArgumentException.class, () -> evaluationService.confirmProbation(10L,
                ConfirmProbationRequest.builder().decision(ProbationRecommendation.PASS).build()));
    }

    @Test
    void testConfirmProbation_EmployeeNotOnProbation() {
        employee.setEmployeeStatus(EmployementStatus.FullStaff);

        assertThrows(IllegalStateException.class, () -> evaluationService.confirmProbation(10L,
                ConfirmProbationRequest.builder().decision(ProbationRecommendation.PASS).build()));
    }

    @Test
    void testConfirmProbation_CannotConfirmOwnProbation() {
        evaluation.setEmployee(hr);
        hr.setEmployeeStatus(EmployementStatus.Probation);

        assertThrows(AccessDeniedException.class, () -> evaluationService.confirmProbation(10L,
                ConfirmProbationRequest.builder().decision(ProbationRecommendation.PASS).build()));
    }
}
