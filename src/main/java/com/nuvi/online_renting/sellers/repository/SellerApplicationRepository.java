package com.nuvi.online_renting.sellers.repository;

import com.nuvi.online_renting.common.enums.SellerStatus;
import com.nuvi.online_renting.sellers.model.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    List<SellerApplication> findByStatus(SellerStatus status);
    List<SellerApplication> findByUserId(Long userId);
}