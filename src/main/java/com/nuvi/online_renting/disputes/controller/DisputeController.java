package com.nuvi.online_renting.disputes.controller;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.DisputeStatus;
import com.nuvi.online_renting.disputes.dto.DisputeRequestDTO;
import com.nuvi.online_renting.disputes.dto.DisputeResolveDTO;
import com.nuvi.online_renting.disputes.dto.DisputeResponseDTO;
import com.nuvi.online_renting.disputes.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Raise and manage disputes for rental bookings. Renters raise disputes; admins review and resolve them.")
public class DisputeController {

    private final DisputeService disputeService;

    @Operation(
            summary = "Raise a dispute",
            description = "A renter raises a dispute against one of their CONFIRMED or COMPLETED bookings. " +
                          "Only one dispute is allowed per booking. " +
                          "Provide a reason (ITEM_NOT_RETURNED, ITEM_DAMAGED, WRONG_ITEM_DELIVERED, ITEM_NOT_AS_DESCRIBED, NO_SHOW, OTHER) " +
                          "and a detailed description."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('RAISE_DISPUTE')")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> raiseDispute(@Valid @RequestBody DisputeRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dispute raised successfully", disputeService.raiseDispute(dto)));
    }

    @Operation(summary = "Get dispute by ID", description = "Retrieve details of a single dispute. Renters can view their own disputes; admins can view all.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_DISPUTES', 'MANAGE_DISPUTES')")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> getDisputeById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dispute fetched", disputeService.getDisputeById(id)));
    }

    @Operation(
            summary = "List disputes",
            description = "Returns a paginated list of disputes. " +
                          "Admins see all disputes; regular users see only their own. " +
                          "Filter by status: OPEN, UNDER_REVIEW, RESOLVED, REJECTED. " +
                          "Example: GET /api/disputes?status=OPEN&page=0&size=10"
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_DISPUTES', 'MANAGE_DISPUTES')")
    public ResponseEntity<ApiResponse<PagedResponse<DisputeResponseDTO>>> getAllDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Disputes fetched", disputeService.getAllDisputes(status, pageable)));
    }

    @Operation(summary = "Mark dispute as Under Review (Admin)", description = "Admin acknowledges the dispute and begins reviewing it. Transitions status from OPEN to UNDER_REVIEW.")
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAuthority('MANAGE_DISPUTES')")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> markUnderReview(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dispute is now under review", disputeService.markUnderReview(id)));
    }

    @Operation(summary = "Resolve a dispute (Admin)", description = "Admin resolves the dispute in the renter's favour. Requires a resolution note explaining the outcome.")
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('MANAGE_DISPUTES')")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> resolveDispute(@PathVariable Long id,
                                                                           @Valid @RequestBody DisputeResolveDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dispute resolved", disputeService.resolveDispute(id, dto)));
    }

    @Operation(summary = "Reject a dispute (Admin)", description = "Admin rejects the dispute. Requires a note explaining the reason for rejection.")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MANAGE_DISPUTES')")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> rejectDispute(@PathVariable Long id,
                                                                          @Valid @RequestBody DisputeResolveDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dispute rejected", disputeService.rejectDispute(id, dto)));
    }
}
