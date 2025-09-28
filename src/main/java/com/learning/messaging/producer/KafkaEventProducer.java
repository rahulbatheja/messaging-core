package com.learning.messaging.producer;

import com.learning.messaging.dto.PublishResult;
import com.learning.messaging.exceptions.RetryableSendException;
import com.learning.messaging.retry.RetryExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Kafka-based implementation of EventProducer<T>.
 *
 * @param <T> type of event payload
 */
@Slf4j
@Component
public class KafkaEventProducer<T> implements EventProducer<T> {
    private static final String METRIC_PRODUCE_SUCCESS = "messaging.producer.success";
    private static final String METRIC_PRODUCE_FAILURE = "messaging.producer.failure";

    private final KafkaTemplate<String, T> kafkaTemplate;
    private final RetryExecutor<T> retryExecutor;
    private final MeterRegistry meterRegistry;

    public KafkaEventProducer(KafkaTemplate<String, T> kafkaTemplate,
                              RetryExecutor<T> retryExecutor,
                              MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.retryExecutor = retryExecutor;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public CompletableFuture<PublishResult> send(String topic, T event) {
        return send(Objects.requireNonNull(topic), null, Objects.requireNonNull(event));
    }

    /**
     * @param topic must not be null or blank
     * @param key   may be null → Kafka will choose partition
     * @param event must not be null
     */
    @Override
    public CompletableFuture<PublishResult> send(String topic, @Nullable String key, T event) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be null or blank");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        // Capture traceId (from MDC, fallback UUID)
        final String traceId = Objects.requireNonNullElseGet(
                MDC.get("traceId"),
                () -> UUID.randomUUID().toString());


        Supplier<CompletableFuture<PublishResult>> supplier = () -> {
            CompletableFuture<SendResult<String, T>> attemptFuture = kafkaTemplate.send(topic, key, event);

            // return the mapped future
            return attemptFuture.handle((sendResult, throwable) -> {
                if (throwable == null) {
                    RecordMetadata md = sendResult.getRecordMetadata();
                    PublishResult pr = PublishResult.success(
                            topic,
                            md != null ? md.partition() : -1,
                            md != null ? md.offset() : -1,
                            traceId
                    );
                    meterRegistry.counter(METRIC_PRODUCE_SUCCESS, "topic", topic).increment();
                    log.info("✅ produced [topic={}, key={}, partition={}, offset={}, traceId={}]",
                            topic, key, pr.getPartition(), pr.getOffset(), traceId);
                    return pr;
                } else {
                    log.warn("send failed [topic={}, key={}, traceId={}] → will retry if attempts left",
                            topic, key, traceId, throwable);

                    meterRegistry.counter(METRIC_PRODUCE_FAILURE, "topic", topic).increment();
                    throw new RetryableSendException(topic, key, event, traceId, throwable);
                }
            });
        };

        return this.retryExecutor.executeWithRetry(topic, key,event, supplier);
    }

}