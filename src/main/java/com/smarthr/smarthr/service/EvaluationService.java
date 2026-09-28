package com.smarthr.smarthr.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.EvaluationCriteriaEntity;
import com.smarthr.smarthr.entity.EvaluationEntity;
import com.smarthr.smarthr.entity.EvaluationItemEntity;
import com.smarthr.smarthr.enumeration.EvaluationStatus;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.repository.EvaluationCriteriaRepository;
import com.smarthr.smarthr.repository.EvaluationRepository;
import com.smarthr.smarthr.request.EvaluationItemRequest;
import com.smarthr.smarthr.request.SubmitEvaluationRequest;
import com.smarthr.smarthr.response.EvaluationItemResponse;
import com.smarthr.smarthr.response.EvaluationResponse;

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
        evaluation.setStatus(EvaluationStatus.SUBMITTED);

        for (EvaluationItemEntity item : items) {
            item.setEvaluation(evaluation);
        }
        evaluation.setItems(items);

        EvaluationEntity saved = evaluationRepository.save(evaluation);
        return mapToResponse(saved);
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
