package com.nuvi.online_renting.currency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ExchangeRateRequestDTO {

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters (ISO 4217)")
    private String currencyCode;

    @Positive(message = "Rate must be positive")
    private double rateFromBase;

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public double getRateFromBase() { return rateFromBase; }
    public void setRateFromBase(double rateFromBase) { this.rateFromBase = rateFromBase; }
}
