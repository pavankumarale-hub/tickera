package com.pavankumar.tickera.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBookingIdOrderByCreatedAtAsc(String bookingId);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
