package com.pavankumar.tickera.notification;

import com.pavankumar.tickera.common.events.BookingConfirmedIntegrationEvent;
import com.pavankumar.tickera.common.events.PaymentCompletedIntegrationEvent;
import com.pavankumar.tickera.common.events.PaymentFailedIntegrationEvent;
import com.pavankumar.tickera.notification.domain.Notification;
import com.pavankumar.tickera.notification.domain.NotificationRepository;
import com.pavankumar.tickera.notification.listener.NotificationListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link NotificationListener}. Drives each {@code @KafkaHandler}
 * directly — no Spring context, no embedded broker — and asserts that the correct
 * {@link com.pavankumar.tickera.notification.domain.Notification} is persisted for
 * each integration event type.
 */
@Tag("unit")
class NotificationListenerTest {

    private static final String BOOKING_ID     = "book-1";
    private static final String CURRENCY       = "USD";
    private static final String EMAIL          = "EMAIL";
    private static final String SMS            = "SMS";
    private static final String EVENT_NAME     = "Jazz Night";
    private static final String DECLINE_REASON = "Card declined";

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationListener listener = new NotificationListener(repository);

    @Test
    void bookingConfirmed_persistsAnEmailNotification() {
        listener.on(new BookingConfirmedIntegrationEvent(
                "evt-0", BOOKING_ID, "cust-1", EVENT_NAME, 2, new BigDecimal("120.00"), CURRENCY));

        Notification saved = captureNotification();
        assertThat(saved.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(saved.getChannel()).isEqualTo(EMAIL);
        assertThat(saved.getMessage()).contains(EVENT_NAME).contains("2");
    }

    @Test
    void paymentCompleted_persistsAnEmailNotification() {
        listener.on(new PaymentCompletedIntegrationEvent(
                "evt-1", "pay-1", BOOKING_ID, new BigDecimal("240.00"), CURRENCY));

        Notification saved = captureNotification();
        assertThat(saved.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(saved.getChannel()).isEqualTo(EMAIL);
        assertThat(saved.getMessage()).contains("240.00");
    }

    @Test
    void paymentFailed_persistsAnSmsNotification() {
        listener.on(new PaymentFailedIntegrationEvent(
                "evt-2", "pay-2", BOOKING_ID, DECLINE_REASON));

        Notification saved = captureNotification();
        assertThat(saved.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(saved.getChannel()).isEqualTo(SMS);
        assertThat(saved.getMessage()).contains(DECLINE_REASON);
    }

    private Notification captureNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
