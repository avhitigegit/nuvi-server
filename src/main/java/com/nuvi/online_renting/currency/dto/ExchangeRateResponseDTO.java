package com.nuvi.online_renting.currency.dto;

import java.time.LocalDateTime;

public class ExchangeRateResponseDTO {

    private String currencyCode;
    private double rateFromBase; // 1 LKR = rateFromBase <currency>
    private LocalDateTime updatedAt;

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public double getRateFromBase() { return rateFromBase; }
    public void setRateFromBase(double rateFromBase) { this.rateFromBase = rateFromBase; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
