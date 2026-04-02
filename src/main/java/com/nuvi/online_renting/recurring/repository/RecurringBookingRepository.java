package com.nuvi.online_renting.recurring.repository;

import com.nuvi.online_renting.common.enums.RecurringBookingStatus;
import com.nuvi.online_renting.recurring.model.RecurringBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, Long> {

    Page<RecurringBooking> findByUserId(Long userId, Pageable pageable);

    // Find all ACTIVE templates whose next occurrence is due within the scheduling horizon
    @Query("SELECT r FROM RecurringBooking r WHERE r.status = 'ACTIVE' AND r.nextOccurrenceDate <= :horizon")
    List<RecurringBooking> findDueTemplates(@Param("horizon") LocalDate horizon);
}
