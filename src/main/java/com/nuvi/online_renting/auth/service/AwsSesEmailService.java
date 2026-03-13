package com.nuvi.online_renting.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * AWS SES v2 email provider — active when app.email.provider=ses.
 *
 * Replaces Gmail SMTP for production:
 *   - No daily sending limits (Gmail caps at ~500/day)
 *   - Better deliverability and bounce handling
 *   - Cost: ~$0.10 per 1,000 emails
 *
 * Requirements before enabling in production:
 *   1. Verify your sending domain in AWS SES console (adds DNS TXT/CNAME records)
 *   2. Request production access to exit the SES sandbox
 *      (sandbox only allows sending to verified addresses — not suitable for real users)
 *   3. Set environment variable: SES_FROM_EMAIL=noreply@yourdomain.com
 *   4. Set app.email.provider=ses in application-prod.properties
 *
 * AWS setup: SES → Verified identities → Add domain → follow DNS instructions
 */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "ses")
public class AwsSesEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(AwsSesEmailService.class);

    @Value("${app.email.from}")
    private String fromEmail;

    private final SesV2Client sesClient;

    public AwsSesEmailService(SesV2Client sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String userName, String verificationLink) {
        send(toEmail, "Verify Your NUVI Account",
                buildVerificationBody(userName, verificationLink));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        send(toEmail, "Reset Your NUVI Password",
                buildPasswordResetBody(userName, resetLink));
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(fromEmail)
                    .destination(Destination.builder().toAddresses(to).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data(subject).charset("UTF-8").build())
                                    .body(Body.builder()
                                            .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("[SES] Email sent to {}", to);
        } catch (SesV2Exception e) {
            log.error("[SES] Failed to send email to {}: {}", to, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to send email. Please try again later.", e);
        }
    }

    private String buildVerificationBody(String userName, String verificationLink) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>Hi %s,</h2>
                  <p>Thank you for registering with NUVI!</p>
                  <p>Please verify your email address by clicking the button below.
                     This link is valid for <strong>24 hours</strong>.</p>
                  <p style="margin: 32px 0;">
                    <a href="%s"
                       style="background-color:#4F46E5;color:#fff;padding:12px 24px;
                              text-decoration:none;border-radius:6px;font-size:16px;">
                      Verify Email
                    </a>
                  </p>
                  <p>If you did not create an account, you can safely ignore this email.</p>
                  <hr/>
                  <p style="font-size:12px;color:#888;">NUVI Online Renting Platform</p>
                </body>
                </html>
                """.formatted(userName, verificationLink);
    }

    private String buildPasswordResetBody(String userName, String resetLink) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>Hi %s,</h2>
                  <p>We received a request to reset your NUVI account password.</p>
                  <p>Click the button below to reset it.
                     This link is valid for <strong>15 minutes</strong>.</p>
                  <p style="margin: 32px 0;">
                    <a href="%s"
                       style="background-color:#4F46E5;color:#fff;padding:12px 24px;
                              text-decoration:none;border-radius:6px;font-size:16px;">
                      Reset Password
                    </a>
                  </p>
                  <p>If you did not request a password reset, you can safely ignore this email.
                     Your password will not be changed.</p>
                  <hr/>
                  <p style="font-size:12px;color:#888;">NUVI Online Renting Platform</p>
                </body>
                </html>
                """.formatted(userName, resetLink);
    }
}
