package com.nuvi.online_renting.coupons.dto;

import com.nuvi.online_renting.common.enums.DiscountType;

public class ValidateCouponResponseDTO {

    private String code;
    private DiscountType discountType;
    private double discountValue;
    private double originalAmount;
    private double discountAmount;
    private double finalAmount;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(double originalAmount) { this.originalAmount = originalAmount; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(double finalAmount) { this.finalAmount = finalAmount; }
}
