package com.commercehub.order.messaging;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.PaymentSucceededEvent;
import com.commercehub.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(DomainEventPublisher.class)
public class PaymentSucceededEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededEventListener.class);

    private final OrderService orderService;

    public PaymentSucceededEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_ORDER_PAYMENT_SUCCEEDED)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        try {
            orderService.markPaid(event.orderId());
            log.info("Marked order {} as PAID after payment success", event.orderId());
        } catch (RuntimeException ex) {
            log.error("Failed to mark order {} as PAID: {}", event.orderId(), ex.getMessage());
        }
    }
}
