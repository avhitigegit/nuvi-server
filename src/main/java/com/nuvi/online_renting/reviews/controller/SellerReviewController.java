package com.nuvi.online_renting.reviews.controller;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.reviews.dto.SellerReviewRequestDTO;
import com.nuvi.online_renting.reviews.dto.SellerReviewResponseDTO;
import com.nuvi.online_renting.reviews.service.SellerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Seller Reviews", description = "Submit and view seller ratings and reviews. Only renters with a completed booking can review a seller.")
public class SellerReviewController {

    private final SellerReviewService reviewService;

    @Operation(
            summary = "Submit a review for a seller",
            description = "A renter submits a star rating (1–5) and optional comment for the seller " +
                          "after a booking is COMPLETED. Only one review is allowed per booking."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_REVIEW')")
    public ResponseEntity<ApiResponse<SellerReviewResponseDTO>> submitReview(@Valid @RequestBody SellerReviewRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Review submitted successfully", reviewService.submitReview(dto)));
    }

    @Operation(summary = "Get a review by ID", description = "Retrieve details of a single review by its ID. Accessible to all authenticated users.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_REVIEWS')")
    public ResponseEntity<ApiResponse<SellerReviewResponseDTO>> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Review fetched", reviewService.getReviewById(id)));
    }

    @Operation(
            summary = "Get all reviews for a seller",
            description = "Returns a paginated list of reviews for a specific seller, ordered by most recent. " +
                          "Example: GET /api/v1/reviews/seller/3?page=0&size=10"
    )
    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("hasAuthority('VIEW_REVIEWS')")
    public ResponseEntity<ApiResponse<PagedResponse<SellerReviewResponseDTO>>> getReviewsBySeller(
            @PathVariable Long sellerId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Reviews fetched", reviewService.getReviewsBySeller(sellerId, pageable)));
    }
}
