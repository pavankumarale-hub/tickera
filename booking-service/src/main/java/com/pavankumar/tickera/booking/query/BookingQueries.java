package com.pavankumar.tickera.booking.query;

/**
 * Query messages handled by {@link BookingProjection}. Using explicit query
 * objects (rather than calling the repository directly from the controller)
 * keeps the read side behind Axon's {@code QueryGateway}, which is what makes
 * subscription queries / future read-model relocation possible without touching
 * callers.
 */
public final class BookingQueries {

    private BookingQueries() {
    }

    public record FindBookingByIdQuery(String bookingId) {
    }

    public record FindAllBookingsQuery() {
    }

    public record FindBookingsByCustomerQuery(String customerId) {
    }
}
