package com.nuvi.online_renting.sellers.controller;

import com.nuvi.online_renting.common.enums.SellerStatus;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.dto.SellerDecisionDTO;
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
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_SELLER_APPLICATIONS')")
@Tag(name = "Admin — Seller Applications", description = "Admin-only endpoints to review pending seller applications and approve or reject them. Approving an application upgrades the user's role to SELLER.")
public class AdminSellerController {

    private final SellerApplicationService applicationService;

    @Operation(
            summary = "List seller applications (Admin)",
            description = "Returns all seller applications, optionally filtered by status (PENDING, APPROVED, REJECTED). " +
                          "Example: GET /api/admin/sellers/applications?status=PENDING"
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
    @PatchMapping("/applications/{id}")
    public ResponseEntity<SellerApplicationResponseDTO> decide(
            @PathVariable Long id,
            @Valid @RequestBody SellerDecisionDTO decision) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(applicationService.decide(id, decision, adminEmail));
    }
}
