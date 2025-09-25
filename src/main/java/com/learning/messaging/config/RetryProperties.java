package com.learning.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for retry & DLQ behavior in the messaging library.
 *
 * These values are bound from application.properties / application.yml
 * using the prefix "messaging.retry".
 *
 * Example:
 *   messaging.retry.max-attempts=5
 *   messaging.retry.initial-backoff-ms=100
 *   messaging.retry.multiplier=2.0
 *   messaging.retry.max-backoff-ms=5000
 *   messaging.retry.dlq-append-suffix=true
 *   messaging.retry.dlq-topic=global.dlq
 */
@Data
@ConfigurationProperties(prefix = "messaging.producer")
public class RetryProperties {

    /**
     * Maximum number of attempts (including the first one).
     */
    private int maxAttempts = 5;

    /**
     * Initial delay before the first retry (in milliseconds).
     */
    private long initialBackoffMs = 100;

    /**
     * Backoff multiplier for exponential growth.
     * Example: 2.0 means 100ms → 200ms → 400ms → 800ms …
     */
    private double multiplier = 2.0;

    /**
     * Maximum backoff delay (in milliseconds).
     * Prevents retries from growing into minutes/hours.
     */
    private long maxBackoffMs = 5000;

    /**
     * Whether to append ".dlq" to the original topic name.
     */
    private boolean dlqAppendSuffix = true;

    /**
     * Explicit DLQ topic override.
     * If set, this takes precedence over dlqAppendSuffix.
     */
    private String dlqTopic;

    /** Number of threads in retry scheduler. Default = 2. */
    private int schedulerThreads = 2;

    // ---- Helper methods ----

    /**
     * Compute the backoff delay for a given attempt number (1-based).
     */
    public long backoffForAttempt(int attemptIndex) {
        double pow = Math.pow(multiplier, Math.max(0, attemptIndex - 1));
        return Math.min(maxBackoffMs, (long) (initialBackoffMs * pow));
    }

    /**
     * Resolve which DLQ topic to use for an original topic.
     */
    public String resolveDlqTopic(String originalTopic) {
        if (dlqTopic != null && !dlqTopic.isBlank()) {
            return dlqTopic;
        }
        return dlqAppendSuffix ? originalTopic + ".dlq" : originalTopic;
    }
}