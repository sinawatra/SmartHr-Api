package com.smarthr.smarthr.response;

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
public class EvaluationCriteriaResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer displayOrder;
}
