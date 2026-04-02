package com.nuvi.online_renting.appeals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AppealRequestDTO {

    @NotBlank(message = "Appeal reason cannot be empty")
    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
