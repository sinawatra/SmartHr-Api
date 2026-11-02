package com.smarthr.smarthr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} jobs such as the daily probation-ending reminder.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
