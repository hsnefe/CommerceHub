package com.commercehub.analytics.messaging;

import com.commercehub.analytics.service.AnalyticsService;
import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.OrderCancelledEvent;
import com.commercehub.messaging.event.OrderCreatedEvent;
import com.commercehub.messaging.event.StockReleasedEvent;
import com.commercehub.messaging.event.StockReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(DomainEventPublisher.class)
public class DomainEventAnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventAnalyticsListener.class);

    private final AnalyticsService analyticsService;
    private final MessageConverter messageConverter;

    public DomainEventAnalyticsListener(AnalyticsService analyticsService, MessageConverter messageConverter) {
        this.analyticsService = analyticsService;
        this.messageConverter = messageConverter;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_ANALYTICS_EVENTS)
    public void onDomainEvent(Message message) {
        Object payload = messageConverter.fromMessage(message);
        switch (payload) {
            case OrderCreatedEvent event -> analyticsService.record(
                    event.eventId(),
                    AnalyticsService.TYPE_ORDER_CREATED,
                    event.orderId(),
                    event
            );
            case OrderCancelledEvent event -> analyticsService.record(
                    event.eventId(),
                    AnalyticsService.TYPE_ORDER_CANCELLED,
                    event.orderId(),
                    event
            );
            case StockReservedEvent event -> analyticsService.record(
                    event.eventId(),
                    AnalyticsService.TYPE_STOCK_RESERVED,
                    event.orderId(),
                    event
            );
            case StockReleasedEvent event -> analyticsService.record(
                    event.eventId(),
                    AnalyticsService.TYPE_STOCK_RELEASED,
                    event.orderId(),
                    event
            );
            default -> log.warn("Ignoring unexpected analytics payload: {}", payload.getClass().getName());
        }
    }
}
