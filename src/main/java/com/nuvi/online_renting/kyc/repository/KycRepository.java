package com.nuvi.online_renting.kyc.repository;

import com.nuvi.online_renting.common.enums.KycStatus;
import com.nuvi.online_renting.kyc.model.KycSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycRepository extends JpaRepository<KycSubmission, Long> {

    List<KycSubmission> findByUserId(Long userId);

    @Query("SELECT k FROM KycSubmission k WHERE (:status IS NULL OR k.status = :status)")
    Page<KycSubmission> findAllByStatus(@Param("status") KycStatus status, Pageable pageable);
}
