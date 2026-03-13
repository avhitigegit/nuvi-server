package com.nuvi.online_renting.auth.controller;

import com.nuvi.online_renting.auth.dto.PhoneOtpVerifyRequest;
import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.sms.SmsService;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth/phone")
@Tag(name = "Phone Verification", description = "Send and verify a 6-digit OTP to confirm the user's phone number. " +
        "Requires a valid JWT (logged-in user). The OTP is valid for 10 minutes.")
public class PhoneVerificationController {

    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationController.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.sms.otp-expiry-ms:600000}")
    private long otpExpiryMs;

    private final SmsService smsService;
    private final UserRepository userRepository;
    private final AuthenticationFacade authFacade;

    public PhoneVerificationController(SmsService smsService,
                                       UserRepository userRepository,
                                       AuthenticationFacade authFacade) {
        this.smsService = smsService;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
    }

    @Operation(
            summary = "Send phone OTP",
            description = "Generates a 6-digit OTP and sends it to the phone number on the user's account. " +
                    "The OTP is valid for 10 minutes. Call this endpoint first, then verify with POST /verify-otp. " +
                    "Returns an error if the phone number is already verified."
    )
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp() {
        User user = authFacade.getCurrentUser();

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BadRequestException("No phone number found on your account. Please update your profile first.");
        }

        if (user.isPhoneVerified()) {
            throw new BadRequestException("Your phone number is already verified.");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setPhoneOtp(otp);
        user.setPhoneOtpExpiry(LocalDateTime.now().plusSeconds(otpExpiryMs / 1000));
        userRepository.save(user);

        smsService.sendOtp(user.getPhone(), otp);
        log.info("Phone OTP sent to user {} (phone: {})", user.getId(), user.getPhone());

        return ResponseEntity.ok(new ApiResponse<>(true,
                "OTP sent to " + maskPhone(user.getPhone()) + ". Valid for 10 minutes.", null));
    }

    @Operation(
            summary = "Verify phone OTP",
            description = "Validates the 6-digit OTP sent to the user's phone. " +
                    "On success, the phone number is marked as verified. " +
                    "The OTP is single-use — it is cleared after a successful verification."
    )
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody PhoneOtpVerifyRequest req) {
        User user = authFacade.getCurrentUser();

        if (user.isPhoneVerified()) {
            throw new BadRequestException("Your phone number is already verified.");
        }

        if (user.getPhoneOtp() == null || user.getPhoneOtpExpiry() == null) {
            throw new BadRequestException("No OTP found. Please request a new one via POST /api/auth/phone/send-otp.");
        }

        if (user.getPhoneOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!user.getPhoneOtp().equals(req.getOtp())) {
            throw new BadRequestException("Invalid OTP. Please check the code and try again.");
        }

        user.setPhoneVerified(true);
        user.setPhoneOtp(null);
        user.setPhoneOtpExpiry(null);
        userRepository.save(user);

        log.info("Phone verified for user {}", user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Phone number verified successfully.", null));
    }

    @Operation(
            summary = "Resend phone OTP",
            description = "Generates and sends a fresh OTP, replacing any previously issued one. " +
                    "Use this if the previous OTP expired. Returns an error if the phone is already verified."
    )
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp() {
        User user = authFacade.getCurrentUser();

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BadRequestException("No phone number found on your account. Please update your profile first.");
        }

        if (user.isPhoneVerified()) {
            throw new BadRequestException("Your phone number is already verified.");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setPhoneOtp(otp);
        user.setPhoneOtpExpiry(LocalDateTime.now().plusSeconds(otpExpiryMs / 1000));
        userRepository.save(user);

        smsService.sendOtp(user.getPhone(), otp);
        log.info("Phone OTP resent to user {} (phone: {})", user.getId(), user.getPhone());

        return ResponseEntity.ok(new ApiResponse<>(true,
                "A new OTP has been sent to " + maskPhone(user.getPhone()) + ". Valid for 10 minutes.", null));
    }

    /** Shows only last 3 digits: *******567 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 3) return "***";
        return "*".repeat(phone.length() - 3) + phone.substring(phone.length() - 3);
    }
}
