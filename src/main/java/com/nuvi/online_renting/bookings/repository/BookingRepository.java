package com.nuvi.online_renting.bookings.repository;

import com.nuvi.online_renting.bookings.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'CONFIRMED' " +
            "AND ( (b.startDate <= :endDate AND b.endDate >= :startDate) )")
    boolean existsOverlappingBooking(@Param("itemId") Long itemId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

}