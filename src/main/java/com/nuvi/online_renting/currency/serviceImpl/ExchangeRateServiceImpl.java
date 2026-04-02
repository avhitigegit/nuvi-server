package com.nuvi.online_renting.currency.serviceImpl;

import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.currency.dto.ExchangeRateRequestDTO;
import com.nuvi.online_renting.currency.dto.ExchangeRateResponseDTO;
import com.nuvi.online_renting.currency.model.ExchangeRate;
import com.nuvi.online_renting.currency.repository.ExchangeRateRepository;
import com.nuvi.online_renting.currency.service.ExchangeRateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateServiceImpl(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDTO> getAllRates() {
        return exchangeRateRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public ExchangeRateResponseDTO upsertRate(ExchangeRateRequestDTO request) {
        String code = request.getCurrencyCode().toUpperCase();
        ExchangeRate rate = exchangeRateRepository.findById(code)
                .orElseGet(() -> {
                    ExchangeRate r = new ExchangeRate();
                    r.setCurrencyCode(code);
                    return r;
                });
        rate.setRateFromBase(request.getRateFromBase());
        return toDTO(exchangeRateRepository.save(rate));
    }

    @Override
    @Transactional
    public void deleteRate(String currencyCode) {
        String code = currencyCode.toUpperCase();
        if (!exchangeRateRepository.existsById(code)) {
            throw new ResourceNotFoundException("Currency not found: " + code);
        }
        exchangeRateRepository.deleteById(code);
    }

    @Override
    @Transactional(readOnly = true)
    public double convert(double amountInLkr, String targetCurrency) {
        if (targetCurrency == null || targetCurrency.equalsIgnoreCase("LKR")) {
            return amountInLkr;
        }
        return exchangeRateRepository.findById(targetCurrency.toUpperCase())
                .map(rate -> Math.round(amountInLkr * rate.getRateFromBase() * 100.0) / 100.0)
                .orElse(amountInLkr); // fall back to LKR if rate not found
    }

    private ExchangeRateResponseDTO toDTO(ExchangeRate rate) {
        ExchangeRateResponseDTO dto = new ExchangeRateResponseDTO();
        dto.setCurrencyCode(rate.getCurrencyCode());
        dto.setRateFromBase(rate.getRateFromBase());
        dto.setUpdatedAt(rate.getUpdatedAt());
        return dto;
    }
}
