package com.smarthr.smarthr.evaluation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.evaluation.entity.EvaluationCriteriaEntity;
import com.smarthr.smarthr.evaluation.repository.EvaluationCriteriaRepository;
import com.smarthr.smarthr.evaluation.dto.EvaluationCriteriaResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluationCriteriaService {

    private final EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Transactional(readOnly = true)
    public List<EvaluationCriteriaResponse> getAllCriteria() {
        return evaluationCriteriaRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EvaluationCriteriaResponse mapToResponse(EvaluationCriteriaEntity entity) {
        return EvaluationCriteriaResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
