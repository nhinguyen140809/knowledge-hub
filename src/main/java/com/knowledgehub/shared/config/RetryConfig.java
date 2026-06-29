package com.knowledgehub.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Turns on Spring Retry so {@code @Retryable} adapters (e.g. the embedding call) get retry with
 * backoff via AOP. Retry policy stays on the outbound adapter, not on services.
 */
@Configuration
@EnableRetry
class RetryConfig {}
