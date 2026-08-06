package com.commercehub.order.messaging;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.PaymentFailedEvent;
import com.commercehub.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(DomainEventPublisher.class)
public class PaymentFailedEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedEventListener.class);

    private final OrderService orderService;

    public PaymentFailedEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_ORDER_PAYMENT_FAILED)
    public void onPaymentFailed(PaymentFailedEvent event) {
        try {
            orderService.cancelDueToPaymentFailure(event.orderId());
            log.info("Cancelled order {} after payment failure: {}", event.orderId(), event.reason());
        } catch (RuntimeException ex) {
            log.error("Failed to cancel order {} after payment failure: {}", event.orderId(), ex.getMessage());
        }
    }
}
