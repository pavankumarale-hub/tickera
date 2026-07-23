package com.pavankumar.tickera.payment;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavankumar.tickera.common.events.BookingConfirmedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer-driven contract test. payment-service declares the shape of the
 * {@code BookingConfirmed} message it needs; Pact writes that expectation to the
 * shared {@code /pacts} folder, and booking-service's provider test must satisfy
 * it. If booking-service ever drops {@code amount} or renames {@code bookingId},
 * this contract breaks in CI — before it breaks in production.
 *
 * <p>{@code ProviderType.ASYNCH} is what makes this a <em>message</em> pact
 * (Kafka), not an HTTP pact — the message-based CDC pattern applied to an
 * event-driven boundary.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "booking-service", providerType = ProviderType.ASYNCH,
        pactVersion = PactSpecVersion.V3)
class BookingEventsConsumerPactTest {

    @Pact(consumer = "payment-service")
    public MessagePact bookingConfirmed(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .uuid("eventId")
                .uuid("bookingId")
                .stringType("customerId", "cust-42")
                .stringType("eventName", "Symphony Gala")
                .integerType("seats", 3)
                .decimalType("amount", 240.00)
                .stringType("currency", "USD");

        return builder
                .expectsToReceive("a booking confirmed event")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "bookingConfirmed")
    void deserialisesAndAcceptsBookingConfirmed(List<Message> messages) throws Exception {
        Message message = messages.get(0);
        BookingConfirmedIntegrationEvent event = new ObjectMapper()
                .readValue(message.contentsAsBytes(), BookingConfirmedIntegrationEvent.class);

        assertThat(event.bookingId()).isNotBlank();
        assertThat(event.amount()).isNotNull();
        assertThat(event.currency()).hasSize(3);
    }
}
