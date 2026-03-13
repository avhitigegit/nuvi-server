package com.nuvi.online_renting.item.serviceImpl;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.storage.S3StorageService;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.item.service.ItemService;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final AuthenticationFacade authFacade;
    private final S3StorageService s3StorageService;

    public ItemServiceImpl(ItemRepository itemRepository,
                           AuthenticationFacade authFacade,
                           S3StorageService s3StorageService) {
        this.itemRepository = itemRepository;
        this.authFacade = authFacade;
        this.s3StorageService = s3StorageService;
    }

    @Override
    @Transactional
    public ItemResponseDTO createItem(ItemRequestDTO dto) {
        User currentUser = authFacade.getCurrentUser();

        if (!currentUser.isKycVerified()) {
            throw new ForbiddenException("Your identity (KYC) has not been verified yet. " +
                    "Please complete your seller application and wait for admin approval before listing items.");
        }

        if (currentUser.isSuspended()) {
            throw new ForbiddenException("Your seller account has been suspended. " +
                    "You cannot list new items. Please contact support.");
        }

        Item item = new Item();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPricePerDay(dto.getPricePerDay());
        item.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : true);
        item.setSeller(currentUser);
        item.setLatitude(dto.getLatitude());
        item.setLongitude(dto.getLongitude());

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
                                                      Boolean available, Long sellerId,
                                                      Double lat, Double lng, Double radiusKm,
                                                      Pageable pageable) {
        // Location search: fetch candidates with coordinates, filter by Haversine in Java
        if (lat != null && lng != null && radiusKm != null) {
            List<Item> candidates = itemRepository.findItemsWithCoordinates(name, minPrice, maxPrice, available, sellerId);

            List<ItemResponseDTO> filtered = new ArrayList<>();
            for (Item item : candidates) {
                double distance = haversineKm(lat, lng, item.getLatitude(), item.getLongitude());
                if (distance <= radiusKm) {
                    ItemResponseDTO dto = convertToResponseDTO(item);
                    dto.setDistanceKm(Math.round(distance * 10.0) / 10.0);
                    filtered.add(dto);
                }
            }

            // Sort by distance ascending
            filtered.sort(Comparator.comparingDouble(ItemResponseDTO::getDistanceKm));

            // Manual pagination
            int pageNum = pageable.getPageNumber();
            int pageSize = pageable.getPageSize();
            int start = Math.min(pageNum * pageSize, filtered.size());
            int end = Math.min(start + pageSize, filtered.size());
            List<ItemResponseDTO> pageContent = filtered.subList(start, end);

            return new PagedResponse<>(pageContent, pageNum, pageSize, filtered.size());
        }

        // Standard search without location
        Page<Item> page = itemRepository.searchItems(name, minPrice, maxPrice, available, sellerId, pageable);
        return new PagedResponse<>(page.map(this::convertToResponseDTO));
    }

    /**
     * Haversine formula — calculates great-circle distance between two GPS points in km.
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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

        if (!isAdmin && !currentUser.isKycVerified()) {
            throw new ForbiddenException("Your identity (KYC) has not been verified. You cannot update items until your seller application is approved.");
        }

        if (!isAdmin && currentUser.isSuspended()) {
            throw new ForbiddenException("Your seller account has been suspended. You cannot update items. Please contact support.");
        }

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPricePerDay(dto.getPricePerDay());
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }
        item.setLatitude(dto.getLatitude());
        item.setLongitude(dto.getLongitude());

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

        item.setDeleted(true);
        item.setDeletedAt(LocalDateTime.now());
        itemRepository.save(item);
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

        // Validate actual file content — prevents renamed executables from being uploaded
        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            String detectedMime = URLConnection.guessContentTypeFromStream(bis);
            if (detectedMime == null || !detectedMime.startsWith("image/")) {
                throw new BadRequestException("File content does not match an image type. Upload aborted.");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read file content for validation.");
        }

        String s3Key = "items/item_" + id + "_" + System.currentTimeMillis() + "." + ext;
        String imageUrl = s3StorageService.uploadFile(s3Key, file);

        item.setImageUrl(imageUrl);
        return convertToResponseDTO(itemRepository.save(item));
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
        dto.setLatitude(item.getLatitude());
        dto.setLongitude(item.getLongitude());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setCreatedBy(item.getCreatedBy());
        dto.setUpdatedBy(item.getUpdatedBy());
        return dto;
    }
}
