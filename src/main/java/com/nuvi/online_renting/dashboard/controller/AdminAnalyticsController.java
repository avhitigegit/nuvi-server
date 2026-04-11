package com.nuvi.online_renting.dashboard.controller;

import com.nuvi.online_renting.dashboard.dto.AdminAnalyticsDTO;
import com.nuvi.online_renting.dashboard.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Admin Analytics", description = "Platform-wide analytics and time-series chart data for the admin dashboard.")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(
            summary = "Get admin analytics",
            description = "Returns platform-wide totals (users, sellers, items, bookings, revenue) and " +
                          "daily time-series data for the last N days (default 30, max 365). " +
                          "Use dailyBookings and dailyRevenue arrays to draw line/bar charts. " +
                          "Example: GET /api/v1/admin/analytics?days=30"
    )
    @GetMapping
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<AdminAnalyticsDTO> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getAnalytics(days));
    }
}
