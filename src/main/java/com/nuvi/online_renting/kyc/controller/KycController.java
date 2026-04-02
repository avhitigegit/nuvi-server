package com.nuvi.online_renting.kyc.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.KycDocumentType;
import com.nuvi.online_renting.common.enums.KycStatus;
import com.nuvi.online_renting.kyc.dto.KycRejectRequestDTO;
import com.nuvi.online_renting.kyc.dto.KycResponseDTO;
import com.nuvi.online_renting.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "KYC", description = "Identity verification workflow. Users submit documents; admins review and approve or reject.")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @Operation(
            summary = "Submit KYC document",
            description = "Upload an identity document for KYC verification. " +
                          "Allowed types: NIC, PASSPORT, DRIVING_LICENSE. " +
                          "Allowed formats: pdf, jpg, jpeg, png. Max size: 20MB. " +
                          "Send as multipart/form-data with field 'file'."
    )
    @PostMapping("/api/v1/kyc/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KycResponseDTO> submitKyc(
            @RequestParam KycDocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kycService.submitKyc(documentType, file));
    }

    @Operation(summary = "View my KYC submissions", description = "Returns all KYC submissions made by the currently logged-in user.")
    @GetMapping("/api/v1/kyc/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KycResponseDTO>> getMySubmissions() {
        return ResponseEntity.ok(kycService.getMySubmissions());
    }

    @Operation(
            summary = "List all KYC submissions (Admin)",
            description = "Admin only. Returns all KYC submissions with optional status filter: PENDING, APPROVED, REJECTED."
    )
    @GetMapping("/api/v1/admin/kyc")
    @PreAuthorize("hasAuthority('MANAGE_KYC')")
    public ResponseEntity<PagedResponse<KycResponseDTO>> getAllSubmissions(
            @RequestParam(required = false) KycStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(kycService.getAllSubmissions(status, pageable));
    }

    @Operation(summary = "Approve KYC submission (Admin)", description = "Admin approves a PENDING KYC submission. The user's kycVerified flag is set to true automatically.")
    @PatchMapping("/api/v1/admin/kyc/{id}/approve")
    @PreAuthorize("hasAuthority('MANAGE_KYC')")
    public ResponseEntity<KycResponseDTO> approveSubmission(@PathVariable Long id) {
        return ResponseEntity.ok(kycService.approveSubmission(id));
    }

    @Operation(summary = "Reject KYC submission (Admin)", description = "Admin rejects a PENDING KYC submission with a mandatory reason. The user can resubmit after rejection.")
    @PatchMapping("/api/v1/admin/kyc/{id}/reject")
    @PreAuthorize("hasAuthority('MANAGE_KYC')")
    public ResponseEntity<KycResponseDTO> rejectSubmission(@PathVariable Long id,
                                                            @Valid @RequestBody KycRejectRequestDTO dto) {
        return ResponseEntity.ok(kycService.rejectSubmission(id, dto.getReason()));
    }
}
