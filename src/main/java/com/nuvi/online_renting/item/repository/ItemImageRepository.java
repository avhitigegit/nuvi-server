package com.nuvi.online_renting.item.repository;

import com.nuvi.online_renting.item.model.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    List<ItemImage> findByItemIdOrderByDisplayOrderAscCreatedAtAsc(Long itemId);

    Optional<ItemImage> findByIdAndItemId(Long id, Long itemId);

    long countByItemId(Long itemId);
}
