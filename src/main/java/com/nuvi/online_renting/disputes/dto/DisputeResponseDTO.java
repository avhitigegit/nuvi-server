package com.nuvi.online_renting.disputes.dto;

import com.nuvi.online_renting.common.enums.DisputeReason;
import com.nuvi.online_renting.common.enums.DisputeStatus;

import java.time.LocalDateTime;

public class DisputeResponseDTO {

    private Long id;

    private Long bookingId;
    private Long raisedByUserId;
    private String raisedByUserName;

    private DisputeReason reason;
    private String description;
    private DisputeStatus status;

    private String resolutionNote;
    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getRaisedByUserId() { return raisedByUserId; }
    public void setRaisedByUserId(Long raisedByUserId) { this.raisedByUserId = raisedByUserId; }

    public String getRaisedByUserName() { return raisedByUserName; }
    public void setRaisedByUserName(String raisedByUserName) { this.raisedByUserName = raisedByUserName; }

    public DisputeReason getReason() { return reason; }
    public void setReason(DisputeReason reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
