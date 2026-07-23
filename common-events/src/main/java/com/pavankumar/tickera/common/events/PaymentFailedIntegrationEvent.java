package com.pavankumar.tickera.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Emitted on {@link KafkaTopics#PAYMENT_EVENTS} when a payment is declined.
 * Consumed by the {@code BookingSaga} to compensate (cancel the booking and
 * release the held seats).
 */
public record PaymentFailedIntegrationEvent(
        String eventId,
        String paymentId,
        String bookingId,
        String reason) {

    @JsonCreator
    public PaymentFailedIntegrationEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("paymentId") String paymentId,
            @JsonProperty("bookingId") String bookingId,
            @JsonProperty("reason") String reason) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.reason = reason;
    }
}
