package com.nuvi.online_renting.sellers.controller;

import com.nuvi.online_renting.common.storage.FileStorageService;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.sellers.dto.SellerApplicationRequestDTO;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.service.SellerApplicationService;
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
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final AuthenticationFacade authFacade;

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('APPLY_SELLER')")
    public ResponseEntity<SellerApplicationResponseDTO> apply(
            @Valid @RequestBody SellerApplicationRequestDTO sellerApplicationRequestDTO) {
        Long userId = authFacade.getCurrentUser().getId();
        SellerApplicationResponseDTO result = applicationService.apply(userId, sellerApplicationRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping(value = "/apply/{id}/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('UPLOAD_SELLER_DOCS')")
    public ResponseEntity<String> uploadDocs(
            @PathVariable Long id,
            @RequestPart("docs") List<MultipartFile> docs) {
        long maxFileSize = 50 * 1024 * 1024;
        for (MultipartFile file : docs) {
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.badRequest()
                        .body("File " + file.getOriginalFilename() + " exceeds 50MB limit");
            }
        }
        List<String> urls = fileStorageService.storeFiles(docs);
        applicationService.attachDocs(id, urls);
        return ResponseEntity.ok("Documents uploaded successfully");
    }

    @GetMapping("/applications/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SellerApplicationResponseDTO>> myApplications() {
        Long userId = authFacade.getCurrentUser().getId();
        return ResponseEntity.ok(applicationService.getMine(userId));
    }
}

//Why isAuthenticated() on myApplications: Once a USER is approved and becomes a SELLER, they no longer have APPLY_SELLER in their permissions. But they
//should still be able to view their application status. Using isAuthenticated() allows all roles to check their own history.