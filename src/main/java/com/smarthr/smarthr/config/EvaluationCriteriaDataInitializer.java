package com.smarthr.smarthr.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smarthr.smarthr.evaluation.entity.EvaluationCriteriaEntity;
import com.smarthr.smarthr.evaluation.repository.EvaluationCriteriaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EvaluationCriteriaDataInitializer implements CommandLineRunner {

    private final EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Override
    public void run(String... args) {
        if (evaluationCriteriaRepository.count() == 0) {

            List<EvaluationCriteriaEntity> defaultCriteria = List.of(
                    EvaluationCriteriaEntity.builder()
                            .name("Job Knowledge & Skills")
                            .description("Understanding of role responsibilities, tools, and processes.")
                            .displayOrder(1)
                            .build(),
                    EvaluationCriteriaEntity.builder()
                            .name("Quality of Work")
                            .description("Accuracy, thoroughness, and consistency of output.")
                            .displayOrder(2)
                            .build(),
                    EvaluationCriteriaEntity.builder()
                            .name("Productivity & Efficiency")
                            .description("Ability to meet deadlines and manage workload.")
                            .displayOrder(3)
                            .build(),
                    EvaluationCriteriaEntity.builder()
                            .name("Communication & Teamwork")
                            .description("Collaboration with peers, managers, and stakeholders.")
                            .displayOrder(4)
                            .build(),
                    EvaluationCriteriaEntity.builder()
                            .name("Attendance & Punctuality")
                            .description("Reliability, timeliness, and adherence to schedule.")
                            .displayOrder(5)
                            .build(),
                    EvaluationCriteriaEntity.builder()
                            .name("Initiative & Problem-Solving")
                            .description("Proactivity, ownership, and independent judgement.")
                            .displayOrder(6)
                            .build()
            );

            evaluationCriteriaRepository.saveAll(defaultCriteria);
        }
    }
}
