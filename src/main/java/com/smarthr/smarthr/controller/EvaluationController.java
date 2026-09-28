package com.smarthr.smarthr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.smarthr.smarthr.request.SubmitEvaluationRequest;
import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.EvaluationResponse;
import com.smarthr.smarthr.service.EvaluationService;

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
}
