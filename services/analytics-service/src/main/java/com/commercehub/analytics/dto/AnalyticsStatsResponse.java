package com.commercehub.analytics.dto;

public record AnalyticsStatsResponse(
        long ordersCreated,
        long ordersCancelled,
        long stockReserved,
        long stockReleased
) {
}
