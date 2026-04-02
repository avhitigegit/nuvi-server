package com.nuvi.online_renting.coupons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ValidateCouponRequestDTO {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @Positive(message = "Booking amount must be positive")
    private double bookingAmount;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public double getBookingAmount() { return bookingAmount; }
    public void setBookingAmount(double bookingAmount) { this.bookingAmount = bookingAmount; }
}
