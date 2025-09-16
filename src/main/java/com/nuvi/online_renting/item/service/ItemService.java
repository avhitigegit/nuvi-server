package com.nuvi.online_renting.item.service;

import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;

import java.util.List;

public interface ItemService {
    ItemResponseDTO createItem(ItemRequestDTO itemRequestDTO);

    ItemResponseDTO getItemById(Long id);

    List<ItemResponseDTO> getAllItems();

    ItemResponseDTO updateItem(Long id, ItemRequestDTO itemRequestDTO);

    void deleteItem(Long id);
}
