package com.nuvi.online_renting.bookings.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingResponseDTO {

    private Long id;

    private Long userId;
    private String userName;

    private Long itemId;
    private String itemName;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    // Return tracking
    private LocalDateTime returnedAt;
    private String returnNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
