package com.nuvi.online_renting.item.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.item.dto.ItemBlockedDateRequestDTO;
import com.nuvi.online_renting.item.dto.ItemBlockedDateResponseDTO;
import com.nuvi.online_renting.item.dto.ItemImageResponseDTO;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ItemService {

    ItemResponseDTO createItem(ItemRequestDTO itemRequestDTO);

    ItemResponseDTO getItemById(Long id, String currency);

    PagedResponse<ItemResponseDTO> searchItems(String name, Double minPrice, Double maxPrice,
                                               Boolean available, Long sellerId, Long categoryId,
                                               Double lat, Double lng, Double radiusKm,
                                               String currency, Pageable pageable);

    ItemResponseDTO updateItem(Long id, ItemRequestDTO itemRequestDTO);

    void deleteItem(Long id);

    PagedResponse<ItemResponseDTO> getMyItems(Pageable pageable);

    // Image upload: seller/admin uploads an image for an item
    ItemResponseDTO uploadImage(Long id, MultipartFile file);

    // Returns the stored file path so the controller can serve it
    String getImagePath(Long id);

    // Availability calendar — seller-managed blocked date ranges
    ItemBlockedDateResponseDTO addBlockedDate(Long itemId, ItemBlockedDateRequestDTO dto);
    List<ItemBlockedDateResponseDTO> getBlockedDates(Long itemId);
    void removeBlockedDate(Long itemId, Long blockedDateId);

    // Multi-image gallery
    ItemImageResponseDTO addGalleryImage(Long itemId, MultipartFile file, int displayOrder);
    List<ItemImageResponseDTO> getGalleryImages(Long itemId);
    void deleteGalleryImage(Long itemId, Long imageId);
}
