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

    @Override
    public void sendBookingCreatedToSeller(String sellerEmail, String sellerName,
                                           String renterName, String itemName,
                                           String startDate, String endDate) {
        send(sellerEmail, "New Booking Request — " + itemName,
                notification(sellerName,
                        "You have a new booking request for <strong>" + itemName + "</strong>.",
                        "Renter: " + renterName + "<br/>Dates: " + startDate + " → " + endDate +
                        "<br/><br/>Please log in to confirm or reject this request."));
    }

    @Override
    public void sendBookingConfirmedToRenter(String renterEmail, String renterName,
                                             String itemName, String startDate, String endDate) {
        send(renterEmail, "Booking Confirmed — " + itemName,
                notification(renterName,
                        "Great news! Your booking for <strong>" + itemName + "</strong> has been confirmed.",
                        "Dates: " + startDate + " → " + endDate));
    }

    @Override
    public void sendBookingRejectedToRenter(String renterEmail, String renterName,
                                            String itemName, String reason) {
        send(renterEmail, "Booking Not Accepted — " + itemName,
                notification(renterName,
                        "Unfortunately your booking request for <strong>" + itemName + "</strong> was not accepted.",
                        "Reason: " + (reason != null ? reason : "No reason provided.")));
    }

    @Override
    public void sendBookingCancelledToRenter(String renterEmail, String renterName, String itemName) {
        send(renterEmail, "Booking Cancelled — " + itemName,
                notification(renterName,
                        "Your booking for <strong>" + itemName + "</strong> has been cancelled.", ""));
    }

    @Override
    public void sendBookingCancelledToSeller(String sellerEmail, String sellerName,
                                             String renterName, String itemName) {
        send(sellerEmail, "Booking Cancelled — " + itemName,
                notification(sellerName,
                        "A booking for your item <strong>" + itemName + "</strong> has been cancelled.",
                        "Renter: " + renterName));
    }

    @Override
    public void sendBookingCompletedToRenter(String renterEmail, String renterName, String itemName) {
        send(renterEmail, "Rental Complete — " + itemName,
                notification(renterName,
                        "Your rental of <strong>" + itemName + "</strong> has been marked as completed.",
                        "We hope you had a great experience! Feel free to leave a review."));
    }

    @Override
    public void sendBookingCompletedToSeller(String sellerEmail, String sellerName,
                                             String renterName, String itemName) {
        send(sellerEmail, "Rental Complete — " + itemName,
                notification(sellerName,
                        "The rental of <strong>" + itemName + "</strong> has been completed.",
                        "Renter: " + renterName));
    }

    @Override
    public void sendDisputeRaisedToSeller(String sellerEmail, String sellerName,
                                          String renterName, String itemName, String reason) {
        send(sellerEmail, "Dispute Raised — " + itemName,
                notification(sellerName,
                        "A dispute has been raised regarding your item <strong>" + itemName + "</strong>.",
                        "Renter: " + renterName + "<br/>Reason: " + reason +
                        "<br/><br/>Our team will review this and contact you if needed."));
    }

    @Override
    public void sendDisputeResolvedToRenter(String renterEmail, String renterName,
                                            String itemName, String resolutionNote) {
        send(renterEmail, "Dispute Resolved — " + itemName,
                notification(renterName,
                        "Your dispute regarding <strong>" + itemName + "</strong> has been resolved.",
                        "Resolution: " + resolutionNote));
    }

    @Override
    public void sendDisputeRejectedToRenter(String renterEmail, String renterName,
                                            String itemName, String rejectionNote) {
        send(renterEmail, "Dispute Rejected — " + itemName,
                notification(renterName,
                        "Your dispute regarding <strong>" + itemName + "</strong> has been reviewed and rejected.",
                        "Note: " + rejectionNote));
    }

    @Override
    public void sendSellerApplicationApproved(String toEmail, String userName) {
        send(toEmail, "Seller Application Approved",
                notification(userName,
                        "Congratulations! Your seller application has been approved.",
                        "You can now list items on the NUVI platform."));
    }

    @Override
    public void sendSellerApplicationRejected(String toEmail, String userName, String reason) {
        send(toEmail, "Seller Application Update",
                notification(userName,
                        "Unfortunately, your seller application was not approved at this time.",
                        "Reason: " + (reason != null ? reason : "No reason provided.")));
    }

    @Override
    public void sendSellerSuspended(String toEmail, String userName, String reason) {
        send(toEmail, "Account Suspension Notice",
                notification(userName,
                        "Your seller account has been suspended.",
                        "Reason: " + (reason != null ? reason : "Policy violation.") +
                        "<br/><br/>Please contact support if you believe this is an error."));
    }

    @Override
    public void sendSellerUnsuspended(String toEmail, String userName) {
        send(toEmail, "Account Suspension Lifted",
                notification(userName,
                        "Your seller account suspension has been lifted.",
                        "You can now list and manage items on the NUVI platform again."));
    }

    @Override
    public void sendKycApproved(String toEmail, String userName) {
        send(toEmail, "KYC Verification Approved",
                notification(userName,
                        "Your identity (KYC) has been verified successfully.",
                        "You can now list items on the NUVI platform."));
    }

    @Override
    public void sendKycRejected(String toEmail, String userName, String reason) {
        send(toEmail, "KYC Verification Update",
                notification(userName,
                        "Your KYC submission could not be approved.",
                        "Reason: " + (reason != null ? reason : "Document could not be verified.") +
                        "<br/><br/>Please resubmit with a clearer document."));
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

    private String notification(String userName, String headline, String body) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>Hi %s,</h2>
                  <p>%s</p>
                  <p>%s</p>
                  <hr/>
                  <p style="font-size:12px;color:#888;">NUVI Online Renting Platform</p>
                </body>
                </html>
                """.formatted(userName, headline, body);
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
