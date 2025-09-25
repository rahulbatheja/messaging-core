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
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    public void retry(String topic, String key, T event, Runnable task) {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        attempt(topic, key, event, task, 1, traceId);
    }

    private void attempt(String topic, String key, T event, Runnable task, int attemptNo, String traceId) {
        try {
            task.run();
            log.info("Retry success [topic={}, key={}, attempt={}, traceId={}]",
                    topic, key, attemptNo, traceId);
            count(METRIC_SUCCESS, topic);
        } catch (Exception exception) {
            log.warn(" Retry failed [topic={}, key={}, attempt={}, traceId={}, error={}]",
                    topic, key, attemptNo, traceId, exception.toString());

            if (attemptNo < props.getMaxAttempts()) {
                long delayMs = props.backoffForAttempt(attemptNo);
                log.debug(" Scheduling retry [topic={}, key={}, nextAttempt={}, delayMs={}, traceId={}]",
                        topic, key, attemptNo + 1, delayMs, traceId);

                count(METRIC_SCHEDULED, topic);

                this.scheduler.schedule(() -> {
                    attempt(topic, key, event, task, attemptNo + 1, traceId);
                }, delayMs, TimeUnit.MILLISECONDS);
            } else {
                log.error("Retry exhausted [topic={}, key={}, attempts={}, traceId={}, error={}]",
                        topic, key, props.getMaxAttempts(), traceId, exception.toString());

                count(METRIC_EXHAUSTED, topic);

                RetryableSendException retryableSendException =
                        new RetryableSendException(topic, key, event, traceId, exception);

                exhaustedHandlers.forEach(handler -> handler.onExhausted(retryableSendException));
            }

        } finally {
            MDC.clear();
        }
    }

}

