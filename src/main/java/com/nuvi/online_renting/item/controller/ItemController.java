package com.nuvi.online_renting.item.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Items", description = "Browse, search, and manage rental item listings. Sellers create and manage their own items. All users can search and view items without authentication.")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "Create a new item listing", description = "Seller creates a new item available for rent. The logged-in seller is automatically set as the owner of the item. Requires SELLER or ADMIN role.")
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ITEM')")
    public ResponseEntity<ItemResponseDTO> createItem(@RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.createItem(itemRequestDTO));
    }

    @Operation(summary = "Get item by ID", description = "Retrieve full details of a single item by its ID. No authentication required.")
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @Operation(
            summary = "Search and browse items",
            description = "Search items with optional filters: name (partial match), price range, availability, and seller. " +
                          "Supports pagination and sorting. No authentication required. " +
                          "Example: GET /api/items?name=bike&minPrice=500&maxPrice=3000&available=true&page=0&size=10&sort=pricePerDay,asc"
    )
    @GetMapping
    public ResponseEntity<PagedResponse<ItemResponseDTO>> getAllItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long sellerId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(itemService.searchItems(name, minPrice, maxPrice, available, sellerId, pageable));
    }

    @Operation(summary = "Get my item listings", description = "Returns a paginated list of all items created by the currently logged-in seller. Requires SELLER or ADMIN role.")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CREATE_ITEM')")
    public ResponseEntity<PagedResponse<ItemResponseDTO>> getMyItems(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(itemService.getMyItems(pageable));
    }

    @Operation(summary = "Update an item listing", description = "Update the details of an existing item. Only the seller who owns the item or an ADMIN can perform this action.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<ItemResponseDTO> updateItem(@PathVariable Long id,
                                                      @RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.updateItem(id, itemRequestDTO));
    }

    @Operation(summary = "Delete an item listing", description = "Permanently removes an item from the platform. Only the seller who owns the item or an ADMIN can perform this action.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DELETE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Upload an image for an item",
            description = "Upload a photo for a specific item listing. Send as multipart/form-data with field name 'file'. " +
                          "Allowed formats: jpg, jpeg, png, gif, webp. Max size: 50MB. " +
                          "Only the item owner (seller) or an ADMIN can upload an image."
    )
    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<ItemResponseDTO> uploadImage(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(itemService.uploadImage(id, file));
    }

    @Operation(summary = "Get item image", description = "Returns the uploaded image file for the given item. No authentication required. Returns 404 if no image has been uploaded yet.")
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        String imagePath = itemService.getImagePath(id);
        if (imagePath == null || imagePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(imagePath);
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(resource);
    }
}