package com.pavankumar.tickethub.payment.coreapi.events;

import java.math.BigDecimal;

/**
 * Internal payment domain events (event-sourced). The public Kafka contracts in
 * {@code common-events} are derived from these by the integration publisher.
 */
public final class PaymentEvents {

    private PaymentEvents() {
    }

    public record PaymentProcessedEvent(
            String paymentId,
            String bookingId,
            BigDecimal amount,
            String currency) {
    }

    public record PaymentDeclinedEvent(
            String paymentId,
            String bookingId,
            String reason) {
    }
}
