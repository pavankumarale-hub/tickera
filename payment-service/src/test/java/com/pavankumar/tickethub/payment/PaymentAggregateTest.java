package com.pavankumar.tickethub.payment;

import com.pavankumar.tickethub.payment.aggregate.PaymentAggregate;
import com.pavankumar.tickethub.payment.coreapi.commands.ProcessPaymentCommand;
import com.pavankumar.tickethub.payment.coreapi.events.PaymentEvents.PaymentDeclinedEvent;
import com.pavankumar.tickethub.payment.coreapi.events.PaymentEvents.PaymentProcessedEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class PaymentAggregateTest {

    private FixtureConfiguration<PaymentAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
    }

    @Test
    void chargeWithinLimit_isProcessed() {
        fixture.givenNoPriorActivity()
                .when(new ProcessPaymentCommand("p-1", "b-1", "cust-1", new BigDecimal("240.00"), "USD"))
                .expectEvents(new PaymentProcessedEvent("p-1", "b-1", new BigDecimal("240.00"), "USD"));
    }

    @Test
    void chargeOverLimit_isDeclined() {
        fixture.givenNoPriorActivity()
                .when(new ProcessPaymentCommand("p-2", "b-2", "cust-1", new BigDecimal("5000.00"), "USD"))
                .expectEvents(new PaymentDeclinedEvent("p-2", "b-2", "Amount exceeds authorisation limit"));
    }

    @Test
    void nonPositiveAmount_isRejected() {
        fixture.givenNoPriorActivity()
                .when(new ProcessPaymentCommand("p-3", "b-3", "cust-1", new BigDecimal("0.00"), "USD"))
                .expectException(IllegalArgumentException.class);
    }
}
