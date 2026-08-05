package com.commercehub.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(MessagingTopology.EXCHANGE_EVENTS, routingKey, event);
    }
}
