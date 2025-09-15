package com.nuvi.online_renting.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ItemRequestDTO {
    private Long id;
    @NotBlank(message = "Item name cannot be empty")
    private String name;
    private String description;
    @NotNull(message = "Price per day is required")
    @Positive(message = "Price must be greater than 0")
    private Double pricePerDay;
    private Boolean available;

    public ItemRequestDTO() {
    }

    public ItemRequestDTO(Long id, String name, String description, Double pricePerDay, Boolean available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pricePerDay = pricePerDay;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(Double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}