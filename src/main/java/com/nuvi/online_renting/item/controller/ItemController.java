package com.nuvi.online_renting.item.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.item.dto.ItemBlockedDateRequestDTO;
import com.nuvi.online_renting.item.dto.ItemBlockedDateResponseDTO;
import com.nuvi.online_renting.item.dto.ItemImageResponseDTO;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.service.ItemService;
import jakarta.validation.Valid;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "Items", description = "Browse, search, and manage rental item listings. Sellers create and manage their own items. All users can search and view items without authentication.")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "Create a new item listing", description = "Seller creates a new item available for rent. The logged-in seller is automatically set as the owner of the item. Requires SELLER or ADMIN role.")
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ITEM')")
    public ResponseEntity<ItemResponseDTO> createItem(@Valid @RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.createItem(itemRequestDTO));
    }

    @Operation(summary = "Get item by ID", description = "Retrieve full details of a single item by its ID. No authentication required.")
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getItemById(
            @PathVariable Long id,
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS)
                        .staleWhileRevalidate(5, TimeUnit.MINUTES)
                        .cachePublic())
                .body(itemService.getItemById(id, currency));
    }

    @Operation(
            summary = "Search and browse items",
            description = "Search items with optional filters: name (partial match), price range, availability, and seller. " +
                          "Add lat, lng, and radiusKm for location-based search — returns items within the given radius, sorted by distance. " +
                          "Supports pagination and sorting. No authentication required. " +
                          "Example: GET /api/v1/items?name=bike&available=true&lat=6.9271&lng=79.8612&radiusKm=10&page=0&size=10"
    )
    @GetMapping
    public ResponseEntity<PagedResponse<ItemResponseDTO>> getAllItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String currency,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(itemService.searchItems(name, minPrice, maxPrice, available, sellerId, categoryId, lat, lng, radiusKm, currency, pageable));
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
                                                      @Valid @RequestBody ItemRequestDTO itemRequestDTO) {
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

    // ─── Availability Calendar ────────────────────────────────────────────────

    @Operation(
            summary = "Block a date range for an item",
            description = "Seller blocks a date range on their item (e.g. maintenance, personal use). " +
                          "Renters cannot book the item during this period. " +
                          "Send startDate and endDate as YYYY-MM-DD."
    )
    @PostMapping("/{id}/blocked-dates")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<ItemBlockedDateResponseDTO> addBlockedDate(
            @PathVariable Long id,
            @Valid @RequestBody ItemBlockedDateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addBlockedDate(id, dto));
    }

    @Operation(
            summary = "Get blocked dates for an item",
            description = "Returns all seller-blocked date ranges for an item. No authentication required. " +
                          "Use this to render an availability calendar on the frontend."
    )
    @GetMapping("/{id}/blocked-dates")
    public ResponseEntity<List<ItemBlockedDateResponseDTO>> getBlockedDates(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getBlockedDates(id));
    }

    @Operation(
            summary = "Remove a blocked date range",
            description = "Seller removes a previously blocked date range from their item."
    )
    @DeleteMapping("/{id}/blocked-dates/{blockedDateId}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<Void> removeBlockedDate(@PathVariable Long id,
                                                   @PathVariable Long blockedDateId) {
        itemService.removeBlockedDate(id, blockedDateId);
        return ResponseEntity.noContent().build();
    }

    // ─── Multi-image Gallery ───────────────────────────────────────────────────

    @Operation(
            summary = "Add a gallery image to an item",
            description = "Upload an additional photo to an item's image gallery (max 10 images). " +
                          "Send as multipart/form-data with field name 'file'. " +
                          "Use 'displayOrder' to control position (0 = first). " +
                          "Only the item owner (seller) or an ADMIN can upload."
    )
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<ItemImageResponseDTO> addGalleryImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int displayOrder) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.addGalleryImage(id, file, displayOrder));
    }

    @Operation(
            summary = "Get all gallery images for an item",
            description = "Returns all gallery images for an item ordered by displayOrder then upload time. " +
                          "Each entry includes a pre-signed URL valid for 60 minutes. No authentication required."
    )
    @GetMapping("/{id}/images")
    public ResponseEntity<java.util.List<ItemImageResponseDTO>> getGalleryImages(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getGalleryImages(id));
    }

    @Operation(
            summary = "Delete a gallery image from an item",
            description = "Permanently removes a specific gallery image. " +
                          "Only the item owner (seller) or an ADMIN can delete."
    )
    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteGalleryImage(@PathVariable Long id,
                                                    @PathVariable Long imageId) {
        itemService.deleteGalleryImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get item image", description = "Redirects to the item's image URL stored in AWS S3. No authentication required. Returns 404 if no image has been uploaded yet.")
    @GetMapping("/{id}/image")
    public ResponseEntity<Void> getImage(@PathVariable Long id) {
        String imageUrl = itemService.getImagePath(id);
        if (imageUrl == null || imageUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // Cache the redirect for 5 minutes — safe because S3 pre-signed URLs are valid for 60 minutes
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, imageUrl);
        headers.setCacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic());
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}