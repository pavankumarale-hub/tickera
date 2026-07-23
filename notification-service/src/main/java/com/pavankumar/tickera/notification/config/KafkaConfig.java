package com.pavankumar.tickera.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Notification-service is a pure consumer — no producer, so no DLT recoverer.
 * Failed notifications are retried with backoff and then logged and skipped
 * (a missed notification is preferable to a stuck consumer offset).
 */
@Configuration
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxAttempts(3);
        return new DefaultErrorHandler(backOff);
    }
}
