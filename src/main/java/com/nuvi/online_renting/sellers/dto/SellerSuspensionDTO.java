package com.nuvi.online_renting.sellers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SellerSuspensionDTO {

    @NotBlank(message = "Suspension reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
