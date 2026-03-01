package com.nuvi.online_renting.item.repository;

import com.nuvi.online_renting.item.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findBySellerId(Long sellerId);

    List<Item> findByAvailableTrue();
}
