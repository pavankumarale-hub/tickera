package com.pavankumar.tickera.payment.query;

import com.pavankumar.tickera.payment.coreapi.PaymentStatus;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentDeclinedEvent;
import com.pavankumar.tickera.payment.coreapi.events.PaymentEvents.PaymentProcessedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ProcessingGroup("payment-projection")
public class PaymentProjection {

    private final PaymentSummaryRepository repository;

    public PaymentProjection(PaymentSummaryRepository repository) {
        this.repository = repository;
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
    }
}
