package com.nuvi.online_renting.itemreviews.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.itemreviews.dto.ItemReviewRequestDTO;
import com.nuvi.online_renting.itemreviews.dto.ItemReviewResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ItemReviewService {

    ItemReviewResponseDTO submitReview(ItemReviewRequestDTO dto);

    ItemReviewResponseDTO getReviewById(Long id);

    PagedResponse<ItemReviewResponseDTO> getReviewsByItem(Long itemId, Pageable pageable);
}
