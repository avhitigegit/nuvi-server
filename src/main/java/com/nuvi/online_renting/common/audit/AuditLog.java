package com.nuvi.online_renting.common.audit;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Persistent record of every admin action.
 *
 * One row = one admin action. Rows are never updated or deleted — this table
 * is an append-only compliance log.
 *
 * Fields:
 *   adminEmail   — who performed the action (from JWT)
 *   action       — what was done  (e.g. SELLER_APPROVED, USER_DELETED)
 *   resourceType — which entity was affected (e.g. SellerApplication, User)
 *   resourceId   — PK of the affected entity (null for bulk/non-entity actions)
 *   ipAddress    — client IP at the time of the request
 *   createdAt    — when the action was recorded (UTC)
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_admin_email", columnList = "admin_email"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_created_at",  columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email", nullable = false, length = 150)
    private String adminEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 60)
    private String resourceType;

    @Column(name = "resource_id", length = 40)
    private String resourceId;

    @Column(name = "ip_address", length = 45) // 45 chars covers IPv6
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters
    public Long getId()             { return id; }
    public String getAdminEmail()   { return adminEmail; }
    public String getAction()       { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId()   { return resourceId; }
    public String getIpAddress()    { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setAdminEmail(String adminEmail)     { this.adminEmail = adminEmail; }
    public void setAction(String action)             { this.action = action; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceId(String resourceId)     { this.resourceId = resourceId; }
    public void setIpAddress(String ipAddress)       { this.ipAddress = ipAddress; }
}
