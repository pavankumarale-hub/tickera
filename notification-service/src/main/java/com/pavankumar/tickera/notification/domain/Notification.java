package com.pavankumar.tickera.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persistent record of a customer-facing notification derived from an
 * integration event. In a real system these rows would drive email, SMS, or
 * push delivery; here they are exposed over REST so the event flow is
 * observable end-to-end.
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private String bookingId;

    private String channel;
    private String message;
    @Column(name = "created_at")
    private Instant createdAt;

    public Notification() {
    }

    public Notification(String bookingId, String channel, String message, Instant createdAt) {
        this.bookingId = bookingId;
        this.channel = channel;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
