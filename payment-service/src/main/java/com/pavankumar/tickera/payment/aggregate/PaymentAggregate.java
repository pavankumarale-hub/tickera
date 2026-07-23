package com.pavankumar.tickera.payment.aggregate;

import com.pavankumar.tickera.payment.coreapi.PaymentStatus;
import com.pavankumar.tickera.payment.coreapi.commands.ProcessPaymentCommand;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentDeclinedEvent;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentProcessedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * Event-sourced payment aggregate. A payment is created once per command: the
 * constructor handler evaluates the (stubbed) authorisation rule and applies
 * either a processed or declined event. Real gateways would call out to a PSP
 * here; the shape — decision captured as an immutable event — stays the same.
 */
@Aggregate
public class PaymentAggregate {

    /** Stubbed authorisation ceiling — charges above this are "declined". */
    private static final BigDecimal AUTH_LIMIT = new BigDecimal("1000.00");

    @AggregateIdentifier
    private String paymentId;
    private PaymentStatus status;

    protected PaymentAggregate() {
    }

    @CommandHandler
    public PaymentAggregate(ProcessPaymentCommand cmd) {
        if (cmd.amount() == null || cmd.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (cmd.amount().compareTo(AUTH_LIMIT) > 0) {
            apply(new PaymentDeclinedEvent(cmd.paymentId(), cmd.bookingId(),
                    "Amount exceeds authorisation limit"));
        } else {
            apply(new PaymentProcessedEvent(cmd.paymentId(), cmd.bookingId(),
                    cmd.amount(), cmd.currency()));
        }
    }

    @EventSourcingHandler
    public void on(PaymentProcessedEvent event) {
        this.paymentId = event.paymentId();
        this.status = PaymentStatus.COMPLETED;
    }

    @EventSourcingHandler
    public void on(PaymentDeclinedEvent event) {
        this.paymentId = event.paymentId();
        this.status = PaymentStatus.DECLINED;
    }
}
