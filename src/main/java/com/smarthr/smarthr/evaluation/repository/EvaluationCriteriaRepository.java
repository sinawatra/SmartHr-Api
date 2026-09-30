package com.smarthr.smarthr.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.evaluation.entity.EvaluationCriteriaEntity;

@Repository
public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteriaEntity, Integer> {
    List<EvaluationCriteriaEntity> findAllByOrderByDisplayOrderAsc();
}
