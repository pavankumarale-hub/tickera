-- Supports PaymentSummaryRepository.findByBookingId — used by the list
-- endpoint when filtering payments by booking.
CREATE INDEX ix_payment_summary_booking_id ON payment_summary (booking_id);
