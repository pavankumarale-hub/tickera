package com.pavankumar.tickethub.notification.api;

import com.pavankumar.tickethub.notification.domain.Notification;
import com.pavankumar.tickethub.notification.domain.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Customer notifications derived from the event stream")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "List notifications, optionally filtered by booking")
    public List<Notification> list(@RequestParam(required = false) String bookingId) {
        return (bookingId == null)
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByBookingIdOrderByCreatedAtAsc(bookingId);
    }
}
