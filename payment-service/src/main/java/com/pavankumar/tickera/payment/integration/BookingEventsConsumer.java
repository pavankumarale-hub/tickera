package com.pavankumar.tickera.payment.integration;

import com.pavankumar.tickera.common.events.BookingConfirmedIntegrationEvent;
import com.pavankumar.tickera.common.events.KafkaTopics;
import com.pavankumar.tickera.payment.coreapi.commands.ProcessPaymentCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes {@code BookingConfirmed} integration events and triggers a payment.
 * The {@link IdempotencyGuard} makes the handler safe against Kafka's
 * at-least-once redelivery: a duplicate {@code eventId} never results in a
 * second charge.
 */
@Component
@KafkaListener(topics = KafkaTopics.BOOKING_EVENTS, groupId = "payment-service")
public class BookingEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventsConsumer.class);

    private final CommandGateway commandGateway;
    private final IdempotencyGuard idempotencyGuard;

    public BookingEventsConsumer(CommandGateway commandGateway, IdempotencyGuard idempotencyGuard) {
        this.commandGateway = commandGateway;
        this.idempotencyGuard = idempotencyGuard;
    }

    @KafkaHandler
    public void on(BookingConfirmedIntegrationEvent event) {
        if (!idempotencyGuard.firstDelivery(event.eventId())) {
            log.info("Duplicate BookingConfirmed {} for booking {} — skipping",
                    event.eventId(), event.bookingId());
            return;
        }
        String paymentId = UUID.randomUUID().toString();
        log.info("Charging booking {} ({} {}) as payment {}",
                event.bookingId(), event.amount(), event.currency(), paymentId);
        commandGateway.send(new ProcessPaymentCommand(
                paymentId, event.bookingId(), event.customerId(),
                event.amount(), event.currency()));
    }

    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.debug("Ignoring unrecognised booking-events payload: {}", payload.getClass());
    }
}
