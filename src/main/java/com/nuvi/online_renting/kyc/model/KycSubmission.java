package com.nuvi.online_renting.kyc.model;

import com.nuvi.online_renting.common.enums.KycDocumentType;
import com.nuvi.online_renting.common.enums.KycStatus;
import com.nuvi.online_renting.users.model.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_submissions")
@EntityListeners(AuditingEntityListener.class)
public class KycSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycDocumentType documentType;

    // S3 key for the uploaded document
    @Column(nullable = false)
    private String documentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.PENDING;

    // Admin fills this in on rejection
    @Column(length = 1000)
    private String adminComment;

    private LocalDateTime reviewedAt;

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

    public KycDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(KycDocumentType documentType) { this.documentType = documentType; }

    public String getDocumentKey() { return documentKey; }
    public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }

    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
