package com.pavankumar.tickera.payment.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, String> {

    List<PaymentSummary> findByBookingId(String bookingId);
}
