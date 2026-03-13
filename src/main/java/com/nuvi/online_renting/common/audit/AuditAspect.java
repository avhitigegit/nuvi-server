package com.nuvi.online_renting.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Intercepts every method annotated with @Auditable and writes a row to audit_log.
 *
 * Runs AFTER the method returns successfully (AfterReturning) so failed requests
 * are never recorded — only committed admin actions appear in the audit trail.
 *
 * Resource ID extraction:
 *   Inspects the method arguments for the first Long value — this is conventionally
 *   the @PathVariable entity ID (e.g. /applications/{id}, /{userId}/suspend).
 *   If no Long is found (e.g. bulk list endpoints) resourceId is recorded as null.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @AfterReturning("@annotation(auditable)")
    public void logAdminAction(JoinPoint joinPoint, Auditable auditable) {
        try {
            String adminEmail = resolveAdminEmail();
            String resourceId = resolveResourceId(joinPoint.getArgs());
            String ipAddress  = resolveClientIp();

            auditLogService.record(adminEmail, auditable.action(),
                    auditable.resourceType(), resourceId, ipAddress);

        } catch (Exception e) {
            // Audit logging must never break the HTTP response
            log.error("AuditAspect failed to record action={}", auditable.action(), e);
        }
    }

    private String resolveAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "unknown";
    }

    /**
     * Returns the string form of the first Long argument found in args[].
     * Covers all current admin methods where the first path variable is the entity ID.
     */
    private String resolveResourceId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long) {
                return arg.toString();
            }
        }
        return null;
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
