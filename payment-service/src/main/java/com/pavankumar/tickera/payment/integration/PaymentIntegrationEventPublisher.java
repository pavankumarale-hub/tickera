package com.pavankumar.tickera.payment.integration;

import com.pavankumar.tickera.common.events.KafkaTopics;
import com.pavankumar.tickera.common.events.PaymentCompletedIntegrationEvent;
import com.pavankumar.tickera.common.events.PaymentFailedIntegrationEvent;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentDeclinedEvent;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentProcessedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes payment outcomes back onto Kafka for the booking-service saga to
 * react to. Keyed by {@code bookingId} so all events for one booking stay
 * ordered on the same partition.
 */
@Component
@ProcessingGroup("payment-integration-publisher")
public class PaymentIntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentIntegrationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentIntegrationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        PaymentCompletedIntegrationEvent out = new PaymentCompletedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.paymentId(),
                event.bookingId(),
                event.amount(),
                event.currency());
        kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, event.bookingId(), out);
        log.info("Published PaymentCompleted for booking {}", event.bookingId());
    }

    @EventHandler
    public void on(PaymentDeclinedEvent event) {
        PaymentFailedIntegrationEvent out = new PaymentFailedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.paymentId(),
                event.bookingId(),
                event.reason());
        kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, event.bookingId(), out);
        log.info("Published PaymentFailed for booking {}", event.bookingId());
    }
}
