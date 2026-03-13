package com.nuvi.online_renting.item.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double pricePerDay;
    private boolean available;

    // Seller info
    private Long sellerId;
    private String sellerName;

    private String imageUrl;

    // Location
    private Double latitude;
    private Double longitude;
    private Double distanceKm; // populated only when location search is used

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
