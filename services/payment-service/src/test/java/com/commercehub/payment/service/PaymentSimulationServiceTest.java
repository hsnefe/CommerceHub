package com.commercehub.payment.service;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.OrderItemPayload;
import com.commercehub.messaging.event.PaymentFailedEvent;
import com.commercehub.messaging.event.PaymentSucceededEvent;
import com.commercehub.messaging.event.StockReservedEvent;
import com.commercehub.payment.config.PaymentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSimulationServiceTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Test
    void processStockReserved_publishesSucceededWhenSimulateFailureFalse() {
        PaymentSimulationService service = new PaymentSimulationService(
                domainEventPublisher,
                new PaymentProperties(false)
        );
        StockReservedEvent event = sampleEvent();

        service.processStockReserved(event);

        ArgumentCaptor<PaymentSucceededEvent> captor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
        verify(domainEventPublisher).publish(eq(MessagingTopology.ROUTING_PAYMENT_SUCCEEDED), captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(event.orderId());
        assertThat(captor.getValue().amount()).isEqualByComparingTo(event.totalPrice());
    }

    @Test
    void processStockReserved_publishesFailedWhenSimulateFailureTrue() {
        PaymentSimulationService service = new PaymentSimulationService(
                domainEventPublisher,
                new PaymentProperties(true)
        );
        StockReservedEvent event = sampleEvent();

        service.processStockReserved(event);

        ArgumentCaptor<PaymentFailedEvent> captor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(domainEventPublisher).publish(eq(MessagingTopology.ROUTING_PAYMENT_FAILED), captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(event.orderId());
        assertThat(captor.getValue().reason()).isEqualTo("Simulated payment failure");
    }

    private StockReservedEvent sampleEvent() {
        return new StockReservedEvent(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("99.90"),
                List.of(new OrderItemPayload(UUID.randomUUID(), 1))
        );
    }
}
