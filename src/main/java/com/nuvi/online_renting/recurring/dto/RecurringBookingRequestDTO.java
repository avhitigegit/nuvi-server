package com.nuvi.online_renting.recurring.dto;

import com.nuvi.online_renting.common.enums.RecurrenceFrequency;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class RecurringBookingRequestDTO {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @Positive(message = "Duration must be at least 1 day")
    private int durationDays;

    @NotNull(message = "Frequency is required")
    private RecurrenceFrequency frequency;

    @NotNull(message = "First occurrence date is required")
    @FutureOrPresent(message = "First occurrence date cannot be in the past")
    private LocalDate firstOccurrenceDate;

    // Optional stop conditions
    private LocalDate endAfterDate;
    private Integer maxOccurrences;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public RecurrenceFrequency getFrequency() { return frequency; }
    public void setFrequency(RecurrenceFrequency frequency) { this.frequency = frequency; }

    public LocalDate getFirstOccurrenceDate() { return firstOccurrenceDate; }
    public void setFirstOccurrenceDate(LocalDate firstOccurrenceDate) { this.firstOccurrenceDate = firstOccurrenceDate; }

    public LocalDate getEndAfterDate() { return endAfterDate; }
    public void setEndAfterDate(LocalDate endAfterDate) { this.endAfterDate = endAfterDate; }

    public Integer getMaxOccurrences() { return maxOccurrences; }
    public void setMaxOccurrences(Integer maxOccurrences) { this.maxOccurrences = maxOccurrences; }
}
