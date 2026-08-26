package com.pavankumar.tickera.payment.query;

import com.pavankumar.tickera.payment.coreapi.PaymentStatus;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentDeclinedEvent;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentProcessedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Builds the payment read model from the event stream. Each {@code @EventHandler}
 * persists a {@link PaymentSummary} row so the query side can serve payment
 * outcomes without touching the aggregate store.
 *
 * <p>Running under a named processing group lets us reset the tracking token and
 * rebuild the entire read model from the event store at any time.
 */
@Component
@ProcessingGroup("payment-projection")
public class PaymentProjection {

    private final PaymentSummaryRepository repository;
    private final Counter completedCounter;
    private final Counter declinedCounter;

    public PaymentProjection(PaymentSummaryRepository repository, MeterRegistry registry) {
        this.repository = repository;
        this.completedCounter = transitionCounter(registry, "COMPLETED");
        this.declinedCounter  = transitionCounter(registry, "DECLINED");
    }

    private static Counter transitionCounter(MeterRegistry registry, String toStatus) {
        return Counter.builder("tickera.payment.transitions")
                .tag("to", toStatus)
                .description("Cumulative payment state-machine transitions by outcome")
                .register(registry);
    }

    @EventHandler
    public void on(PaymentProcessedEvent event, @Timestamp Instant timestamp) {
        PaymentSummary summary = new PaymentSummary();
        summary.setPaymentId(event.paymentId());
        summary.setBookingId(event.bookingId());
        summary.setAmount(event.amount());
        summary.setCurrency(event.currency());
        summary.setStatus(PaymentStatus.COMPLETED);
        summary.setCreatedAt(timestamp);
        repository.save(summary);
        completedCounter.increment();
    }

    @EventHandler
    public void on(PaymentDeclinedEvent event, @Timestamp Instant timestamp) {
        PaymentSummary summary = new PaymentSummary();
        summary.setPaymentId(event.paymentId());
        summary.setBookingId(event.bookingId());
        summary.setStatus(PaymentStatus.DECLINED);
        summary.setReason(event.reason());
        summary.setCreatedAt(timestamp);
        repository.save(summary);
        declinedCounter.increment();
    }
}
