package com.nuvi.online_renting.common.audit;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAuthority('FULL_ACCESS')")
@Tag(name = "Admin — Audit Logs", description = "Read-only access to the admin audit trail. " +
        "Every admin action (approve/reject seller, delete user, resolve dispute, etc.) " +
        "is automatically recorded here.")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(
            summary = "Query audit logs (Admin)",
            description = "Returns a paginated, filterable list of admin actions. " +
                          "All filter parameters are optional and can be combined. " +
                          "Results are always sorted most-recent first. " +
                          "Example: GET /api/v1/admin/audit-logs?action=SELLER_APPROVED&from=2026-01-01T00:00:00"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponseDTO>>> getAuditLogs(
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<AuditLogResponseDTO> result = new PagedResponse<>(
                auditLogService.findFiltered(adminEmail, action, resourceType, from, to, pageable)
                               .map(AuditLogResponseDTO::from)
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs fetched", result));
    }
}
