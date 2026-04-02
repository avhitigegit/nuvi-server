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

    // ─── Auth ─────────────────────────────────────────────────────────────────
    void sendVerificationEmail(String toEmail, String userName, String verificationLink);

    void sendPasswordResetEmail(String toEmail, String userName, String resetLink);

    // ─── Booking events ───────────────────────────────────────────────────────
    void sendBookingCreatedToSeller(String sellerEmail, String sellerName,
                                    String renterName, String itemName,
                                    String startDate, String endDate);

    void sendBookingConfirmedToRenter(String renterEmail, String renterName,
                                      String itemName, String startDate, String endDate);

    void sendBookingRejectedToRenter(String renterEmail, String renterName,
                                     String itemName, String reason);

    void sendBookingCancelledToRenter(String renterEmail, String renterName, String itemName);

    void sendBookingCancelledToSeller(String sellerEmail, String sellerName,
                                      String renterName, String itemName);

    void sendBookingCompletedToRenter(String renterEmail, String renterName, String itemName);

    void sendBookingCompletedToSeller(String sellerEmail, String sellerName,
                                      String renterName, String itemName);

    // ─── Dispute events ───────────────────────────────────────────────────────
    void sendDisputeRaisedToSeller(String sellerEmail, String sellerName,
                                   String renterName, String itemName, String reason);

    void sendDisputeResolvedToRenter(String renterEmail, String renterName,
                                     String itemName, String resolutionNote);

    void sendDisputeRejectedToRenter(String renterEmail, String renterName,
                                     String itemName, String rejectionNote);

    // ─── Seller application events ────────────────────────────────────────────
    void sendSellerApplicationApproved(String toEmail, String userName);

    void sendSellerApplicationRejected(String toEmail, String userName, String reason);

    // ─── Seller suspension events ─────────────────────────────────────────────
    void sendSellerSuspended(String toEmail, String userName, String reason);

    void sendSellerUnsuspended(String toEmail, String userName);

    // ─── KYC events ──────────────────────────────────────────────────────────
    void sendKycApproved(String toEmail, String userName);

    void sendKycRejected(String toEmail, String userName, String reason);
}
