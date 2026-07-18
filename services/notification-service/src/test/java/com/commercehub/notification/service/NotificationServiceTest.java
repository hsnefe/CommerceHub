package com.commercehub.notification.service;

import com.commercehub.notification.dto.SendNotificationRequest;
import com.commercehub.notification.dto.SendNotificationResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    private SendNotificationRequest sampleRequest() {
        return new SendNotificationRequest(
                "user@example.com",
                "Order Created",
                "Your order has been created."
        );
    }

    @Nested
    class Send {

        @Test
        void send_returnsSuccess() {
            SendNotificationResponse response = notificationService.send(sampleRequest());

            assertThat(response.success()).isTrue();
        }

        @Test
        void send_withDifferentRecipient_returnsSuccess() {
            SendNotificationResponse response = notificationService.send(
                    new SendNotificationRequest(
                            "admin@commercehub.test",
                            "Order Created",
                            "Your order has been created."
                    )
            );

            assertThat(response.success()).isTrue();
        }

        @Test
        void send_withLongSubjectAndMessage_returnsSuccess() {
            String longSubject = "Order Created - ".repeat(10);
            String longMessage = "Your order has been created with many items. ".repeat(5);

            SendNotificationResponse response = notificationService.send(
                    new SendNotificationRequest("user@example.com", longSubject, longMessage)
            );

            assertThat(response.success()).isTrue();
        }

        @Test
        void send_withSpecialCharactersInMessage_returnsSuccess() {
            SendNotificationResponse response = notificationService.send(
                    new SendNotificationRequest(
                            "user@example.com",
                            "Sipariş Oluşturuldu",
                            "Siparişiniz oluşturuldu: ürün #123 — toplam ₺1.999,99"
                    )
            );

            assertThat(response.success()).isTrue();
        }

        @Test
        void send_withUppercaseEmail_returnsSuccess() {
            SendNotificationResponse response = notificationService.send(
                    new SendNotificationRequest(
                            "USER@EXAMPLE.COM",
                            "Order Created",
                            "Your order has been created."
                    )
            );

            assertThat(response.success()).isTrue();
        }

        @Test
        void send_alwaysReturnsTrueRegardlessOfContent() {
            SendNotificationResponse first = notificationService.send(
                    new SendNotificationRequest("a@b.co", "Subject A", "Message A")
            );
            SendNotificationResponse second = notificationService.send(
                    new SendNotificationRequest("z@y.co", "Subject Z", "Message Z")
            );

            assertThat(first.success()).isTrue();
            assertThat(second.success()).isTrue();
        }
    }
}
