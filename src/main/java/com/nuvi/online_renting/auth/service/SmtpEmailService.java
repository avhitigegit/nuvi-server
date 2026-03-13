package com.nuvi.online_renting.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * SMTP email provider — sends email via JavaMail (Gmail in dev).
 * Active when app.email.provider=smtp (default).
 *
 * Gmail limits: ~500 emails/day. Suitable for development and low-traffic testing only.
 * For production, switch to AwsSesEmailService (app.email.provider=ses).
 *
 * Configuration (application-dev.properties):
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=${MAIL_USERNAME}
 *   spring.mail.password=${MAIL_PASSWORD}   ← Gmail App Password, not your account password
 */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailService implements EmailService {

    @Value("${app.email.from:noreply@nuvi.com}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + to + ". Please try again later.", e);
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
