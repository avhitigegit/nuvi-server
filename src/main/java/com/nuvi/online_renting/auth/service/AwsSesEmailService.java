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
