package com.pavankumar.tickera.booking.integration;

import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingConfirmedEvent;
import com.pavankumar.tickera.common.events.BookingConfirmedIntegrationEvent;
import com.pavankumar.tickera.common.events.KafkaTopics;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Anti-corruption boundary: translates the internal {@link BookingConfirmedEvent}
 * domain event into the public {@link BookingConfirmedIntegrationEvent} contract
 * and publishes it to Kafka for other bounded contexts.
 *
 * <p>Runs in its own processing group so a slow/failed Kafka publish is isolated
 * from the read-model projection. The message is keyed by {@code bookingId} to
 * preserve per-booking ordering, and carries a fresh {@code eventId} that
 * downstream consumers use as their idempotency key.
 */
@Component
@ProcessingGroup("integration-publisher")
public class BookingIntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingIntegrationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookingIntegrationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @EventHandler
    public void on(BookingConfirmedEvent event) {
        BookingConfirmedIntegrationEvent integrationEvent = new BookingConfirmedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.bookingId(),
                event.customerId(),
                event.eventName(),
                event.seats(),
                event.amount(),
                event.currency());
        kafkaTemplate.send(KafkaTopics.BOOKING_EVENTS, event.bookingId(), integrationEvent);
        log.info("Published BookingConfirmed integration event for booking {}", event.bookingId());
    }
}
