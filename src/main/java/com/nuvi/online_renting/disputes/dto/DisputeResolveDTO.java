package com.nuvi.online_renting.disputes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DisputeResolveDTO {

    @NotBlank(message = "Resolution note is required")
    @Size(min = 5, max = 1000, message = "Resolution note must be between 5 and 1000 characters")
    private String resolutionNote;

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}
