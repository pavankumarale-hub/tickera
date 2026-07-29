package com.pavankumar.tickera.notification.api;

import com.pavankumar.tickera.notification.domain.Notification;

import java.time.Instant;

/**
 * Public API projection of a {@link Notification}. Omits the internal
 * surrogate key ({@code id}) to avoid coupling the REST contract to the
 * database schema.
 */
public record NotificationResponse(
        String bookingId,
        String channel,
        String message,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getBookingId(),
                n.getChannel(),
                n.getMessage(),
                n.getCreatedAt());
    }
}
