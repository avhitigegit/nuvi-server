package com.nuvi.online_renting.auth.service;

/**
 * Abstraction over the transactional email provider.
 *
 * Two implementations are provided:
 *   SmtpEmailService  — JavaMail / Gmail SMTP (dev, app.email.provider=smtp)
 *   AwsSesEmailService — AWS SES v2           (prod, app.email.provider=ses)
 *
 * Switch via application.properties:
 *   app.email.provider=smtp   (default — uses spring.mail.* SMTP config)
 *   app.email.provider=ses    (production — uses AWS SES, requires SES_FROM_EMAIL env var)
 */
public interface EmailService {

    void sendVerificationEmail(String toEmail, String userName, String verificationLink);

    void sendPasswordResetEmail(String toEmail, String userName, String resetLink);
}
