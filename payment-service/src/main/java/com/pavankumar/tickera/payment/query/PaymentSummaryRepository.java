package com.pavankumar.tickera.payment.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, String> {

    List<PaymentSummary> findByBookingId(String bookingId);

    Optional<PaymentSummary> findFirstByBookingId(String bookingId);
}
