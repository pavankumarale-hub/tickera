package com.pavankumar.tickera.booking.query;

import com.pavankumar.tickera.booking.query.BookingQueries.FindAllBookingsQuery;
import com.pavankumar.tickera.booking.query.BookingQueries.FindBookingByIdQuery;
import com.pavankumar.tickera.booking.query.BookingQueries.FindBookingsByCustomerQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Read-side facade. {@code findById} is annotated {@link Cacheable} against the
 * Redis-backed {@code bookings} cache: hot bookings (e.g. a client polling for
 * payment status) are served from Redis, and {@link BookingProjection} evicts
 * the entry whenever an event changes the row. This is the same read-through
 * cache + event-driven invalidation pattern used for high-read lookups.
 */
@Service
public class BookingQueryService {

    private final QueryGateway queryGateway;

    public BookingQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Cacheable(cacheNames = "bookings", key = "#bookingId", unless = "#result == null")
    public BookingSummary findById(String bookingId) {
        return queryGateway.query(
                new FindBookingByIdQuery(bookingId),
                ResponseTypes.optionalInstanceOf(BookingSummary.class))
                .join()
                .orElse(null);
    }

    public List<BookingSummary> findAll() {
        return queryGateway.query(
                new FindAllBookingsQuery(),
                ResponseTypes.multipleInstancesOf(BookingSummary.class))
                .join();
    }

    public List<BookingSummary> findByCustomer(String customerId) {
        return queryGateway.query(
                new FindBookingsByCustomerQuery(customerId),
                ResponseTypes.multipleInstancesOf(BookingSummary.class))
                .join();
    }
}
