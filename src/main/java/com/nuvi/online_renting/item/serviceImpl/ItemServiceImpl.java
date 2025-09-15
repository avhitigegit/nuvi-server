package com.nuvi.online_renting.item.serviceImpl;

import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.item.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public ItemResponseDTO createItem(ItemRequestDTO itemRequestDTO) {
        Item item = new Item();
        item.setName(itemRequestDTO.getName());
        item.setDescription(itemRequestDTO.getDescription());
        item.setPricePerDay(itemRequestDTO.getPricePerDay());

        Item savedItem = itemRepository.save(item);
        return convertToItemResponseDTO(savedItem);
    }

    @Override
    @Transactional
    public ItemResponseDTO getItemById(Long id) {
        return itemRepository.findById(id)
                .map(this::convertToItemResponseDTO)
                .orElseThrow(() -> new RuntimeException("Item not found with id " + id));
    }

    @Override
    @Transactional
    public List<ItemResponseDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::convertToItemResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ItemResponseDTO updateItem(Long id, ItemRequestDTO itemRequestDTO) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id " + id));

        item.setName(itemRequestDTO.getName());
        item.setDescription(itemRequestDTO.getDescription());
        item.setPricePerDay(itemRequestDTO.getPricePerDay());

        Item updatedItem = itemRepository.save(item);
        return convertToItemResponseDTO(updatedItem);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    private ItemResponseDTO convertToItemResponseDTO(Item item) {
        ItemResponseDTO itemResponseDTO = new ItemResponseDTO();
        itemResponseDTO.setId(item.getId());
        itemResponseDTO.setName(item.getName());
        itemResponseDTO.setDescription(item.getDescription());
        itemResponseDTO.setPricePerDay(item.getPricePerDay());
        return itemResponseDTO;
    }
}