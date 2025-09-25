package com.learning.messaging.exceptions;

import lombok.Getter;

import java.time.Instant;

/**
 * Wrapper exception for exhausted retries.
 * Bundles context (topic, key, event, traceId, timestamp, cause).
 */
@Getter
public class RetryableSendException extends RuntimeException {

    private final String topic;
    private final String key;
    private final Object event;
    private final String traceId;
    private final Instant timestamp;

    public RetryableSendException(String topic,
                                  String key,
                                  Object event,
                                  String traceId,
                                  Throwable cause) {
        super("Retries exhausted for topic=" + topic +
                        ", key=" + key +
                        ", traceId=" + traceId +
                        ", at=" + Instant.now(),
                cause);

        this.topic = topic;
        this.key = key;
        this.event = event;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }
}