package com.nuvi.online_renting.dashboard.controller;

import com.nuvi.online_renting.dashboard.dto.SellerDashboardDTO;
import com.nuvi.online_renting.dashboard.service.SellerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/dashboard")
@Tag(name = "Seller Dashboard", description = "Analytics and performance summary for sellers.")
public class SellerDashboardController {

    private final SellerDashboardService dashboardService;

    public SellerDashboardController(SellerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(
            summary = "Get seller dashboard",
            description = "Returns a full analytics summary for the logged-in seller: item counts, booking stats by status, " +
                          "earnings (all-time, this month, this week), and top 5 items by completed bookings. " +
                          "Admins can query any seller by passing ?sellerId=. " +
                          "Earnings are calculated as pricePerDay × rental days for COMPLETED bookings only."
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CREATE_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<SellerDashboardDTO> getDashboard(
            @RequestParam(required = false) Long sellerId) {
        return ResponseEntity.ok(dashboardService.getDashboard(sellerId));
    }
}
