package com.nuvi.online_renting.appeals.repository;

import com.nuvi.online_renting.appeals.model.SuspensionAppeal;
import com.nuvi.online_renting.common.enums.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuspensionAppealRepository extends JpaRepository<SuspensionAppeal, Long> {

    List<SuspensionAppeal> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    Page<SuspensionAppeal> findByStatus(AppealStatus status, Pageable pageable);

    boolean existsBySellerIdAndStatus(Long sellerId, AppealStatus status);
}
