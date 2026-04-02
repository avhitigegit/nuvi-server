package com.nuvi.online_renting.recurring.dto;

import com.nuvi.online_renting.common.enums.RecurrenceFrequency;
import com.nuvi.online_renting.common.enums.RecurringBookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecurringBookingResponseDTO {

    private Long id;
    private Long userId;
    private String userName;
    private Long itemId;
    private String itemName;
    private int durationDays;
    private RecurrenceFrequency frequency;
    private LocalDate firstOccurrenceDate;
    private LocalDate nextOccurrenceDate;
    private LocalDate endAfterDate;
    private Integer maxOccurrences;
    private int occurrencesCreated;
    private RecurringBookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public RecurrenceFrequency getFrequency() { return frequency; }
    public void setFrequency(RecurrenceFrequency frequency) { this.frequency = frequency; }

    public LocalDate getFirstOccurrenceDate() { return firstOccurrenceDate; }
    public void setFirstOccurrenceDate(LocalDate firstOccurrenceDate) { this.firstOccurrenceDate = firstOccurrenceDate; }

    public LocalDate getNextOccurrenceDate() { return nextOccurrenceDate; }
    public void setNextOccurrenceDate(LocalDate nextOccurrenceDate) { this.nextOccurrenceDate = nextOccurrenceDate; }

    public LocalDate getEndAfterDate() { return endAfterDate; }
    public void setEndAfterDate(LocalDate endAfterDate) { this.endAfterDate = endAfterDate; }

    public Integer getMaxOccurrences() { return maxOccurrences; }
    public void setMaxOccurrences(Integer maxOccurrences) { this.maxOccurrences = maxOccurrences; }

    public int getOccurrencesCreated() { return occurrencesCreated; }
    public void setOccurrencesCreated(int occurrencesCreated) { this.occurrencesCreated = occurrencesCreated; }

    public RecurringBookingStatus getStatus() { return status; }
    public void setStatus(RecurringBookingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
