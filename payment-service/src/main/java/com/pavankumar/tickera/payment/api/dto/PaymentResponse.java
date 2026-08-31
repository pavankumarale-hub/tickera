package com.pavankumar.tickera.payment.api.dto;

import com.pavankumar.tickera.payment.coreapi.PaymentStatus;
import com.pavankumar.tickera.payment.query.PaymentSummary;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire representation of a payment outcome returned by the REST API. The
 * {@code reason} field is populated only for {@code DECLINED} payments.
 */
public record PaymentResponse(
        String paymentId,
        String bookingId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String reason,
        Instant createdAt) {

    public static PaymentResponse from(PaymentSummary s) {
        return new PaymentResponse(
                s.getPaymentId(), s.getBookingId(), s.getAmount(),
                s.getCurrency(), s.getStatus(), s.getReason(), s.getCreatedAt());
    }
}
