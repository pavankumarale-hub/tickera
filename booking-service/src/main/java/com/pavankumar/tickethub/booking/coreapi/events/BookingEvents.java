package com.pavankumar.tickethub.booking.coreapi.events;

import java.math.BigDecimal;

/**
 * Domain events for the booking aggregate. These are the source of truth: they
 * are appended to the event store and replayed to rebuild aggregate/projection
 * state. They stay <em>inside</em> this bounded context — only the coarse
 * {@code common-events} integration events are published to Kafka.
 */
public final class BookingEvents {

    private BookingEvents() {
    }

    public record BookingCreatedEvent(
            String bookingId,
            String customerId,
            String eventName,
            int seats,
            BigDecimal amount,
            String currency) {
    }

    public record BookingConfirmedEvent(
            String bookingId,
            String customerId,
            String eventName,
            int seats,
            BigDecimal amount,
            String currency) {
    }

    public record BookingPaidEvent(
            String bookingId,
            String paymentId) {
    }

    public record BookingCancelledEvent(
            String bookingId,
            String reason) {
    }
}
