package com.nuvi.online_renting.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SendMessageRequestDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String content;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
