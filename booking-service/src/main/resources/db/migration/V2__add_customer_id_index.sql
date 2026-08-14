-- Supports BookingSummaryRepository.findByCustomerId — used by the list
-- endpoint when filtering bookings by customer.
CREATE INDEX ix_booking_summary_customer_id ON booking_summary (customer_id);
