package com.nuvi.online_renting.sellers.controller;


import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.storage.FileStorageService;
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

    @PostMapping(value = "/apply", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<SellerApplicationResponseDTO> apply(
            @Valid @RequestPart("data") SellerApplicationRequestDTO data,
            @RequestPart(value = "docs", required = false) List<MultipartFile> docs) {

        Long userId = authFacade.getCurrentUser().getId();
        List<String> urls = fileStorageService.storeFiles(docs); // returns list of accessible URLs or paths
        SellerApplicationResponseDTO result = applicationService.apply(userId, data, urls);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/applications/me")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<List<SellerApplicationResponseDTO>> myApplications() {
        Long userId = authFacade.getCurrentUser().getId();
        return ResponseEntity.ok(applicationService.getMine(userId));
    }
}
