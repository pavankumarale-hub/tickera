package com.pavankumar.tickera.booking.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for the {@link BookingSummary} read model. The custom
 * derived query supports filtering bookings by customer for the list endpoint.
 */
public interface BookingSummaryRepository extends JpaRepository<BookingSummary, String> {

    List<BookingSummary> findByCustomerId(String customerId);
}
