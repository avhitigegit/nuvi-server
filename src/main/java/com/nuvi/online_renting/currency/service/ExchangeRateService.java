package com.nuvi.online_renting.currency.service;

import com.nuvi.online_renting.currency.dto.ExchangeRateRequestDTO;
import com.nuvi.online_renting.currency.dto.ExchangeRateResponseDTO;

import java.util.List;

public interface ExchangeRateService {

    List<ExchangeRateResponseDTO> getAllRates();

    ExchangeRateResponseDTO upsertRate(ExchangeRateRequestDTO request);

    void deleteRate(String currencyCode);

    /**
     * Convert an amount from LKR to the given currency.
     * Returns the original amount unchanged if the currency is LKR or not found.
     */
    double convert(double amountInLkr, String targetCurrency);
}
