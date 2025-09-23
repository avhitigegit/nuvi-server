package com.nuvi.online_renting.sellers.service;

import com.nuvi.online_renting.sellers.dto.SellerApplicationRequestDTO;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.dto.SellerDecisionDTO;

import java.util.List;

public interface SellerApplicationService {

    SellerApplicationResponseDTO apply(Long userId, SellerApplicationRequestDTO dto, List<String> uploadedDocUrls);

    SellerApplicationResponseDTO getById(Long id);

    List<SellerApplicationResponseDTO> getMine(Long userId);

    List<SellerApplicationResponseDTO> getByStatus(String status); // admin

    SellerApplicationResponseDTO decide(Long applicationId, SellerDecisionDTO decision, String adminEmail);

}
