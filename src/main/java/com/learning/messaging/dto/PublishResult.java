package com.learning.messaging.dto;

import lombok.Value;

import java.time.Instant;

/**
 * Immutable DTO representing the outcome of publishing an event to Kafka (or DLQ).
 *
 * <p>This class separates information into two categories:
 * <ul>
 *   <li><b>Business-use fields</b>: {@link #status}, {@link #error}.
 *       These determine how client services should react (e.g. retry, DLQ handling).</li>
 *   <li><b>Operational/debug fields</b>: {@link #topic}, {@link #partition}, {@link #offset}, {@link #traceId}, {@link #timestamp}.
 *       These are for observability, logging, metrics, and debugging only — not for business logic.</li>
 * </ul>
 *
 * <p>Client services should ONLY make decisions based on {@link #status}.
 * All other fields are meant to support observability and troubleshooting.</p>
 */
@Value
public class PublishResult {

    /**
     * Overall status of the publish attempt.
     */
    Status status;

    /**
     * Kafka topic where the message landed.
     * <p>Primarily for debugging/observability — not business logic.</p>
     */
    String topic;

    /**
     * Partition assigned by Kafka.
     * <p>May be {@code null} if unknown or if the record failed before partition assignment.</p>
     */
    Integer partition;

    /**
     * Offset within the partition.
     * <p>May be {@code null} if the record never made it to a partition (e.g. failure).</p>
     */
    Long offset;

    /**
     * Exception that caused the failure, if any.
     * <p>{@code null} when status is {@link Status#SUCCESS}.</p>
     */
    Throwable error;

    /**
     * TraceId carried in MDC at the time of send.
     * <p>Useful for log correlation across microservices.</p>
     */
    String traceId;

    /**
     * Instant when the publish attempt finished.
     */
    Instant timestamp;

    /**
     * Status of the publish result.
     */
    public enum Status { SUCCESS, FAILED, DLQ }

    // ---- Factory methods ----

    /**
     * Successful publish result.
     */
    public static PublishResult success(String topic, int partition, long offset, String traceId) {
        return new PublishResult(Status.SUCCESS, topic, partition, offset, null, traceId, Instant.now());
    }

    /**
     * Failed publish result.
     */
    public static PublishResult failed(String topic, Throwable error, String traceId) {
        return new PublishResult(Status.FAILED, topic, null, null, error, traceId, Instant.now());
    }

    /**
     * Dead-lettered publish result (when message is pushed to DLQ).
     */
    public static PublishResult dlq(String dlqTopic, Throwable error, String traceId) {
        return new PublishResult(Status.DLQ, dlqTopic, null, null, error, traceId, Instant.now());
    }
}