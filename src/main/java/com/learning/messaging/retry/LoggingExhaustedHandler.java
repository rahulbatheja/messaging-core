package com.learning.messaging.retry;

import com.learning.messaging.exceptions.RetryableSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logs exhausted retries for visibility.
 */
@Slf4j
@Component
public class LoggingExhaustedHandler implements RetryExhaustedHandler {

    @Override
    public void onExhausted(RetryableSendException error) {
        log.error(" Exhausted retries [topic={}, key={}, traceId={}, timestamp={}, event={}, error={}]",
                error.getTopic(),
                error.getKey(),
                error.getTraceId(),
                error.getTimestamp(),
                error.getEvent(),
                error.getCause().toString(),
                error.getCause());
    }
}