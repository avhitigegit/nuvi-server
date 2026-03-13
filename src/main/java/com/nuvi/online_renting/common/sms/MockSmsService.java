package com.nuvi.online_renting.common.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock SMS provider — active when app.sms.provider=mock (default).
 * Prints the OTP to the application log instead of sending a real SMS.
 * Safe for local development and CI; zero external dependencies and zero cost.
 *
 * To switch to real SMS delivery, set app.sms.provider=sns in application.properties
 * and ensure AWS SNS credentials are configured.
 */
@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(MockSmsService.class);

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        log.info("[MOCK SMS] To: {} — OTP: {}", phoneNumber, otp);
    }
}
