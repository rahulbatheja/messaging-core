package com.learning.messaging.retry;

import com.learning.messaging.config.RetryProperties;
import com.learning.messaging.exceptions.RetryableSendException;
import com.learning.messaging.producer.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends exhausted events to DLQ topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqExhaustedHandler implements RetryExhaustedHandler {

    private final EventProducer<Object> producer;  // generic producer
    private final RetryProperties props;

    @Override
    public void onExhausted(RetryableSendException error) {
        String dlqTopic = props.resolveDlqTopic(error.getTopic());

        log.warn("🚨 Sending to DLQ [dlqTopic={}, originalTopic={}, key={}, traceId={}, timestamp={}]",
                dlqTopic,
                error.getTopic(),
                error.getKey(),
                error.getTraceId(),
                error.getTimestamp());

        producer.send(dlqTopic, error.getKey(), error.getEvent());
    }

}