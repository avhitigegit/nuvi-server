package com.nuvi.online_renting.sellers.controller;

import com.nuvi.online_renting.common.enums.SellerStatus;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.dto.SellerDecisionDTO;
import com.nuvi.online_renting.sellers.service.SellerApplicationService;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {
    private final SellerApplicationService applicationService;
    @GetMapping("/applications")
    public ResponseEntity<List<SellerApplicationResponseDTO>> list(@RequestParam(required = false) SellerStatus status) {
        return ResponseEntity.ok(applicationService.getByStatus(status == null ? null : status.name()));
    }

    @PatchMapping("/applications/{id}")
    public ResponseEntity<SellerApplicationResponseDTO> decide(@PathVariable Long id,
                                                               @Valid @RequestBody SellerDecisionDTO decision) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(applicationService.decide(id, decision, adminEmail));
    }
}
