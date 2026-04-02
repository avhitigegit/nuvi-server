package com.nuvi.online_renting.recurring.model;

import com.nuvi.online_renting.common.enums.RecurrenceFrequency;
import com.nuvi.online_renting.common.enums.RecurringBookingStatus;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.users.model.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_bookings")
@EntityListeners(AuditingEntityListener.class)
public class RecurringBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // How long each rental period lasts (in days)
    @Column(nullable = false)
    private int durationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    // Date the first occurrence starts
    @Column(nullable = false)
    private LocalDate firstOccurrenceDate;

    // Date the next booking will be created by the scheduler
    @Column(nullable = false)
    private LocalDate nextOccurrenceDate;

    // Optional: stop creating occurrences after this date (null = run until cancelled)
    private LocalDate endAfterDate;

    // Optional: stop after this many total occurrences (null = unlimited)
    private Integer maxOccurrences;

    @Column(nullable = false)
    private int occurrencesCreated = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringBookingStatus status = RecurringBookingStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

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

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
