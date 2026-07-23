package com.pavankumar.tickera.booking.saga;

import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CancelBookingCommand;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCancelledEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingConfirmedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingPaidEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Orchestrates the booking → payment flow and owns the <em>timeout</em>, which
 * is the part a plain chain of Kafka listeners tends to get wrong.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@code BookingConfirmed} starts the saga and schedules a payment
 *       deadline. (The Kafka integration event that actually asks
 *       payment-service to charge is emitted by {@code BookingIntegrationEventPublisher}.)</li>
 *   <li>{@code BookingPaid} — happy path — cancels the deadline and ends the saga.</li>
 *   <li>If the deadline fires first, the saga issues the compensating
 *       {@code CancelBookingCommand}, releasing the held seats.</li>
 *   <li>{@code BookingCancelled} ends the saga.</li>
 * </ol>
 * See {@code docs/adr/0004-eventual-consistency.md} for why the timeout lives here.
 */
@Saga
public class BookingSaga {

    private static final Logger log = LoggerFactory.getLogger(BookingSaga.class);
    private static final String PAYMENT_DEADLINE = "payment-timeout";

    @Autowired
    private transient CommandGateway commandGateway;

    private String deadlineId;

    @StartSaga
    @SagaEventHandler(associationProperty = "bookingId")
    public void on(BookingConfirmedEvent event, DeadlineManager deadlineManager) {
        // In production this is minutes; kept short here so the demo shows the
        // compensation path without a long wait. Configurable via deadline duration.
        this.deadlineId = deadlineManager.schedule(
                java.time.Duration.ofMinutes(15), PAYMENT_DEADLINE, event.bookingId());
        log.info("Saga started for booking {} — awaiting payment (deadline {})",
                event.bookingId(), deadlineId);
    }

    @SagaEventHandler(associationProperty = "bookingId")
    @EndSaga
    public void on(BookingPaidEvent event, DeadlineManager deadlineManager) {
        cancelDeadline(deadlineManager);
        log.info("Booking {} paid (payment {}) — saga complete",
                event.bookingId(), event.paymentId());
    }

    @SagaEventHandler(associationProperty = "bookingId")
    @EndSaga
    public void on(BookingCancelledEvent event, DeadlineManager deadlineManager) {
        cancelDeadline(deadlineManager);
        log.info("Booking {} cancelled ({}) — saga ended", event.bookingId(), event.reason());
    }

    @DeadlineHandler(deadlineName = PAYMENT_DEADLINE)
    public void onPaymentTimeout(String bookingId) {
        log.warn("Payment deadline elapsed for booking {} — compensating", bookingId);
        commandGateway.send(new CancelBookingCommand(bookingId, "Payment not received in time"));
    }

    private void cancelDeadline(DeadlineManager deadlineManager) {
        if (deadlineId != null) {
            deadlineManager.cancelSchedule(PAYMENT_DEADLINE, deadlineId);
            deadlineId = null;
        }
    }
}
