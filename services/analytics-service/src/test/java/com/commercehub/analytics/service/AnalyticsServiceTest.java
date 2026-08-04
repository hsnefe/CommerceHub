package com.commercehub.analytics.service;

import com.commercehub.analytics.dto.AnalyticsStatsResponse;
import com.commercehub.analytics.entity.AnalyticsEvent;
import com.commercehub.analytics.repository.AnalyticsEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void record_persistsEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":\"" + orderId + "\"}");
        when(analyticsEventRepository.save(any(AnalyticsEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        analyticsService.record(eventId, AnalyticsService.TYPE_ORDER_CREATED, orderId, java.util.Map.of("x", 1));

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getEventType()).isEqualTo(AnalyticsService.TYPE_ORDER_CREATED);
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void stats_aggregatesCounts() {
        when(analyticsEventRepository.countByEventType(AnalyticsService.TYPE_ORDER_CREATED)).thenReturn(3L);
        when(analyticsEventRepository.countByEventType(AnalyticsService.TYPE_ORDER_CANCELLED)).thenReturn(1L);
        when(analyticsEventRepository.countByEventType(AnalyticsService.TYPE_STOCK_RESERVED)).thenReturn(2L);
        when(analyticsEventRepository.countByEventType(AnalyticsService.TYPE_STOCK_RELEASED)).thenReturn(1L);

        AnalyticsStatsResponse stats = analyticsService.stats();

        assertThat(stats.ordersCreated()).isEqualTo(3);
        assertThat(stats.ordersCancelled()).isEqualTo(1);
        assertThat(stats.stockReserved()).isEqualTo(2);
        assertThat(stats.stockReleased()).isEqualTo(1);
    }
}
