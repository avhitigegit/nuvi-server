package com.nuvi.online_renting.item.repository;

import com.nuvi.online_renting.item.model.ItemBlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemBlockedDateRepository extends JpaRepository<ItemBlockedDate, Long> {

    List<ItemBlockedDate> findByItemIdOrderByStartDateAsc(Long itemId);

    @Query("SELECT COUNT(b) > 0 FROM ItemBlockedDate b " +
           "WHERE b.item.id = :itemId " +
           "AND (b.startDate <= :endDate AND b.endDate >= :startDate)")
    boolean existsOverlappingBlock(@Param("itemId") Long itemId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}
