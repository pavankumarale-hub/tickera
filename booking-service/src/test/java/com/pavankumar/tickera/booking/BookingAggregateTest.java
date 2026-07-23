package com.pavankumar.tickera.booking;

import com.pavankumar.tickera.booking.aggregate.BookingAggregate;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CancelBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.ConfirmBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CreateBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.MarkBookingPaidCommand;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCancelledEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingConfirmedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCreatedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingPaidEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Given/when/then tests over the event stream — no Spring, no database, no Kafka.
 * This is the fast inner loop of TDD on an event-sourced aggregate: you assert on
 * the events produced (or the exception thrown), not on mutable getters.
 */
class BookingAggregateTest {

    private static final String ID = "b-1";
    private static final BigDecimal AMOUNT = new BigDecimal("120.00");

    private FixtureConfiguration<BookingAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(BookingAggregate.class);
    }

    @Test
    void createBooking_emitsCreatedEvent() {
        fixture.givenNoPriorActivity()
                .when(new CreateBookingCommand(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new BookingCreatedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"));
    }

    @Test
    void createBooking_rejectsNonPositiveSeats() {
        fixture.givenNoPriorActivity()
                .when(new CreateBookingCommand(ID, "cust-1", "Jazz Night", 0, AMOUNT, "USD"))
                .expectException(IllegalArgumentException.class);
    }

    @Test
    void confirm_thenPay_walksTheHappyPath() {
        fixture.given(new BookingCreatedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"))
                .when(new ConfirmBookingCommand(ID))
                .expectEvents(new BookingConfirmedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"));

        fixture.given(
                        new BookingCreatedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"),
                        new BookingConfirmedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"))
                .when(new MarkBookingPaidCommand(ID, "pay-9"))
                .expectEvents(new BookingPaidEvent(ID, "pay-9"));
    }

    @Test
    void cannotPayABookingThatWasNeverConfirmed() {
        fixture.given(new BookingCreatedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"))
                .when(new MarkBookingPaidCommand(ID, "pay-9"))
                .expectException(IllegalStateException.class);
    }

    @Test
    void cannotCancelAPaidBooking() {
        fixture.given(
                        new BookingCreatedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"),
                        new BookingConfirmedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"),
                        new BookingPaidEvent(ID, "pay-9"))
                .when(new CancelBookingCommand(ID, "changed my mind"))
                .expectException(IllegalStateException.class);
    }

    @Test
    void confirmedBookingCanBeCancelled() {
        fixture.given(
                        new BookingCreatedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"),
                        new BookingConfirmedEvent(ID, "cust-1", "Jazz Night", 2, AMOUNT, "USD"))
                .when(new CancelBookingCommand(ID, "payment timeout"))
                .expectEvents(new BookingCancelledEvent(ID, "payment timeout"));
    }
}
