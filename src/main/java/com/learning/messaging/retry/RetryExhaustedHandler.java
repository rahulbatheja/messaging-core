package com.learning.messaging.retry;

import com.learning.messaging.exceptions.RetryableSendException;

/**
 * Callback for when retries are exhausted and the task still failed.
 *
 * Multiple handlers can be registered; all will be invoked.
 */
public interface RetryExhaustedHandler {

    /**
     * Invoked when retries are exhausted.
     *
     * @param error wrapper containing original topic, key, event, and cause
     */
    void onExhausted(RetryableSendException error);
}