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

    private Long categoryId;
    private String categoryName;

    private double averageRating;
    private int totalReviews;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
