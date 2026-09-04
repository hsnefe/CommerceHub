package com.commercehub.payment.messaging;

import com.commercehub.messaging.MessagingTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the queues this service consumes. A listener bean that drops out of the
 * context leaves its queue without a consumer, which kills the asynchronous flow
 * while every other test still passes.
 *
 * <p>The test profile leaves the listener containers registered but stopped
 * ({@code auto-startup: false}), so no broker is needed to assert the wiring.
 */
@SpringBootTest
@ActiveProfiles("test")
class ListenerRegistrationTest {

    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Test
    void registersAListenerForEveryQueueItConsumes() {
        Set<String> queues = rabbitListenerEndpointRegistry.getListenerContainers().stream()
                .filter(AbstractMessageListenerContainer.class::isInstance)
                .map(AbstractMessageListenerContainer.class::cast)
                .flatMap(container -> Arrays.stream(container.getQueueNames()))
                .collect(Collectors.toSet());

        assertThat(queues).containsExactlyInAnyOrder(MessagingTopology.QUEUE_PAYMENT_STOCK_RESERVED);
    }
}
