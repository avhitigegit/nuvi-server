package com.nuvi.online_renting.common.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * AWS SNS SMS provider — active when app.sms.provider=sns.
 * Requires: app.sms.provider=sns in application.properties
 *           plus valid AWS credentials (aws.access-key / aws.secret-key / aws.region).
 *
 * SNS pricing: ~$0.02–$0.04 per SMS depending on destination country.
 * Enable "SMS sandbox" in your AWS account during testing to avoid charges.
 */
@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "sns")
public class AwsSnsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(AwsSnsService.class);

    private final SnsClient snsClient;

    public AwsSnsService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(phoneNumber)
                    .message("Your NUVI verification code is: " + otp + ". Valid for 10 minutes. Do not share this code.")
                    .build();
            snsClient.publish(request);
            log.info("[SNS SMS] OTP sent to {}", phoneNumber);
        } catch (SnsException e) {
            log.error("[SNS SMS] Failed to send OTP to {}: {}", phoneNumber, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to send verification SMS. Please try again later.", e);
        }
    }
}
