package com.nuvi.online_renting.currency.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Stores the exchange rate from the base currency (LKR) to other currencies.
 * rateFromBase = how many units of this currency equal 1 LKR.
 * Example: if 1 LKR = 0.0031 USD, store currencyCode=USD, rateFromBase=0.0031.
 */
@Entity
@Table(name = "exchange_rates")
@EntityListeners(AuditingEntityListener.class)
public class ExchangeRate {

    @Id
    @Column(length = 3)
    private String currencyCode; // ISO 4217, e.g. USD, EUR, GBP

    @Column(nullable = false)
    private double rateFromBase; // 1 LKR = rateFromBase <currencyCode>

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public double getRateFromBase() { return rateFromBase; }
    public void setRateFromBase(double rateFromBase) { this.rateFromBase = rateFromBase; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
