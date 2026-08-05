package com.commercehub.analytics.controller;

import com.commercehub.analytics.dto.AnalyticsEventPageResponse;
import com.commercehub.analytics.dto.AnalyticsStatsResponse;
import com.commercehub.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Domain event analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/events")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List recorded domain events")
    public ResponseEntity<AnalyticsEventPageResponse> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(analyticsService.list(page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get domain event statistics (ADMIN)")
    public ResponseEntity<AnalyticsStatsResponse> stats() {
        return ResponseEntity.ok(analyticsService.stats());
    }
}
