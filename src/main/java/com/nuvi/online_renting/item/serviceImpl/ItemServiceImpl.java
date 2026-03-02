package com.nuvi.online_renting.item.serviceImpl;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.item.service.ItemService;
import com.nuvi.online_renting.users.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final AuthenticationFacade authFacade;

    @Value("${app.upload.dir}")
    private String uploadDir;

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
        item.setSeller(currentUser);

        return convertToResponseDTO(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponseDTO getItemById(Long id) {
        return itemRepository.findById(id)
                .map(this::convertToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
    }

    @Override
    @Transactional
    public PagedResponse<ItemResponseDTO> searchItems(String name, Double minPrice, Double maxPrice,
                                                      Boolean available, Long sellerId, Pageable pageable) {
        Page<Item> page = itemRepository.searchItems(name, minPrice, maxPrice, available, sellerId, pageable);
        return new PagedResponse<>(page.map(this::convertToResponseDTO));
    }

    @Override
    @Transactional
    public ItemResponseDTO updateItem(Long id, ItemRequestDTO dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to update this item");
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
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to delete this item");
        }

        itemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PagedResponse<ItemResponseDTO> getMyItems(Pageable pageable) {
        Long sellerId = authFacade.getCurrentUser().getId();
        Page<Item> page = itemRepository.findBySellerId(sellerId, pageable);
        return new PagedResponse<>(page.map(this::convertToResponseDTO));
    }

    @Override
    @Transactional
    public ItemResponseDTO uploadImage(Long id, MultipartFile file) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to upload an image for this item");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Invalid file name");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(ext)) {
            throw new BadRequestException("Only image files are allowed (jpg, jpeg, png, gif, webp)");
        }

        try {
            Path uploadPath = Paths.get(uploadDir, "items");
            Files.createDirectories(uploadPath);

            String fileName = "item_" + id + "_" + System.currentTimeMillis() + "." + ext;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            item.setImageUrl(filePath.toString());
            return convertToResponseDTO(itemRepository.save(item));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image: " + e.getMessage());
        }
    }

    @Override
    public String getImagePath(Long id) {
        return itemRepository.findById(id)
                .map(Item::getImageUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
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
        dto.setImageUrl(item.getImageUrl());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setCreatedBy(item.getCreatedBy());
        dto.setUpdatedBy(item.getUpdatedBy());
        return dto;
    }
}
