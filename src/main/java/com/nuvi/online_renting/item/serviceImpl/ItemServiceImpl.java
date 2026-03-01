package com.nuvi.online_renting.item.serviceImpl;

import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.item.service.ItemService;
import com.nuvi.online_renting.users.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final AuthenticationFacade authFacade;

    public ItemServiceImpl(ItemRepository itemRepository, AuthenticationFacade authFacade) {
        this.itemRepository = itemRepository;
        this.authFacade = authFacade;
    }

    @Override
    @Transactional
    public ItemResponseDTO createItem(ItemRequestDTO dto) {
        User currentUser = authFacade.getCurrentUser();

        Item item = new Item();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPricePerDay(dto.getPricePerDay());
        item.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : true);
        item.setSeller(currentUser); // Auto-assign logged-in user as seller

        return convertToResponseDTO(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponseDTO getItemById(Long id) {
        return itemRepository.findById(id)
                .map(this::convertToResponseDTO)
                .orElseThrow(() -> new RuntimeException("Item not found with id " + id));
    }

    @Override
    @Transactional
    public List<ItemResponseDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ItemResponseDTO updateItem(Long id, ItemRequestDTO dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("You are not allowed to update this item");
        }

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPricePerDay(dto.getPricePerDay());
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }

        return convertToResponseDTO(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("You are not allowed to delete this item");
        }

        itemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<ItemResponseDTO> getMyItems() {
        Long sellerId = authFacade.getCurrentUser().getId();
        return itemRepository.findBySellerId(sellerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    private ItemResponseDTO convertToResponseDTO(Item item) {
        ItemResponseDTO dto = new ItemResponseDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPricePerDay(item.getPricePerDay());
        dto.setAvailable(item.isAvailable());
        dto.setSellerId(item.getSeller().getId());
        dto.setSellerName(item.getSeller().getName());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setCreatedBy(item.getCreatedBy());
        dto.setUpdatedBy(item.getUpdatedBy());
        return dto;
    }
}
