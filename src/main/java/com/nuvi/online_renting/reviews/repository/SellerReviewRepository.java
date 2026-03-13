package com.nuvi.online_renting.reviews.repository;

import com.nuvi.online_renting.reviews.model.SellerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerReviewRepository extends JpaRepository<SellerReview, Long> {

    boolean existsByBookingIdAndReviewerId(Long bookingId, Long reviewerId);

    Page<SellerReview> findBySellerId(Long sellerId, Pageable pageable);

    // Compute average rating for a seller across all their reviews
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM SellerReview r WHERE r.seller.id = :sellerId")
    double computeAverageRating(@Param("sellerId") Long sellerId);

    long countBySellerId(Long sellerId);
}
