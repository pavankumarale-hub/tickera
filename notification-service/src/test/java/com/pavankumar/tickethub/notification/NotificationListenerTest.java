package com.pavankumar.tickethub.notification;

import com.pavankumar.tickethub.common.events.PaymentCompletedIntegrationEvent;
import com.pavankumar.tickethub.notification.domain.Notification;
import com.pavankumar.tickethub.notification.domain.NotificationRepository;
import com.pavankumar.tickethub.notification.listener.NotificationListener;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationListenerTest {

    @Test
    void paymentCompleted_persistsAnEmailNotification() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationListener listener = new NotificationListener(repository);

        listener.on(new PaymentCompletedIntegrationEvent(
                "evt-1", "pay-1", "book-1", new BigDecimal("240.00"), "USD"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getBookingId()).isEqualTo("book-1");
        assertThat(saved.getChannel()).isEqualTo("EMAIL");
        assertThat(saved.getMessage()).contains("240.00");
    }
}
