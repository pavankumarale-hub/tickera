package com.pavankumar.tickera.notification.listener;

import com.pavankumar.tickera.common.events.BookingConfirmedIntegrationEvent;
import com.pavankumar.tickera.common.events.KafkaTopics;
import com.pavankumar.tickera.common.events.PaymentCompletedIntegrationEvent;
import com.pavankumar.tickera.common.events.PaymentFailedIntegrationEvent;
import com.pavankumar.tickera.notification.domain.Notification;
import com.pavankumar.tickera.notification.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Fan-in consumer: subscribes to both booking and payment topics and records a
 * customer-facing notification for each interesting event. Deliberately a plain
 * Spring Kafka listener (not Axon) to show a lightweight downstream service
 * consuming the same integration contracts the CQRS services publish.
 *
 * <p>In a real system these rows would drive email/SMS/push; here they are
 * persisted and exposed over REST so the event flow is observable end-to-end.
 */
@Component
@KafkaListener(topics = {KafkaTopics.BOOKING_EVENTS, KafkaTopics.PAYMENT_EVENTS},
        groupId = "notification-service")
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationRepository repository;

    public NotificationListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @KafkaHandler
    public void on(BookingConfirmedIntegrationEvent event) {
        save(event.bookingId(), "EMAIL", String.format(
                "Your booking for '%s' (%d seat(s)) is confirmed — completing payment.",
                event.eventName(), event.seats()));
    }

    @KafkaHandler
    public void on(PaymentCompletedIntegrationEvent event) {
        save(event.bookingId(), "EMAIL", String.format(
                "Payment of %s %s received — your tickets are secured!",
                event.amount(), event.currency()));
    }

    @KafkaHandler
    public void on(PaymentFailedIntegrationEvent event) {
        save(event.bookingId(), "SMS", String.format(
                "Payment failed (%s). Your booking will be released.", event.reason()));
    }

    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.debug("Ignoring unrecognised event: {}", payload.getClass());
    }

    private void save(String bookingId, String channel, String message) {
        repository.save(new Notification(bookingId, channel, message, Instant.now()));
        log.info("[{}] booking {}: {}", channel, bookingId, message);
    }
}
