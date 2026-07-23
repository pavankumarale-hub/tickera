package com.pavankumar.tickera.booking.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSummaryRepository extends JpaRepository<BookingSummary, String> {

    List<BookingSummary> findByCustomerId(String customerId);
}
