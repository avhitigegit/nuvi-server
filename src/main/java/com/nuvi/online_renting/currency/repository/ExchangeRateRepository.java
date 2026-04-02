package com.nuvi.online_renting.currency.repository;

import com.nuvi.online_renting.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {
}
