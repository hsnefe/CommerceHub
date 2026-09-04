package com.commercehub.payment.messaging;

import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.StockReservedEvent;
import com.commercehub.payment.service.PaymentSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockReservedPaymentListener {

    private static final Logger log = LoggerFactory.getLogger(StockReservedPaymentListener.class);

    private final PaymentSimulationService paymentSimulationService;

    public StockReservedPaymentListener(PaymentSimulationService paymentSimulationService) {
        this.paymentSimulationService = paymentSimulationService;
    }

    @RabbitListener(queues = MessagingTopology.QUEUE_PAYMENT_STOCK_RESERVED)
    public void onStockReserved(StockReservedEvent event) {
        try {
            paymentSimulationService.processStockReserved(event);
        } catch (RuntimeException ex) {
            log.error("Failed to process payment for order {}: {}", event.orderId(), ex.getMessage());
        }
    }
}
