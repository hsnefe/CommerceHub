package com.commercehub.analytics.dto;

import java.util.List;

public record AnalyticsEventPageResponse(
        List<AnalyticsEventResponse> content,
        int page,
        int size,
        long totalElements
) {
}
