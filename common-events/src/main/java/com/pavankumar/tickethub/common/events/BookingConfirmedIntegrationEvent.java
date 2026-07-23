package com.pavankumar.tickethub.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Emitted on {@link KafkaTopics#BOOKING_EVENTS} once a booking has reserved
 * seats and is awaiting payment. This is the trigger for {@code payment-service}.
 *
 * <p>{@code eventId} is the idempotency key: consumers dedupe on it so a Kafka
 * redelivery never double-charges. See {@code docs/adr/0003-idempotency-strategy.md}.
 */
public record BookingConfirmedIntegrationEvent(
        String eventId,
        String bookingId,
        String customerId,
        String eventName,
        int seats,
        BigDecimal amount,
        String currency) {

    @JsonCreator
    public BookingConfirmedIntegrationEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("bookingId") String bookingId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("eventName") String eventName,
            @JsonProperty("seats") int seats,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") String currency) {
        this.eventId = eventId;
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.eventName = eventName;
        this.seats = seats;
        this.amount = amount;
        this.currency = currency;
    }
}
