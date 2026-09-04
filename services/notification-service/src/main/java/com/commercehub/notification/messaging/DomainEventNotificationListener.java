package com.commercehub.notification.messaging;

import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.OrderCancelledEvent;
import com.commercehub.messaging.event.OrderCreatedEvent;
import com.commercehub.messaging.event.PaymentFailedEvent;
import com.commercehub.messaging.event.PaymentSucceededEvent;
import com.commercehub.messaging.event.StockReleasedEvent;
import com.commercehub.messaging.event.StockReservedEvent;
import com.commercehub.notification.dto.SendNotificationRequest;
import com.commercehub.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
public class DomainEventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventNotificationListener.class);

    private final NotificationService notificationService;
    private final MessageConverter messageConverter;

    public DomainEventNotificationListener(
            NotificationService notificationService,
            MessageConverter messageConverter
    ) {
        this.notificationService = notificationService;
        this.messageConverter = messageConverter;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_NOTIFICATION_ORDER_EVENTS)
    public void onOrderEvent(Message message) {
        Object payload = messageConverter.fromMessage(message);
        switch (payload) {
            case OrderCreatedEvent event -> notificationService.send(new SendNotificationRequest(
                    event.email(),
                    "Order Created",
                    "Your order " + event.orderId() + " has been created."
            ));
            case OrderCancelledEvent event -> notificationService.send(new SendNotificationRequest(
                    "system@commercehub.local",
                    "Order Cancelled",
                    "Order " + event.orderId() + " has been cancelled."
            ));
            default -> log.warn("Ignoring unexpected order event payload: {}", payload.getClass().getName());
        }
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_NOTIFICATION_STOCK_EVENTS)
    public void onStockEvent(Message message) {
        Object payload = messageConverter.fromMessage(message);
        switch (payload) {
            case StockReservedEvent event -> notificationService.send(new SendNotificationRequest(
                    "system@commercehub.local",
                    "Stock Reserved",
                    "Stock reserved for order " + event.orderId() + "."
            ));
            case StockReleasedEvent event -> notificationService.send(new SendNotificationRequest(
                    "system@commercehub.local",
                    "Stock Released",
                    "Stock released for order " + event.orderId() + "."
            ));
            default -> log.warn("Ignoring unexpected stock event payload: {}", payload.getClass().getName());
        }
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_NOTIFICATION_PAYMENT_EVENTS)
    public void onPaymentEvent(Message message) {
        Object payload = messageConverter.fromMessage(message);
        switch (payload) {
            case PaymentSucceededEvent event -> notificationService.send(new SendNotificationRequest(
                    "system@commercehub.local",
                    "Payment Succeeded",
                    "Payment succeeded for order " + event.orderId() + "."
            ));
            case PaymentFailedEvent event -> notificationService.send(new SendNotificationRequest(
                    "system@commercehub.local",
                    "Payment Failed",
                    "Payment failed for order " + event.orderId() + ": " + event.reason()
            ));
            default -> log.warn("Ignoring unexpected payment event payload: {}", payload.getClass().getName());
        }
    }
}
