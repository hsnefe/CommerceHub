package com.commercehub.inventory.messaging;

import com.commercehub.inventory.service.InventoryService;
import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.OrderCancelledEvent;
import com.commercehub.messaging.event.OrderCreatedEvent;
import com.commercehub.messaging.event.StockReleasedEvent;
import com.commercehub.messaging.event.StockReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnBean(DomainEventPublisher.class)
public class OrderInventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderInventoryEventListener.class);

    private final InventoryService inventoryService;
    private final DomainEventPublisher domainEventPublisher;

    public OrderInventoryEventListener(InventoryService inventoryService, DomainEventPublisher domainEventPublisher) {
        this.inventoryService = inventoryService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_INVENTORY_ORDER_CREATED)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            inventoryService.reserve(event.items());
            domainEventPublisher.publish(
                    MessagingTopology.ROUTING_STOCK_RESERVED,
                    new StockReservedEvent(
                            UUID.randomUUID(),
                            Instant.now(),
                            event.orderId(),
                            event.items()
                    )
            );
            log.info("Reserved stock for order {}", event.orderId());
        } catch (RuntimeException ex) {
            log.error("Failed to reserve stock for order {}: {}", event.orderId(), ex.getMessage());
        }
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_INVENTORY_ORDER_CANCELLED)
    public void onOrderCancelled(OrderCancelledEvent event) {
        try {
            inventoryService.release(event.items());
            domainEventPublisher.publish(
                    MessagingTopology.ROUTING_STOCK_RELEASED,
                    new StockReleasedEvent(
                            UUID.randomUUID(),
                            Instant.now(),
                            event.orderId(),
                            event.items()
                    )
            );
            log.info("Released stock for order {}", event.orderId());
        } catch (RuntimeException ex) {
            log.error("Failed to release stock for order {}: {}", event.orderId(), ex.getMessage());
        }
    }
}
