package com.nuvi.online_renting.sellers.controller;

import com.nuvi.online_renting.common.audit.Auditable;
import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.enums.SellerStatus;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.dto.SellerDecisionDTO;
import com.nuvi.online_renting.sellers.dto.SellerSuspensionDTO;
import com.nuvi.online_renting.sellers.service.SellerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_SELLER_APPLICATIONS')")
@Tag(name = "Admin — Seller Applications", description = "Admin-only endpoints to review pending seller applications and approve or reject them. Approving an application upgrades the user's role to SELLER.")
public class AdminSellerController {

    private final SellerApplicationService applicationService;

    @Operation(
            summary = "List seller applications (Admin)",
            description = "Returns all seller applications, optionally filtered by status (PENDING, APPROVED, REJECTED). " +
                          "Example: GET /api/v1/admin/sellers/applications?status=PENDING"
    )
    @GetMapping("/applications")
    public ResponseEntity<List<SellerApplicationResponseDTO>> list(
            @RequestParam(required = false) SellerStatus status) {
        return ResponseEntity.ok(applicationService.getByStatus(status == null ? null : status.name()));
    }

    @Operation(
            summary = "Approve or reject a seller application (Admin)",
            description = "Admin reviews a seller application and sets the decision to APPROVED or REJECTED, with an optional note. " +
                          "When APPROVED, the applicant's role is automatically upgraded from USER to SELLER, " +
                          "granting them access to create and manage item listings."
    )
    @Auditable(action = "SELLER_APPLICATION_DECIDED", resourceType = "SellerApplication")
    @PatchMapping("/applications/{id}")
    public ResponseEntity<SellerApplicationResponseDTO> decide(
            @PathVariable Long id,
            @Valid @RequestBody SellerDecisionDTO decision) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(applicationService.decide(id, decision, adminEmail));
    }

    @Operation(
            summary = "Suspend a seller (Admin)",
            description = "Suspends an approved seller by user ID. A suspended seller cannot create or update items " +
                          "and their items cannot be booked. The seller retains login access and can view their own data."
    )
    @Auditable(action = "SELLER_SUSPENDED", resourceType = "User")
    @PreAuthorize("hasAuthority('SUSPEND_SELLER')")
    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(
            @PathVariable Long userId,
            @Valid @RequestBody SellerSuspensionDTO dto) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        applicationService.suspendSeller(userId, dto, adminEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Seller suspended successfully", null));
    }

    @Operation(
            summary = "Unsuspend a seller (Admin)",
            description = "Lifts the suspension on a seller, restoring full seller access."
    )
    @Auditable(action = "SELLER_UNSUSPENDED", resourceType = "User")
    @PreAuthorize("hasAuthority('SUSPEND_SELLER')")
    @PatchMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspend(@PathVariable Long userId) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        applicationService.unsuspendSeller(userId, adminEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Seller unsuspended successfully", null));
    }
}
