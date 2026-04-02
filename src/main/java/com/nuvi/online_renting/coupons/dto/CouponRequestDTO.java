package com.nuvi.online_renting.coupons.dto;

import com.nuvi.online_renting.common.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CouponRequestDTO {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_\\-]+$", message = "Code must contain only uppercase letters, digits, hyphens, or underscores")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @Positive(message = "Discount value must be positive")
    private double discountValue;

    @PositiveOrZero(message = "Minimum booking amount cannot be negative")
    private double minBookingAmount = 0;

    // 0 = unlimited
    @PositiveOrZero(message = "Max uses cannot be negative")
    private int maxUses = 0;

    private LocalDateTime expiryDate;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMinBookingAmount() { return minBookingAmount; }
    public void setMinBookingAmount(double minBookingAmount) { this.minBookingAmount = minBookingAmount; }

    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}
