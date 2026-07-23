package com.pavankumar.tickera.payment.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

/**
 * Instructs the payment aggregate to attempt a charge for a confirmed booking.
 */
public record ProcessPaymentCommand(
        @TargetAggregateIdentifier String paymentId,
        String bookingId,
        String customerId,
        BigDecimal amount,
        String currency) {
}
