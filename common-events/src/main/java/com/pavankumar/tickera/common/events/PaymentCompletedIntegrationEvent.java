package com.pavankumar.tickera.common.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Emitted on {@link KafkaTopics#PAYMENT_EVENTS} when a payment settles.
 * Consumed by the {@code BookingSaga} to mark the booking as PAID.
 */
public record PaymentCompletedIntegrationEvent(
        @JsonProperty("eventId")   String eventId,
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("bookingId") String bookingId,
        @JsonProperty("amount")    BigDecimal amount,
        @JsonProperty("currency")  String currency) {
}
