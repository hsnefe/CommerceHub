package com.commercehub.analytics.service;

import com.commercehub.analytics.dto.AnalyticsEventPageResponse;
import com.commercehub.analytics.dto.AnalyticsEventResponse;
import com.commercehub.analytics.dto.AnalyticsStatsResponse;
import com.commercehub.analytics.entity.AnalyticsEvent;
import com.commercehub.analytics.repository.AnalyticsEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnalyticsService {

    public static final String TYPE_ORDER_CREATED = "OrderCreated";
    public static final String TYPE_ORDER_CANCELLED = "OrderCancelled";
    public static final String TYPE_STOCK_RESERVED = "StockReserved";
    public static final String TYPE_STOCK_RELEASED = "StockReleased";
    public static final String TYPE_PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String TYPE_PAYMENT_FAILED = "PaymentFailed";

    private final AnalyticsEventRepository analyticsEventRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsEventRepository analyticsEventRepository, ObjectMapper objectMapper) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(UUID eventId, String eventType, UUID orderId, Object payload) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setOrderId(orderId);
        event.setPayload(toJson(payload));
        analyticsEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public AnalyticsEventPageResponse list(int page, int size) {
        Page<AnalyticsEvent> result = analyticsEventRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(page, size));
        return new AnalyticsEventPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsStatsResponse stats() {
        return new AnalyticsStatsResponse(
                analyticsEventRepository.countByEventType(TYPE_ORDER_CREATED),
                analyticsEventRepository.countByEventType(TYPE_ORDER_CANCELLED),
                analyticsEventRepository.countByEventType(TYPE_STOCK_RESERVED),
                analyticsEventRepository.countByEventType(TYPE_STOCK_RELEASED)
        );
    }

    private AnalyticsEventResponse toResponse(AnalyticsEvent event) {
        return new AnalyticsEventResponse(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getOrderId(),
                event.getPayload(),
                event.getReceivedAt()
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return String.valueOf(payload);
        }
    }
}
