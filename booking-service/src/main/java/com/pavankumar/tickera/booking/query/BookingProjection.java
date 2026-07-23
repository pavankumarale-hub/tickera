package com.pavankumar.tickera.booking.query;

import com.pavankumar.tickera.booking.coreapi.BookingStatus;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCancelledEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingConfirmedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingCreatedEvent;
import com.pavankumar.tickera.booking.coreapi.events.BookingEvents.BookingPaidEvent;
import com.pavankumar.tickera.booking.query.BookingQueries.FindAllBookingsQuery;
import com.pavankumar.tickera.booking.query.BookingQueries.FindBookingByIdQuery;
import com.pavankumar.tickera.booking.query.BookingQueries.FindBookingsByCustomerQuery;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Builds and serves the booking read model. Each {@code @EventHandler} upserts
 * the {@link BookingSummary} row and evicts the Redis cache entry so the next
 * read repopulates it.
 *
 * <p>Eviction is done explicitly through the {@link CacheManager} rather than with
 * {@code @CacheEvict}: Axon invokes event handlers reflectively on the target
 * bean, which bypasses Spring's caching AOP proxy — so the annotation would
 * silently do nothing here. Calling the cache API directly is unambiguous.
 *
 * <p>Running under a named processing group lets us reset the tracking token and
 * rebuild the whole projection from the event store.
 */
@Component
@ProcessingGroup("booking-projection")
public class BookingProjection {

    static final String CACHE = "bookings";

    private final BookingSummaryRepository repository;
    private final CacheManager cacheManager;

    public BookingProjection(BookingSummaryRepository repository, CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @EventHandler
    public void on(BookingCreatedEvent event, @Timestamp Instant timestamp) {
        BookingSummary summary = new BookingSummary();
        summary.setBookingId(event.bookingId());
        summary.setCustomerId(event.customerId());
        summary.setEventName(event.eventName());
        summary.setSeats(event.seats());
        summary.setAmount(event.amount());
        summary.setCurrency(event.currency());
        summary.setStatus(BookingStatus.CREATED);
        summary.setUpdatedAt(timestamp);
        repository.save(summary);
        evict(event.bookingId());
    }

    @EventHandler
    public void on(BookingConfirmedEvent event, @Timestamp Instant timestamp) {
        updateStatus(event.bookingId(), BookingStatus.CONFIRMED, timestamp);
    }

    @EventHandler
    public void on(BookingPaidEvent event, @Timestamp Instant timestamp) {
        repository.findById(event.bookingId()).ifPresent(summary -> {
            summary.setStatus(BookingStatus.PAID);
            summary.setPaymentId(event.paymentId());
            summary.setUpdatedAt(timestamp);
            repository.save(summary);
        });
        evict(event.bookingId());
    }

    @EventHandler
    public void on(BookingCancelledEvent event, @Timestamp Instant timestamp) {
        updateStatus(event.bookingId(), BookingStatus.CANCELLED, timestamp);
    }

    private void updateStatus(String bookingId, BookingStatus status, Instant timestamp) {
        repository.findById(bookingId).ifPresent(summary -> {
            summary.setStatus(status);
            summary.setUpdatedAt(timestamp);
            repository.save(summary);
        });
        evict(bookingId);
    }

    private void evict(String bookingId) {
        Cache cache = cacheManager.getCache(CACHE);
        if (cache != null) {
            cache.evict(bookingId);
        }
    }

    @QueryHandler
    public Optional<BookingSummary> handle(FindBookingByIdQuery query) {
        return repository.findById(query.bookingId());
    }

    @QueryHandler
    public List<BookingSummary> handle(FindAllBookingsQuery query) {
        return repository.findAll();
    }

    @QueryHandler
    public List<BookingSummary> handle(FindBookingsByCustomerQuery query) {
        return repository.findByCustomerId(query.customerId());
    }
}
