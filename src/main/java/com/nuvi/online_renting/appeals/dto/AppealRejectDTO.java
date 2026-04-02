package com.nuvi.online_renting.appeals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AppealRejectDTO {

    @NotBlank(message = "Rejection comment cannot be empty")
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
