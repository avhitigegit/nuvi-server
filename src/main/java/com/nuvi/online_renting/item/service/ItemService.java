package com.nuvi.online_renting.item.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ItemService {

    ItemResponseDTO createItem(ItemRequestDTO itemRequestDTO);

    ItemResponseDTO getItemById(Long id);

    PagedResponse<ItemResponseDTO> searchItems(String name, Double minPrice, Double maxPrice,
                                               Boolean available, Long sellerId, Pageable pageable);

    ItemResponseDTO updateItem(Long id, ItemRequestDTO itemRequestDTO);

    void deleteItem(Long id);

    PagedResponse<ItemResponseDTO> getMyItems(Pageable pageable);
}
