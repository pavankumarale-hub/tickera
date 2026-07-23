package com.pavankumar.tickera.booking.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

/**
 * Commands routed to {@code BookingAggregate}. Grouped in one file because they
 * form a single small vocabulary; each carries the aggregate id it targets.
 */
public final class BookingCommands {

    private BookingCommands() {
    }

    public record CreateBookingCommand(
            @TargetAggregateIdentifier String bookingId,
            String customerId,
            String eventName,
            int seats,
            BigDecimal amount,
            String currency) {
    }

    public record ConfirmBookingCommand(
            @TargetAggregateIdentifier String bookingId) {
    }

    public record MarkBookingPaidCommand(
            @TargetAggregateIdentifier String bookingId,
            String paymentId) {
    }

    public record CancelBookingCommand(
            @TargetAggregateIdentifier String bookingId,
            String reason) {
    }
}
