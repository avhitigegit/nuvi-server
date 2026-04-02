package com.nuvi.online_renting.itemreviews.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.itemreviews.dto.ItemReviewRequestDTO;
import com.nuvi.online_renting.itemreviews.dto.ItemReviewResponseDTO;
import com.nuvi.online_renting.itemreviews.service.ItemReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/item-reviews")
@Tag(name = "Item Reviews", description = "Item-specific ratings and reviews. Renters can review an item after a COMPLETED booking.")
public class ItemReviewController {

    private final ItemReviewService itemReviewService;

    public ItemReviewController(ItemReviewService itemReviewService) {
        this.itemReviewService = itemReviewService;
    }

    @Operation(
            summary = "Submit an item review",
            description = "Submit a rating (1-5) and optional comment for an item. " +
                          "Only allowed after the booking is COMPLETED. One review per booking."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_REVIEW')")
    public ResponseEntity<ItemReviewResponseDTO> submitReview(@Valid @RequestBody ItemReviewRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemReviewService.submitReview(dto));
    }

    @Operation(summary = "Get item review by ID", description = "Returns a single item review by its ID.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_REVIEWS')")
    public ResponseEntity<ItemReviewResponseDTO> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(itemReviewService.getReviewById(id));
    }

    @Operation(
            summary = "Get reviews for an item",
            description = "Returns paginated reviews for a specific item, sorted by most recent. No authentication required."
    )
    @GetMapping("/item/{itemId}")
    public ResponseEntity<PagedResponse<ItemReviewResponseDTO>> getReviewsByItem(
            @PathVariable Long itemId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(itemReviewService.getReviewsByItem(itemId, pageable));
    }
}
