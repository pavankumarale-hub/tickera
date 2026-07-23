package com.pavankumar.tickera.booking.integration;

import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CancelBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.MarkBookingPaidCommand;
import com.pavankumar.tickera.common.events.KafkaTopics;
import com.pavankumar.tickera.common.events.PaymentCompletedIntegrationEvent;
import com.pavankumar.tickera.common.events.PaymentFailedIntegrationEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound anti-corruption boundary: consumes payment outcomes from Kafka and
 * turns them into commands against the booking aggregate. Spring routes by the
 * {@code __TypeId__} header written by the producer, so each payload type lands
 * in its own {@link KafkaHandler}.
 *
 * <p>The aggregate itself is the idempotency guard here: {@code MarkBookingPaid}
 * on an already-PAID booking is rejected by the state check, so a redelivered
 * payment event is a no-op rather than a double transition.
 */
@Component
@KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "booking-service")
public class PaymentEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    private final CommandGateway commandGateway;

    public PaymentEventsConsumer(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @KafkaHandler
    public void on(PaymentCompletedIntegrationEvent event) {
        log.info("Payment {} completed for booking {}", event.paymentId(), event.bookingId());
        commandGateway.send(new MarkBookingPaidCommand(event.bookingId(), event.paymentId()));
    }

    @KafkaHandler
    public void on(PaymentFailedIntegrationEvent event) {
        log.warn("Payment failed for booking {}: {}", event.bookingId(), event.reason());
        commandGateway.send(new CancelBookingCommand(
                event.bookingId(), "Payment failed: " + event.reason()));
    }

    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.debug("Ignoring unrecognised payment-events payload: {}", payload.getClass());
    }
}
