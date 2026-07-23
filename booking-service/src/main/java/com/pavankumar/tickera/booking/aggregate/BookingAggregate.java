package com.pavankumar.tickera.booking.aggregate;

import com.pavankumar.tickera.booking.coreapi.BookingStatus;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CancelBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.ConfirmBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CreateBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.MarkBookingPaidCommand;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCancelledEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingConfirmedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCreatedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingPaidEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * Event-sourced booking aggregate.
 *
 * <p>Command handlers validate an intent against current state and, if valid,
 * {@code apply(...)} an event — they never mutate fields directly. All state
 * changes happen in {@code @EventSourcingHandler} methods, which Axon also
 * replays from the event store to rebuild the aggregate on load. That split is
 * the whole point of event sourcing: the events are the record, the fields are
 * a derived cache.
 */
@Aggregate
public class BookingAggregate {

    @AggregateIdentifier
    private String bookingId;
    private String customerId;
    private String eventName;
    private int seats;
    private BigDecimal amount;
    private String currency;
    private BookingStatus status;

    protected BookingAggregate() {
        // Required by Axon to reconstruct the aggregate before replay.
    }

    @CommandHandler
    public BookingAggregate(CreateBookingCommand cmd) {
        if (cmd.seats() <= 0) {
            throw new IllegalArgumentException("A booking must reserve at least one seat");
        }
        if (cmd.amount() == null || cmd.amount().signum() <= 0) {
            throw new IllegalArgumentException("Booking amount must be positive");
        }
        apply(new BookingCreatedEvent(
                cmd.bookingId(), cmd.customerId(), cmd.eventName(),
                cmd.seats(), cmd.amount(), cmd.currency()));
    }

    @CommandHandler
    public void handle(ConfirmBookingCommand cmd) {
        if (status != BookingStatus.CREATED) {
            throw new IllegalStateException(
                    "Only a CREATED booking can be confirmed (was " + status + ")");
        }
        apply(new BookingConfirmedEvent(
                bookingId, customerId, eventName, seats, amount, currency));
    }

    @CommandHandler
    public void handle(MarkBookingPaidCommand cmd) {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only a CONFIRMED booking can be marked paid (was " + status + ")");
        }
        apply(new BookingPaidEvent(bookingId, cmd.paymentId()));
    }

    @CommandHandler
    public void handle(CancelBookingCommand cmd) {
        if (status == BookingStatus.PAID || status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a booking in state " + status);
        }
        apply(new BookingCancelledEvent(bookingId, cmd.reason()));
    }

    @EventSourcingHandler
    public void on(BookingCreatedEvent event) {
        this.bookingId = event.bookingId();
        this.customerId = event.customerId();
        this.eventName = event.eventName();
        this.seats = event.seats();
        this.amount = event.amount();
        this.currency = event.currency();
        this.status = BookingStatus.CREATED;
    }

    @EventSourcingHandler
    public void on(BookingConfirmedEvent event) {
        this.status = BookingStatus.CONFIRMED;
    }

    @EventSourcingHandler
    public void on(BookingPaidEvent event) {
        this.status = BookingStatus.PAID;
    }

    @EventSourcingHandler
    public void on(BookingCancelledEvent event) {
        this.status = BookingStatus.CANCELLED;
    }
}
