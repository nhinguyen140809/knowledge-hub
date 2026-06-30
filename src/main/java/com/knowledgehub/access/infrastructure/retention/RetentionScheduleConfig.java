package com.knowledgehub.access.infrastructure.retention;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduled credential-retention purge. */
@Configuration
@EnableScheduling
class RetentionScheduleConfig {}
