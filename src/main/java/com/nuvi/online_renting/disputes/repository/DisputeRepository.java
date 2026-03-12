package com.nuvi.online_renting.disputes.repository;

import com.nuvi.online_renting.common.enums.DisputeStatus;
import com.nuvi.online_renting.disputes.model.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    // Check if a dispute already exists for this booking by this user
    boolean existsByBookingIdAndRaisedById(Long bookingId, Long userId);

    // Filter disputes — admin sees all, user sees own
    @Query("SELECT d FROM Dispute d WHERE " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:userId IS NULL OR d.raisedBy.id = :userId)")
    Page<Dispute> filterDisputes(@Param("status") DisputeStatus status,
                                 @Param("userId") Long userId,
                                 Pageable pageable);
}
