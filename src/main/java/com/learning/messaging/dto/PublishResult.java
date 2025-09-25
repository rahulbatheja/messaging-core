package com.learning.messaging.dto;


import lombok.Value;

/**
 * Result of publishing an event to Kafka (or DLQ).
 *
 * <p>Fields are divided into two groups:
 * <ul>
 *   <li><b>Business-use</b>: {@link #status}, {@link #error}</li>
 *   <li><b>Operational/debug</b>: {@link #topic}, {@link #partition}, {@link #offset}, {@link #traceId}</li>
 * </ul>
 *
 * <p>Client services should only make business decisions on {@link #status}.
 * The rest are for observability (logs/metrics/debug).
 */
@Value
public class PublishResult {

    /**
     * Overall status of the publish attempt.
     */
    Status status;

    /**
     * Kafka topic where the message landed (debug only).
     */
    String topic;

    /**
     * Partition assigned (debug only, may be null).
     */
    Integer partition;

    /**
     * Offset in partition (debug only, may be null).
     */
    Long offset;

    /**
     * Exception that caused the failure, if any.
     */
    Throwable error;

    /**
     * TraceId carried in MDC at the time of send (for log correlation).
     */
    String traceId;

    public enum Status {SUCCESS, FAILED, DLQ}

    // ---- Factory methods ----

    public static PublishResult success(String topic, int partition, long offset, String traceId) {
        return new PublishResult(Status.SUCCESS, topic, partition, offset, null, traceId);
    }

    public static PublishResult failed(String topic, Throwable error, String traceId) {
        return new PublishResult(Status.FAILED, topic, null, null, error, traceId);
    }

    public static PublishResult dlq(String dlqTopic, Throwable error, String traceId) {
        return new PublishResult(Status.DLQ, dlqTopic, null, null, error, traceId);
    }
}