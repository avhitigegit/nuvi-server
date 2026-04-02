package com.nuvi.online_renting.coupons.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.coupons.dto.CouponRequestDTO;
import com.nuvi.online_renting.coupons.dto.CouponResponseDTO;
import com.nuvi.online_renting.coupons.dto.ValidateCouponRequestDTO;
import com.nuvi.online_renting.coupons.dto.ValidateCouponResponseDTO;
import com.nuvi.online_renting.coupons.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Coupons", description = "Admin manages promotional coupon codes; renters validate and apply them at booking.")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "Create a coupon (admin)")
    @PostMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<CouponResponseDTO> createCoupon(@Valid @RequestBody CouponRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @Operation(summary = "List all coupons (admin)")
    @GetMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<PagedResponse<CouponResponseDTO>> getAllCoupons(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(couponService.getAllCoupons(pageable));
    }

    @Operation(summary = "Get coupon by ID (admin)")
    @GetMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<CouponResponseDTO> getCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.getCoupon(id));
    }

    @Operation(summary = "Deactivate a coupon (admin)")
    @PatchMapping("/api/v1/admin/coupons/{id}/deactivate")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<CouponResponseDTO> deactivateCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.deactivateCoupon(id));
    }

    @Operation(
            summary = "Validate a coupon code",
            description = "Preview the discount for a given booking amount before creating the booking. Does NOT consume the coupon."
    )
    @PostMapping("/api/v1/coupons/validate")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<ValidateCouponResponseDTO> validateCoupon(@Valid @RequestBody ValidateCouponRequestDTO request) {
        return ResponseEntity.ok(couponService.validateCoupon(request));
    }
}
