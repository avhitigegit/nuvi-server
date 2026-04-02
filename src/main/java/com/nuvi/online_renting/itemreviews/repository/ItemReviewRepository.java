package com.nuvi.online_renting.itemreviews.repository;

import com.nuvi.online_renting.itemreviews.model.ItemReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemReviewRepository extends JpaRepository<ItemReview, Long> {

    boolean existsByBookingIdAndReviewerId(Long bookingId, Long reviewerId);

    Page<ItemReview> findByItemId(Long itemId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ItemReview r WHERE r.item.id = :itemId")
    double calculateAverageRating(@Param("itemId") Long itemId);

    long countByItemId(Long itemId);
}
