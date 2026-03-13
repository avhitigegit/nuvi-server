package com.nuvi.online_renting.common.audit;

import java.lang.annotation.*;

/**
 * Marks a controller method for automatic admin audit logging.
 *
 * Place on any admin method that mutates state. The AuditAspect intercepts
 * the method AFTER it returns successfully and writes one row to audit_log.
 *
 * Usage:
 *   @Auditable(action = "SELLER_APPROVED", resourceType = "SellerApplication")
 *   public ResponseEntity<?> decide(@PathVariable Long id, ...) { ... }
 *
 * The resource ID is automatically extracted from the first Long parameter
 * of the annotated method (typically the @PathVariable entity ID).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** Short, SCREAMING_SNAKE_CASE description of the action. E.g. "SELLER_APPROVED". */
    String action();

    /** The entity type being affected. E.g. "SellerApplication", "User", "Dispute". */
    String resourceType();
}
