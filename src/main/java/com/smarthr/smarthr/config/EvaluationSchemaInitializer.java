package com.smarthr.smarthr.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.smarthr.smarthr.evaluation.entity.EvaluationStatus;

import lombok.RequiredArgsConstructor;

/**
 * Keeps the evaluations.status check constraint in sync with {@link EvaluationStatus}.
 *
 * <p>Hibernate's ddl-auto=update creates the constraint with the enum values that existed when the
 * table was first created, but never updates it, so saving a newer value (e.g. CONFIRMED) fails.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class EvaluationSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        String allowed = Arrays.stream(EvaluationStatus.values())
                .map(status -> "'" + status.name() + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE evaluations DROP CONSTRAINT IF EXISTS evaluations_status_check");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD CONSTRAINT evaluations_status_check CHECK (status IN (" + allowed + "))");
    }
}
