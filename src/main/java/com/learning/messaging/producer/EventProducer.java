package com.learning.messaging.producer;

import com.learning.messaging.dto.PublishResult;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for publishing domain events to Kafka (or other brokers).
 *
 * @param <T> the type of event payload
 */
public interface EventProducer<T> {

    /**
     * Send an event to a topic (no explicit key).
     * Partitioning will follow Kafka's default strategy (sticky or round-robin).
     *
     * @param topic the Kafka topic (must not be null)
     * @param event the event payload (must not be null)
     * @return a future representing the async publish result
     */
    CompletableFuture<PublishResult> send(@NonNull String topic,
                                          @NonNull T event);

    /**
     * Send an event with an optional partitioning key.
     * When key is non-null, Kafka routes consistently to the same partition.
     *
     * @param topic the Kafka topic (must not be null)
     * @param key   optional partitioning key (nullable)
     * @param event the event payload (must not be null)
     * @return a future representing the async publish result
     */
    CompletableFuture<PublishResult> send(@NonNull String topic,
                                          @Nullable String key,
                                          @NonNull T event);
}