package com.pavankumar.tickera.booking.coreapi;

/**
 * Booking lifecycle. Transitions are enforced inside {@code BookingAggregate}:
 * CREATED → CONFIRMED → PAID, with CANCELLED reachable from CREATED/CONFIRMED
 * (the payment-failure compensation path driven by the saga).
 */
public enum BookingStatus {
    CREATED,
    CONFIRMED,
    PAID,
    CANCELLED
}
