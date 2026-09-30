package com.smarthr.smarthr.evaluation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.evaluation.dto.EvaluationCriteriaResponse;
import com.smarthr.smarthr.evaluation.service.EvaluationCriteriaService;

import lombok.RequiredArgsConstructor;

/**
 * Controller exposing the evaluation criteria template used to build
 * evaluation forms (e.g., Job Knowledge & Skills, Quality of Work, ...).
 */
@RestController
@RequestMapping({"/api/v1/evaluation-criteria", "/api/v1/evaluation-criteria/"})
@RequiredArgsConstructor
public class EvaluationCriteriaController {

    private final EvaluationCriteriaService evaluationCriteriaService;

    /**
     * Get all evaluation criteria, ordered for display.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<EvaluationCriteriaResponse>>> getAllCriteria() {
        List<EvaluationCriteriaResponse> criteria = evaluationCriteriaService.getAllCriteria();
        return ResponseEntity.ok(ApiResponse.success(criteria));
    }
}
