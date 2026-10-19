package com.smarthr.smarthr.evaluation.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.smarthr.smarthr.evaluation.dto.SubmitEvaluationRequest;
import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.evaluation.dto.ConfirmProbationRequest;
import com.smarthr.smarthr.evaluation.dto.EvaluationResponse;
import com.smarthr.smarthr.evaluation.service.EvaluationService;

import lombok.RequiredArgsConstructor;

/**
 * Controller for submitting employee performance evaluations.
 */
@RestController
@RequestMapping({"/api/v1/evaluations", "/api/v1/evaluations/"})
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * Submit a completed evaluation for an employee. Only the employee's
     * direct manager (LINE_MANAGER) or an ADMIN may submit one.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> submitEvaluation(
            @Valid @RequestBody SubmitEvaluationRequest request) {
        EvaluationResponse response = evaluationService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evaluation submitted successfully", response));
    }

    /**
     * Probation evaluations submitted by managers and waiting for HR to confirm, oldest first.
     */
    @GetMapping("/probation/pending")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<PagedResponse<EvaluationResponse>>> getPendingProbationConfirmations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<EvaluationResponse> pending = evaluationService.getPendingProbationConfirmations(
                PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return ResponseEntity.ok(ApiResponse.success("Pending probation evaluations retrieved successfully", pending));
    }

    /**
     * HR's final probation decision (PASS, FAIL or DELAY) on a manager's evaluation.
     * Updates the employee's employment status or probation end date accordingly.
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> confirmProbation(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmProbationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Probation decision confirmed successfully",
                evaluationService.confirmProbation(id, request)));
    }
}
