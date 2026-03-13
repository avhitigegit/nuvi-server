package com.nuvi.online_renting.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records one admin action.
     *
     * Runs in its own transaction (REQUIRES_NEW) so an audit log entry is always
     * written even if the calling transaction is later rolled back.
     *
     * @param adminEmail   email of the admin who performed the action
     * @param action       action constant, e.g. "SELLER_APPROVED"
     * @param resourceType entity type affected, e.g. "SellerApplication"
     * @param resourceId   PK of the entity (may be null for non-entity actions)
     * @param ipAddress    client IP address
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String adminEmail, String action,
                       String resourceType, String resourceId, String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAdminEmail(adminEmail);
            entry.setAction(action);
            entry.setResourceType(resourceType);
            entry.setResourceId(resourceId);
            entry.setIpAddress(ipAddress);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Never let audit logging break the actual business operation
            log.error("Failed to write audit log: action={} admin={}", action, adminEmail, e);
        }
    }

    /**
     * Paginated, filterable query for the admin UI.
     * All filter parameters are optional — pass null to skip that filter.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findFiltered(String adminEmail, String action, String resourceType,
                                       LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findFiltered(adminEmail, action, resourceType, from, to, pageable);
    }
}
