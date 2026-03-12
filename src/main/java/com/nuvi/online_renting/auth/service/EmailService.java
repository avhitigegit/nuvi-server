package com.nuvi.online_renting.auth.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String userName, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Verify Your NUVI Account");
            helper.setText(buildVerificationEmailBody(userName, verificationLink), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email. Please try again later.", e);
        }
    }

    private String buildVerificationEmailBody(String userName, String verificationLink) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>Hi %s,</h2>
                  <p>Thank you for registering with NUVI!</p>
                  <p>Please verify your email address by clicking the button below. This link is valid for <strong>24 hours</strong>.</p>
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

    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Reset Your NUVI Password");
            helper.setText(buildEmailBody(userName, resetLink), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email. Please try again later.", e);
        }
    }

    private String buildEmailBody(String userName, String resetLink) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>Hi %s,</h2>
                  <p>We received a request to reset your NUVI account password.</p>
                  <p>Click the button below to reset it. This link is valid for <strong>15 minutes</strong>.</p>
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
