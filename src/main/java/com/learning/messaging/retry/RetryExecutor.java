package com.learning.messaging.retry;

import com.learning.messaging.config.RetryProperties;
import com.learning.messaging.exceptions.RetryableSendException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class RetryExecutor<T> {

    private static final String METRIC_SCHEDULED = "messaging.retry.scheduled";
    private static final String METRIC_SUCCESS = "messaging.retry.success";
    private static final String METRIC_EXHAUSTED = "messaging.retry.exhausted";

    private final RetryProperties props;
    private final MeterRegistry meterRegistry;
    private final List<RetryExhaustedHandler> exhaustedHandlers;
    private final ScheduledExecutorService scheduler;

    @Autowired
    public RetryExecutor(RetryProperties props,
                         MeterRegistry meterRegistry,
                         List<RetryExhaustedHandler> exhaustedHandlers,
                         @Qualifier("messagingRetryScheduler") ScheduledExecutorService scheduler) {
        this.props = props;
        this.meterRegistry = meterRegistry;
        this.exhaustedHandlers = exhaustedHandlers;
        this.scheduler = scheduler;
    }

    private void count(String metricName, String topic) {
        meterRegistry.counter(metricName, "topic", topic).increment();
    }

    public <R> CompletableFuture<R> executeWithRetry(String topic, String key, T event, Supplier<CompletableFuture<R>> taskSupplier) {
        CompletableFuture<R> resultFuture = new CompletableFuture<>();
        attempt(topic, key, event, taskSupplier, resultFuture, 1);
        return resultFuture;
    }

    private <R> void attempt(String topic, String key, T event, Supplier<CompletableFuture<R>> taskSupplier,
                             CompletableFuture<R> outer, int attemptNo) {
        String traceId = MDC.get("traceId");
        log.debug("Attempt start [topic={}, key={}, attempt={}, traceId={}]", topic, key, attemptNo, traceId);
        try {
            CompletableFuture<R> attemptFuture = taskSupplier.get();
            attemptFuture.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Retry attempt success [topic={}, key={}, attempt={}, traceId={}]", topic, key, attemptNo, traceId);
                    count(METRIC_SUCCESS, topic);
                    outer.complete(result);
                } else {
                    long delayMs = props.backoffForAttempt(attemptNo);

                    if (attemptNo < props.getMaxAttempts()) {
                        log.warn("Retry attempt failed → scheduling retry [topic={}, key={}, attempt={}, nextAttempt={}, delayMs={}, traceId={}, error={}]",
                                topic, key, attemptNo, attemptNo + 1, delayMs, traceId, exception.toString());

                        count(METRIC_SCHEDULED, topic);

                        this.scheduler.schedule(() ->
                                        attempt(topic, key, event, taskSupplier, outer, attemptNo + 1),
                                delayMs,
                                TimeUnit.MILLISECONDS
                        );
                    } else {
                        log.error("Retry exhausted [topic={}, key={}, attempts={}, traceId={}, error={}]",
                                topic, key, props.getMaxAttempts(), traceId, exception.toString());

                        count(METRIC_EXHAUSTED, topic);

                        RetryableSendException rse =
                                new RetryableSendException(topic, key, event, traceId, exception);
                        exhaustedHandlers.forEach(h -> h.onExhausted(rse));

                        outer.completeExceptionally(rse);

                    }
                }
            });
        } catch (Exception exception) {
            outer.completeExceptionally(exception);
        } finally {
            MDC.remove("traceId");
        }


    }

}

