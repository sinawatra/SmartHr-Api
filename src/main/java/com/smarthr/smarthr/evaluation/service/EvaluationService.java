package com.smarthr.smarthr.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.evaluation.entity.EvaluationCriteriaEntity;
import com.smarthr.smarthr.evaluation.entity.EvaluationEntity;
import com.smarthr.smarthr.evaluation.entity.EvaluationItemEntity;
import com.smarthr.smarthr.evaluation.entity.EvaluationStatus;
import com.smarthr.smarthr.evaluation.entity.ProbationRecommendation;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.evaluation.repository.EvaluationCriteriaRepository;
import com.smarthr.smarthr.evaluation.repository.EvaluationRepository;
import com.smarthr.smarthr.evaluation.dto.ConfirmProbationRequest;
import com.smarthr.smarthr.evaluation.dto.EvaluationItemRequest;
import com.smarthr.smarthr.evaluation.dto.SubmitEvaluationRequest;
import com.smarthr.smarthr.evaluation.dto.EvaluationItemResponse;
import com.smarthr.smarthr.evaluation.dto.EvaluationResponse;

import lombok.RequiredArgsConstructor;

/**
 * Service handling submission of employee performance evaluations.
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EmployeeRepository employeeRepository;
    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    private final EvaluationRepository evaluationRepository;
    private final Clock clock;

    /**
     * Submit a completed evaluation for an employee.
     *
     * <p>The caller must be the employee's direct manager
     * ({@code employee.managerId == currentUser.id}) or an ADMIN, and cannot
     * evaluate themselves.
     */
    @Transactional
    public EvaluationResponse submit(SubmitEvaluationRequest request) {
        EmployeeDetails employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + request.getEmployeeId()));

        EmployeeDetails evaluator = currentEmployee();

        boolean isAdmin = hasAuthority("ADMIN");
        boolean isDirectManager = employee.getManagerId() != null
                && Long.valueOf(employee.getManagerId().longValue()).equals(evaluator.getId());
        if (!isAdmin && !isDirectManager) {
            throw new AccessDeniedException("You are not the line manager for this employee.");
        }
        if (evaluator.getId().equals(employee.getId())) {
            throw new AccessDeniedException("You cannot submit an evaluation for yourself.");
        }

        if (request.getPeriodStart() != null && request.getPeriodEnd() != null
                && request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new IllegalArgumentException("Evaluation period end date must be on or after the start date.");
        }

        Set<Integer> seenCriteriaIds = new HashSet<>();
        List<EvaluationItemEntity> items = new ArrayList<>();
        for (EvaluationItemRequest itemRequest : request.getItems()) {
            if (!seenCriteriaIds.add(itemRequest.getCriteriaId())) {
                throw new IllegalArgumentException("Duplicate criteria in submission: " + itemRequest.getCriteriaId());
            }
            EvaluationCriteriaEntity criteria = evaluationCriteriaRepository.findById(itemRequest.getCriteriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid criteria ID: " + itemRequest.getCriteriaId()));

            EvaluationItemEntity item = new EvaluationItemEntity();
            item.setCriteria(criteria);
            item.setScore(itemRequest.getScore());
            items.add(item);
        }

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setEmployee(employee);
        evaluation.setEvaluator(evaluator);
        evaluation.setPeriodStart(request.getPeriodStart());
        evaluation.setPeriodEnd(request.getPeriodEnd());
        evaluation.setOverallComment(request.getOverallComment());
        evaluation.setProbationRecommendation(request.getProbationRecommendation());
        evaluation.setStatus(EvaluationStatus.SUBMITTED);

        for (EvaluationItemEntity item : items) {
            item.setEvaluation(evaluation);
        }
        evaluation.setItems(items);

        EvaluationEntity saved = evaluationRepository.save(evaluation);
        return mapToResponse(saved);
    }

    /**
     * Probation evaluations submitted by managers and waiting for HR to confirm, oldest first.
     */
    @Transactional(readOnly = true)
    public PagedResponse<EvaluationResponse> getPendingProbationConfirmations(Pageable pageable) {
        Page<EvaluationEntity> page = evaluationRepository
                .findByStatusAndProbationRecommendationIsNotNull(EvaluationStatus.SUBMITTED, pageable);
        List<EvaluationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    /**
     * HR's final probation decision on a submitted evaluation. HR may follow or override the
     * manager's recommendation, and the decision is applied to the employee:
     * <ul>
     *   <li>PASS: employment status becomes FullStaff</li>
     *   <li>FAIL: employment status becomes Terminated, ending on {@code lastWorkingDate} (default today)</li>
     *   <li>DELAY: stays on probation, with the probation end date moved to {@code newProbationEndDate}</li>
     * </ul>
     */
    @Transactional
    public EvaluationResponse confirmProbation(Long evaluationId, ConfirmProbationRequest request) {
        EvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found with ID: " + evaluationId));

        if (evaluation.getProbationRecommendation() == null) {
            throw new IllegalArgumentException("Evaluation " + evaluationId + " is not a probation evaluation.");
        }
        if (evaluation.getStatus() == EvaluationStatus.CONFIRMED) {
            throw new IllegalStateException("Evaluation " + evaluationId + " has already been confirmed as "
                    + evaluation.getHrDecision() + ".");
        }
        if (evaluation.getStatus() != EvaluationStatus.SUBMITTED) {
            throw new IllegalStateException("Evaluation " + evaluationId + " has not been submitted by the manager yet.");
        }

        EmployeeDetails employee = evaluation.getEmployee();
        if (employee.getEmployeeStatus() != EmployementStatus.Probation) {
            throw new IllegalStateException("Employee " + employee.getId() + " is not on probation (status: "
                    + employee.getEmployeeStatus() + ").");
        }

        EmployeeDetails hr = currentEmployee();
        if (hr.getId().equals(employee.getId())) {
            throw new AccessDeniedException("You cannot confirm your own probation.");
        }

        LocalDate today = LocalDate.now(clock);
        ProbationRecommendation decision = request.getDecision();
        switch (decision) {
            case PASS -> employee.setEmployeeStatus(EmployementStatus.FullStaff);
            case FAIL -> {
                employee.setEmployeeStatus(EmployementStatus.Terminated);
                employee.setEndDate(request.getLastWorkingDate() != null ? request.getLastWorkingDate() : today);
            }
            case DELAY -> {
                LocalDate newEndDate = request.getNewProbationEndDate();
                if (newEndDate == null) {
                    throw new IllegalArgumentException("New probation end date is required to delay probation.");
                }
                LocalDate currentEndDate = employee.getProbationEndDate();
                if (!newEndDate.isAfter(currentEndDate != null && currentEndDate.isAfter(today) ? currentEndDate : today)) {
                    throw new IllegalArgumentException("New probation end date must be after the current probation end date and today.");
                }
                employee.setProbationEndDate(newEndDate);
            }
        }
        employeeRepository.save(employee);

        evaluation.setHrDecision(decision);
        evaluation.setHrComment(request.getComment());
        evaluation.setHrDecidedBy(hr);
        evaluation.setHrDecidedAt(LocalDateTime.now(clock));
        evaluation.setStatus(EvaluationStatus.CONFIRMED);

        return mapToResponse(evaluationRepository.save(evaluation));
    }

    private EvaluationResponse mapToResponse(EvaluationEntity entity) {
        List<EvaluationItemResponse> items = entity.getItems().stream()
                .map(item -> EvaluationItemResponse.builder()
                        .criteriaId(item.getCriteria().getId())
                        .criteriaName(item.getCriteria().getName())
                        .criteriaDescription(item.getCriteria().getDescription())
                        .score(item.getScore())
                        .build())
                .collect(Collectors.toList());

        double overallScore = items.stream()
                .mapToInt(EvaluationItemResponse::getScore)
                .average()
                .orElse(0.0);

        return EvaluationResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .employeeName(displayName(entity.getEmployee()))
                .evaluatorId(entity.getEvaluator().getId())
                .evaluatorName(displayName(entity.getEvaluator()))
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .status(entity.getStatus().name())
                .overallComment(entity.getOverallComment())
                .probationRecommendation(entity.getProbationRecommendation() != null
                        ? entity.getProbationRecommendation().name() : null)
                .hrDecision(entity.getHrDecision() != null ? entity.getHrDecision().name() : null)
                .hrComment(entity.getHrComment())
                .hrDecidedById(entity.getHrDecidedBy() != null ? entity.getHrDecidedBy().getId() : null)
                .hrDecidedByName(entity.getHrDecidedBy() != null ? displayName(entity.getHrDecidedBy()) : null)
                .hrDecidedAt(entity.getHrDecidedAt())
                .overallScore(overallScore)
                .items(items)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String displayName(EmployeeDetails employee) {
        String first = employee.getFirstName();
        String last = employee.getLastName();
        if (first != null && last != null) {
            return (first + " " + last).trim();
        }
        if (first != null) {
            return first;
        }
        return employee.getUsername();
    }

    /** The employee behind the current authentication, or an error if none. */
    private EmployeeDetails currentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No authenticated user.");
        }
        String username = auth.getName();
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));
    }

    /** Whether the current authentication carries the given authority. */
    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
