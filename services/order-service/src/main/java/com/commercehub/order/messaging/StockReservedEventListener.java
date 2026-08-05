package com.commercehub.order.messaging;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.StockReservedEvent;
import com.commercehub.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(DomainEventPublisher.class)
public class StockReservedEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockReservedEventListener.class);

    private final OrderService orderService;

    public StockReservedEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_ORDER_STOCK_RESERVED)
    public void onStockReserved(StockReservedEvent event) {
        try {
            orderService.markStockReserved(event.orderId());
            log.info("Marked order {} as STOCK_RESERVED", event.orderId());
        } catch (RuntimeException ex) {
            log.error("Failed to mark order {} as STOCK_RESERVED: {}", event.orderId(), ex.getMessage());
        }
    }
}
