package com.nuvi.online_renting.item.repository;

import com.nuvi.online_renting.item.model.Item;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // Pessimistic write lock — used during booking creation to prevent double bookings
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdWithLock(@Param("id") Long id);

    List<Item> findBySellerId(Long sellerId);

    List<Item> findByAvailableTrue();

    // Paginated seller items
    Page<Item> findBySellerId(Long sellerId, Pageable pageable);

    // Search with optional filters + pagination
    @Query("SELECT i FROM Item i WHERE " +
            "(:name IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:minPrice IS NULL OR i.pricePerDay >= :minPrice) AND " +
            "(:maxPrice IS NULL OR i.pricePerDay <= :maxPrice) AND " +
            "(:available IS NULL OR i.available = :available) AND " +
            "(:sellerId IS NULL OR i.seller.id = :sellerId)")
    Page<Item> searchItems(@Param("name") String name,
                           @Param("minPrice") Double minPrice,
                           @Param("maxPrice") Double maxPrice,
                           @Param("available") Boolean available,
                           @Param("sellerId") Long sellerId,
                           Pageable pageable);
}