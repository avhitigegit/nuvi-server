package com.nuvi.online_renting.bookings.repository;

import com.nuvi.online_renting.bookings.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Exclude CANCELLED and COMPLETED — completed means item was returned
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.item.id = :itemId " +
           "AND b.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "AND (b.startDate <= :endDate AND b.endDate >= :startDate)")
    boolean existsOverlappingBooking(@Param("itemId") Long itemId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:userId IS NULL OR b.user.id = :userId)")
    Page<Booking> filterBookings(@Param("status") String status,
                                 @Param("userId") Long userId,
                                 Pageable pageable);

    // Find active (PENDING or CONFIRMED) bookings for a specific item — for sellers
    @Query("SELECT b FROM Booking b WHERE b.item.id = :itemId " +
           "AND b.status NOT IN ('CANCELLED', 'COMPLETED')")
    Page<Booking> findActiveByItemId(@Param("itemId") Long itemId, Pageable pageable);

    // ─── Dashboard aggregate queries ─────────────────────────────────────────

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.item.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.item.seller.id = :sellerId AND b.status = :status")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") String status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.item.seller.id = :sellerId AND b.createdAt >= :from")
    long countBySellerIdSince(@Param("sellerId") Long sellerId, @Param("from") LocalDateTime from);

    // Earnings: sum of pricePerDay * (endDate - startDate) for COMPLETED bookings
    @Query("SELECT COALESCE(SUM(b.item.pricePerDay * (FUNCTION('DATEDIFF', b.endDate, b.startDate))), 0) " +
           "FROM Booking b WHERE b.item.seller.id = :sellerId AND b.status = 'COMPLETED'")
    double sumEarningsBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(SUM(b.item.pricePerDay * (FUNCTION('DATEDIFF', b.endDate, b.startDate))), 0) " +
           "FROM Booking b WHERE b.item.seller.id = :sellerId AND b.status = 'COMPLETED' AND b.updatedAt >= :from")
    double sumEarningsBySellerIdSince(@Param("sellerId") Long sellerId, @Param("from") LocalDateTime from);

    // Top 5 items by completed booking count for a seller
    @Query("SELECT b.item.id, b.item.name, COUNT(b), " +
           "COALESCE(SUM(b.item.pricePerDay * (FUNCTION('DATEDIFF', b.endDate, b.startDate))), 0) " +
           "FROM Booking b WHERE b.item.seller.id = :sellerId AND b.status = 'COMPLETED' " +
           "GROUP BY b.item.id, b.item.name ORDER BY COUNT(b) DESC")
    List<Object[]> findTopItemsBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
