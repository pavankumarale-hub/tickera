package com.pavankumar.tickera.booking;

import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavankumar.tickera.common.events.BookingConfirmedIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

/**
 * Provider-side Pact verification for the asynchronous {@code booking-events}
 * message. The pact is authored by the <em>consumer</em> (payment-service) and
 * dropped in the shared {@code /pacts} folder; here we prove that the message
 * booking-service actually produces still satisfies that contract.
 *
 * <p>This is the message-based (not HTTP) flavour of consumer-driven contract
 * testing — the same technique applied to an Axon/Kafka event stream so the two
 * services can evolve independently without an integration environment.
 */
@Tag("contract")
@Provider("booking-service")
@PactFolder("../pacts")
@IgnoreNoPactsToVerify   // reactor may build the provider before the consumer regenerates the pact
class BookingEventsProviderPactTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new MessageTestTarget());
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    /**
     * Produces the exact payload booking-service publishes, keyed to the
     * interaction description in the consumer's pact file.
     */
    @au.com.dius.pact.provider.PactVerifyProvider("a booking confirmed event")
    public String bookingConfirmed() throws Exception {
        BookingConfirmedIntegrationEvent event = new BookingConfirmedIntegrationEvent(
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                "cust-42",
                "Symphony Gala",
                3,
                new BigDecimal("240.00"),
                "USD");
        return objectMapper.writeValueAsString(event);
    }
}
