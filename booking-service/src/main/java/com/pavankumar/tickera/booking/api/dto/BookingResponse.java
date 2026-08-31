package com.pavankumar.tickera.booking.api.dto;

import com.pavankumar.tickera.booking.coreapi.BookingStatus;
import com.pavankumar.tickera.booking.query.BookingSummary;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire representation of a booking returned by the REST API. Carries a snapshot
 * of the booking's current state so callers never need to inspect the event store
 * directly.
 */
public record BookingResponse(
        String bookingId,
        String customerId,
        String eventName,
        int seats,
        BigDecimal amount,
        String currency,
        BookingStatus status,
        String paymentId,
        Instant updatedAt) {

    public static BookingResponse from(BookingSummary s) {
        return new BookingResponse(
                s.getBookingId(), s.getCustomerId(), s.getEventName(),
                s.getSeats(), s.getAmount(), s.getCurrency(),
                s.getStatus(), s.getPaymentId(), s.getUpdatedAt());
    }
}
