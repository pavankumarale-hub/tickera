package com.pavankumar.tickethub.payment.api.dto;

import com.pavankumar.tickethub.payment.coreapi.PaymentStatus;
import com.pavankumar.tickethub.payment.query.PaymentSummary;

import java.math.BigDecimal;
import java.time.Instant;

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
