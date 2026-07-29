package com.pavankumar.tickera.notification.api;

import com.pavankumar.tickera.notification.domain.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only query surface for notifications. This service owns no aggregates
 * or sagas — it is a pure Kafka consumer that persists one row per significant
 * booking/payment event and exposes them here.
 *
 * <p>Sort order differs by query type: unfiltered returns newest first
 * (dashboard view); filtered by {@code bookingId} returns oldest first
 * (timeline view showing the lifecycle of one booking in order).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Customer notifications derived from the event stream")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns all notifications when {@code bookingId} is absent (newest first),
     * or the notification history for a single booking when provided (oldest first,
     * suitable for rendering a timeline).
     */
    @GetMapping
    @Operation(summary = "List notifications, optionally filtered by booking")
    public List<NotificationResponse> list(@RequestParam(required = false) String bookingId) {
        var notifications = (bookingId == null)
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }
}
