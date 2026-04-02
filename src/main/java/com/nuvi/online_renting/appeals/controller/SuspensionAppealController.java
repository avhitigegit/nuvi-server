package com.nuvi.online_renting.appeals.controller;

import com.nuvi.online_renting.appeals.dto.AppealRejectDTO;
import com.nuvi.online_renting.appeals.dto.AppealRequestDTO;
import com.nuvi.online_renting.appeals.dto.AppealResponseDTO;
import com.nuvi.online_renting.appeals.service.SuspensionAppealService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.AppealStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Suspension Appeals", description = "Suspended sellers can appeal their suspension; admins review appeals.")
public class SuspensionAppealController {

    private final SuspensionAppealService appealService;

    public SuspensionAppealController(SuspensionAppealService appealService) {
        this.appealService = appealService;
    }

    @Operation(summary = "Submit a suspension appeal", description = "Only callable by a currently suspended seller. One PENDING appeal at a time is allowed.")
    @PostMapping("/api/v1/appeals")
    @PreAuthorize("hasAnyAuthority('SUBMIT_APPEAL', 'FULL_ACCESS')")
    public ResponseEntity<AppealResponseDTO> submitAppeal(@Valid @RequestBody AppealRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appealService.submitAppeal(request));
    }

    @Operation(summary = "Get my appeal history")
    @GetMapping("/api/v1/appeals/me")
    @PreAuthorize("hasAnyAuthority('SUBMIT_APPEAL', 'FULL_ACCESS')")
    public ResponseEntity<List<AppealResponseDTO>> getMyAppeals() {
        return ResponseEntity.ok(appealService.getMyAppeals());
    }

    @Operation(summary = "List all appeals (admin)", description = "Optionally filter by status: PENDING, APPROVED, REJECTED.")
    @GetMapping("/api/v1/admin/appeals")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<PagedResponse<AppealResponseDTO>> getAllAppeals(
            @RequestParam(required = false) AppealStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(appealService.getAllAppeals(status, pageable));
    }

    @Operation(summary = "Approve an appeal (admin)", description = "Approves the appeal and unsuspends the seller.")
    @PatchMapping("/api/v1/admin/appeals/{id}/approve")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<AppealResponseDTO> approveAppeal(@PathVariable Long id) {
        return ResponseEntity.ok(appealService.approveAppeal(id));
    }

    @Operation(summary = "Reject an appeal (admin)", description = "Rejects the appeal with an explanatory comment. The seller remains suspended.")
    @PatchMapping("/api/v1/admin/appeals/{id}/reject")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<AppealResponseDTO> rejectAppeal(@PathVariable Long id,
                                                          @Valid @RequestBody AppealRejectDTO request) {
        return ResponseEntity.ok(appealService.rejectAppeal(id, request));
    }
}
