package com.pavankumar.tickethub.common.events;

/**
 * Single source of truth for the Kafka topic names that bridge bounded contexts.
 *
 * <p>Domain (Axon) events stay inside a service; only the coarse-grained
 * integration events below cross the wire. Keeping the names here (rather than
 * in scattered {@code @KafkaListener} annotations) is what lets the Pact
 * message contracts and the services agree on a single vocabulary.
 */
public final class KafkaTopics {

    /** Booking lifecycle facts published by {@code booking-service}. */
    public static final String BOOKING_EVENTS = "tickethub.booking-events";

    /** Payment outcomes published by {@code payment-service}. */
    public static final String PAYMENT_EVENTS = "tickethub.payment-events";

    private KafkaTopics() {
    }
}
