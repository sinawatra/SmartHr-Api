package com.smarthr.smarthr.evaluation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.evaluation.entity.EvaluationEntity;
import com.smarthr.smarthr.evaluation.entity.EvaluationStatus;

@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {
    List<EvaluationEntity> findByEmployeeId(Long employeeId);
    Page<EvaluationEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    // Evaluations written by a given manager for their direct reports
    Page<EvaluationEntity> findByEvaluatorId(Long evaluatorId, Pageable pageable);

    Page<EvaluationEntity> findByStatus(EvaluationStatus status, Pageable pageable);
}
