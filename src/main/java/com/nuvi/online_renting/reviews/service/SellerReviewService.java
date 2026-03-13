package com.nuvi.online_renting.reviews.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.reviews.dto.SellerReviewRequestDTO;
import com.nuvi.online_renting.reviews.dto.SellerReviewResponseDTO;
import org.springframework.data.domain.Pageable;

public interface SellerReviewService {

    SellerReviewResponseDTO submitReview(SellerReviewRequestDTO dto);

    SellerReviewResponseDTO getReviewById(Long id);

    PagedResponse<SellerReviewResponseDTO> getReviewsBySeller(Long sellerId, Pageable pageable);
}
