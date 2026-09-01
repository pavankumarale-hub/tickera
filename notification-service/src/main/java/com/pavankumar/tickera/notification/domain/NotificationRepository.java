package com.pavankumar.tickera.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link Notification} rows. The two derived queries
 * enforce intentionally different sort orders: booking-scoped lookups return
 * oldest-first (chronological timeline), while the unfiltered list returns
 * newest-first (dashboard view).
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBookingIdOrderByCreatedAtAsc(String bookingId);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
