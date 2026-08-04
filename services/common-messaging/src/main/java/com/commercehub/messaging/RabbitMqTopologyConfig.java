package com.commercehub.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfig {

    @Bean
    public TopicExchange commercehubEventsExchange() {
        return new TopicExchange(MessagingTopology.EXCHANGE_EVENTS, true, false);
    }

    @Bean
    public Queue inventoryOrderCreatedQueue() {
        return new Queue(MessagingTopology.QUEUE_INVENTORY_ORDER_CREATED, true);
    }

    @Bean
    public Queue inventoryOrderCancelledQueue() {
        return new Queue(MessagingTopology.QUEUE_INVENTORY_ORDER_CANCELLED, true);
    }

    @Bean
    public Queue notificationOrderEventsQueue() {
        return new Queue(MessagingTopology.QUEUE_NOTIFICATION_ORDER_EVENTS, true);
    }

    @Bean
    public Queue notificationStockEventsQueue() {
        return new Queue(MessagingTopology.QUEUE_NOTIFICATION_STOCK_EVENTS, true);
    }

    @Bean
    public Binding inventoryOrderCreatedBinding(
            Queue inventoryOrderCreatedQueue,
            TopicExchange commercehubEventsExchange
    ) {
        return BindingBuilder.bind(inventoryOrderCreatedQueue)
                .to(commercehubEventsExchange)
                .with(MessagingTopology.ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding inventoryOrderCancelledBinding(
            Queue inventoryOrderCancelledQueue,
            TopicExchange commercehubEventsExchange
    ) {
        return BindingBuilder.bind(inventoryOrderCancelledQueue)
                .to(commercehubEventsExchange)
                .with(MessagingTopology.ROUTING_ORDER_CANCELLED);
    }

    @Bean
    public Binding notificationOrderEventsBinding(
            Queue notificationOrderEventsQueue,
            TopicExchange commercehubEventsExchange
    ) {
        return BindingBuilder.bind(notificationOrderEventsQueue)
                .to(commercehubEventsExchange)
                .with(MessagingTopology.BINDING_ORDER_ALL);
    }

    @Bean
    public Binding notificationStockEventsBinding(
            Queue notificationStockEventsQueue,
            TopicExchange commercehubEventsExchange
    ) {
        return BindingBuilder.bind(notificationStockEventsQueue)
                .to(commercehubEventsExchange)
                .with(MessagingTopology.BINDING_STOCK_ALL);
    }
}
