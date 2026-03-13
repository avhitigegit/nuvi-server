package com.nuvi.online_renting.common.audit;

import java.time.LocalDateTime;

public class AuditLogResponseDTO {

    private Long id;
    private String adminEmail;
    private String action;
    private String resourceType;
    private String resourceId;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogResponseDTO from(AuditLog log) {
        AuditLogResponseDTO dto = new AuditLogResponseDTO();
        dto.id           = log.getId();
        dto.adminEmail   = log.getAdminEmail();
        dto.action       = log.getAction();
        dto.resourceType = log.getResourceType();
        dto.resourceId   = log.getResourceId();
        dto.ipAddress    = log.getIpAddress();
        dto.createdAt    = log.getCreatedAt();
        return dto;
    }

    public Long getId()             { return id; }
    public String getAdminEmail()   { return adminEmail; }
    public String getAction()       { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId()   { return resourceId; }
    public String getIpAddress()    { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
