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

@Tag("unit")
class NotificationListenerTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationListener listener = new NotificationListener(repository);

    @Test
    void bookingConfirmed_persistsAnEmailNotification() {
        listener.on(new BookingConfirmedIntegrationEvent(
                "evt-0", "book-1", "cust-1", "Jazz Night", 2, new BigDecimal("120.00"), "USD"));

        Notification saved = captureNotification();
        assertThat(saved.getBookingId()).isEqualTo("book-1");
        assertThat(saved.getChannel()).isEqualTo("EMAIL");
        assertThat(saved.getMessage()).contains("Jazz Night").contains("2");
    }

    @Test
    void paymentCompleted_persistsAnEmailNotification() {
        listener.on(new PaymentCompletedIntegrationEvent(
                "evt-1", "pay-1", "book-1", new BigDecimal("240.00"), "USD"));

        Notification saved = captureNotification();
        assertThat(saved.getBookingId()).isEqualTo("book-1");
        assertThat(saved.getChannel()).isEqualTo("EMAIL");
        assertThat(saved.getMessage()).contains("240.00");
    }

    @Test
    void paymentFailed_persistsAnSmsNotification() {
        listener.on(new PaymentFailedIntegrationEvent(
                "evt-2", "pay-2", "book-2", "Card declined"));

        Notification saved = captureNotification();
        assertThat(saved.getBookingId()).isEqualTo("book-2");
        assertThat(saved.getChannel()).isEqualTo("SMS");
        assertThat(saved.getMessage()).contains("Card declined");
    }

    private Notification captureNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
