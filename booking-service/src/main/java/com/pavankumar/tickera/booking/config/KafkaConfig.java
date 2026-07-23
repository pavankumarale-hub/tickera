package com.pavankumar.tickera.booking.config;

import com.pavankumar.tickera.common.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Explicit Kafka topic declarations and consumer error handling.
 *
 * <p><b>Topics:</b> declaring topics here means the broker creates them on first
 * startup with the right partition/replication settings, rather than relying on
 * Kafka's auto-create (which uses broker defaults, often unsuitable for
 * production). A {@link NewTopic} bean is idempotent if the topic already exists.
 *
 * <p><b>Error handler:</b> Kafka gives at-least-once delivery, but transient
 * failures (DB hiccup, downstream timeout) should not immediately move the
 * message to the DLT. The {@link ExponentialBackOff} retries 3 times (1s → 2s
 * → 4s) before the {@link DeadLetterPublishingRecoverer} publishes the poisoned
 * record to {@code <topic>.DLT} for offline investigation — same pattern used
 * in production for L2 triage.
 */
@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;   // increase to 3 in a multi-broker cluster

    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name(KafkaTopics.BOOKING_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic bookingEventsDlt() {
        return TopicBuilder.name(KafkaTopics.BOOKING_EVENTS + ".DLT")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentEventsDlt() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS + ".DLT")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    /**
     * Applies to all {@code @KafkaListener} containers in this service.
     * Three retries with exponential backoff; exhausted records go to the DLT.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxAttempts(3);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
