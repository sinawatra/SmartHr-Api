package com.smarthr.smarthr.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EvaluationItemResponse {
    private Integer criteriaId;
    private String criteriaName;
    private String criteriaDescription;
    private Integer score;
}
