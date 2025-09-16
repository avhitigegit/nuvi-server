package com.nuvi.online_renting.item.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double pricePerDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

}
