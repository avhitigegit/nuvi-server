package com.nuvi.online_renting.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Filtered query — all parameters are optional (null = no filter on that field).
     * Results ordered by createdAt DESC so the latest actions appear first.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:adminEmail  IS NULL OR a.adminEmail  = :adminEmail)
              AND (:action       IS NULL OR a.action      = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (:from         IS NULL OR a.createdAt  >= :from)
              AND (:to           IS NULL OR a.createdAt  <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findFiltered(
            @Param("adminEmail")   String adminEmail,
            @Param("action")       String action,
            @Param("resourceType") String resourceType,
            @Param("from")         LocalDateTime from,
            @Param("to")           LocalDateTime to,
            Pageable pageable
    );
}
