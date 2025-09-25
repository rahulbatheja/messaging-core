package com.learning.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Configuration for retry infrastructure.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RetryProperties.class)
public class RetryConfig {

    /**
     * Shared scheduled executor for retry tasks.
     * <p>
     * - Thread count is configurable via messaging.retry.scheduler-threads
     * - Threads named "messaging-retry-scheduler-<id>"
     * - Daemon threads → won't block JVM shutdown
     * - Uncaught exceptions are logged
     * - Spring calls shutdown() automatically on context close
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService messagingRetryScheduler(RetryProperties props) {
        int threads = Math.max(1, props.getSchedulerThreads());

        return new ScheduledThreadPoolExecutor(threads, r -> {
            Thread t = new Thread(r);
            t.setName("messaging-retry-scheduler-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }
}