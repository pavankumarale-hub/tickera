package com.pavankumar.tickera.payment.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for the {@link PaymentSummary} read model. The custom
 * derived query lets the REST API filter payments by their originating booking.
 */
public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, String> {

    List<PaymentSummary> findByBookingId(String bookingId);
}
