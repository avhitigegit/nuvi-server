package com.nuvi.online_renting.common.sms;

public interface SmsService {

    /**
     * Sends a one-time password to the given phone number.
     *
     * @param phoneNumber E.164 format recommended (e.g. +94771234567)
     * @param otp         The 6-digit code to deliver
     */
    void sendOtp(String phoneNumber, String otp);
}
