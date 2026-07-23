package com.pavankumar.tickera.booking.api.dto;

import com.pavankumar.tickera.booking.coreapi.BookingStatus;
import com.pavankumar.tickera.booking.query.BookingSummary;

import java.math.BigDecimal;
import java.time.Instant;

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
