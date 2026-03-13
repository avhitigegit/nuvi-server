package com.nuvi.online_renting.sellers.controller;

import com.nuvi.online_renting.common.storage.S3StorageService;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.validation.FileValidator;
import com.nuvi.online_renting.sellers.dto.SellerApplicationRequestDTO;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.service.SellerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller Applications", description = "Users apply to become sellers on the platform. After applying, they can upload supporting documents. An admin reviews and approves or rejects the application.")
public class SellerController {

    private final SellerApplicationService applicationService;
    private final S3StorageService s3StorageService;
    private final AuthenticationFacade authFacade;
    private final FileValidator fileValidator;

    @Operation(
            summary = "Apply to become a seller",
            description = "A logged-in USER submits an application to become a seller on the platform. " +
                          "Provide your business name and a short description. " +
                          "After applying, upload supporting documents via POST /api/v1/sellers/apply/{id}/docs. " +
                          "An ADMIN will review and approve or reject the application."
    )
    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('APPLY_SELLER')")
    public ResponseEntity<SellerApplicationResponseDTO> apply(
            @Valid @RequestBody SellerApplicationRequestDTO sellerApplicationRequestDTO) {
        Long userId = authFacade.getCurrentUser().getId();
        SellerApplicationResponseDTO result = applicationService.apply(userId, sellerApplicationRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(
            summary = "Upload supporting documents for a seller application",
            description = "Upload one or more documents (e.g. ID proof, business registration) to support a seller application. " +
                          "Send as multipart/form-data with field name 'docs'. Maximum file size per file is 50MB. " +
                          "Use the application ID returned from POST /api/v1/sellers/apply as the path variable."
    )
    @PostMapping(value = "/apply/{id}/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('UPLOAD_SELLER_DOCS')")
    public ResponseEntity<String> uploadDocs(
            @PathVariable Long id,
            @RequestPart("docs") List<MultipartFile> docs) {
        // Centralised MIME validation — extension whitelist + magic-byte content check
        // Allowed: pdf, jpg, jpeg, png — max 20 MB each
        fileValidator.validateDocuments(docs);

        // Upload to S3 private prefix — returns keys, not public URLs
        List<String> keys = s3StorageService.uploadDocs(id, docs);
        applicationService.attachDocs(id, keys);
        return ResponseEntity.ok("Documents uploaded successfully");
    }

    @Operation(
            summary = "View my seller applications",
            description = "Returns all seller applications submitted by the currently logged-in user. " +
                          "Useful for tracking the status of your application (PENDING, APPROVED, REJECTED). " +
                          "Available to all authenticated users including already-approved sellers who want to see their history."
    )
    @GetMapping("/applications/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SellerApplicationResponseDTO>> myApplications() {
        Long userId = authFacade.getCurrentUser().getId();
        return ResponseEntity.ok(applicationService.getMine(userId));
    }
}

//Why isAuthenticated() on myApplications: Once a USER is approved and becomes a SELLER, they no longer have APPLY_SELLER in their permissions. But they
//should still be able to view their application status. Using isAuthenticated() allows all roles to check their own history.