package com.commercehub.payment.service;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.PaymentFailedEvent;
import com.commercehub.messaging.event.PaymentSucceededEvent;
import com.commercehub.messaging.event.StockReservedEvent;
import com.commercehub.payment.config.PaymentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentSimulationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSimulationService.class);

    private final DomainEventPublisher domainEventPublisher;
    private final PaymentProperties paymentProperties;

    public PaymentSimulationService(
            DomainEventPublisher domainEventPublisher,
            PaymentProperties paymentProperties) {
        this.domainEventPublisher = domainEventPublisher;
        this.paymentProperties = paymentProperties;
    }

    public void processStockReserved(StockReservedEvent event) {
        if (paymentProperties.simulateFailure()) {
            domainEventPublisher.publish(
                    MessagingTopology.ROUTING_PAYMENT_FAILED,
                    new PaymentFailedEvent(
                            UUID.randomUUID(),
                            Instant.now(),
                            event.orderId(),
                            event.userId(),
                            event.totalPrice(),
                            "Simulated payment failure"
                    )
            );
            log.info("Simulated payment failure for order {}", event.orderId());
            return;
        }

        domainEventPublisher.publish(
                MessagingTopology.ROUTING_PAYMENT_SUCCEEDED,
                new PaymentSucceededEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        event.orderId(),
                        event.userId(),
                        event.totalPrice()
                )
        );
        log.info("Simulated payment success for order {}", event.orderId());
    }
}
