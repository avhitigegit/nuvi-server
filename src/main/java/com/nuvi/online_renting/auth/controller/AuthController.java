package com.nuvi.online_renting.auth.controller;

import com.nuvi.online_renting.auth.dto.AuthRequest;
import com.nuvi.online_renting.auth.dto.AuthResponse;
import com.nuvi.online_renting.auth.dto.ForgotPasswordRequest;
import com.nuvi.online_renting.auth.dto.RefreshTokenRequest;
import com.nuvi.online_renting.auth.dto.RegisterRequest;
import com.nuvi.online_renting.auth.dto.ResetPasswordRequest;
import com.nuvi.online_renting.auth.model.PasswordResetToken;
import com.nuvi.online_renting.auth.model.RefreshToken;
import com.nuvi.online_renting.auth.service.CustomUserDetailsService;
import com.nuvi.online_renting.auth.service.EmailService;
import com.nuvi.online_renting.auth.service.PasswordResetTokenService;
import com.nuvi.online_renting.auth.service.RefreshTokenService;
import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.security.CustomUserDetails;
import com.nuvi.online_renting.common.security.JwtTokenService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints to register, log in, refresh access tokens, and log out.")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.email-verification.expiry-ms:86400000}")
    private long emailVerificationExpiryMs;

    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationFacade authFacade;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authManager,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          JwtTokenService jwtTokenService,
                          CustomUserDetailsService userDetailsService,
                          RefreshTokenService refreshTokenService,
                          AuthenticationFacade authFacade,
                          PasswordResetTokenService passwordResetTokenService,
                          EmailService emailService) {
        this.authManager = authManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.authFacade = authFacade;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
    }

    @Operation(
            summary = "Register a new user account",
            description = "Creates a new account with the USER role. A verification email is sent immediately. " +
                          "You must verify your email before you can log in. " +
                          "To become a SELLER, submit a seller application via POST /api/v1/sellers/apply after logging in."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email is already registered. Please use a different email or log in.");
        }

        String verificationToken = UUID.randomUUID().toString().replace("-", "");

        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(Role.USER);
        u.setPhone(req.getPhone());
        u.setEnabled(true);
        u.setEmailVerified(false);
        u.setEmailVerificationToken(verificationToken);
        u.setEmailVerificationTokenExpiry(LocalDateTime.now().plusSeconds(emailVerificationExpiryMs / 1000));
        // PDPA: record the exact moment the user accepted the Terms of Service.
        // Submitting the registration form constitutes acceptance.
        u.setTermsAcceptedAt(LocalDateTime.now());
        userRepository.save(u);

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
        emailService.sendVerificationEmail(u.getEmail(), u.getName(), verificationLink);

        log.info("New user registered, verification email sent: {}", req.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Registration successful. Please check your email to verify your account before logging in.", null));
    }

    @Operation(
            summary = "Verify email address",
            description = "Confirms the user's email using the token sent during registration. " +
                          "The token is valid for 24 hours. After verification, the user can log in."
    )
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid or already used verification token."));

        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification link has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "Email verified successfully. You can now log in.", null));
    }

    @Operation(
            summary = "Resend verification email",
            description = "Sends a new verification email if the account is not yet verified. " +
                          "Use this if the original link expired. The new link is valid for 24 hours."
    )
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String newToken = UUID.randomUUID().toString().replace("-", "");
                user.setEmailVerificationToken(newToken);
                user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusSeconds(emailVerificationExpiryMs / 1000));
                userRepository.save(user);

                String verificationLink = frontendUrl + "/verify-email?token=" + newToken;
                emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationLink);
                log.info("Verification email resent to: {}", email);
            }
        });
        // Always return success to avoid user enumeration
        return ResponseEntity.ok(new ApiResponse<>(true,
                "If that email is registered and unverified, a new verification link has been sent.", null));
    }

    @Operation(
            summary = "Login",
            description = "Authenticate with email and password. Returns a short-lived JWT access token (24h) " +
                          "and a long-lived refresh token (7 days). " +
                          "Email must be verified before login is allowed. " +
                          "Use the access token in the Authorization header as: Bearer <accessToken>. " +
                          "When the access token expires, call POST /api/v1/auth/refresh with the refresh token to get a new one."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(req.getEmail());

        if (!userDetails.getUser().isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Email not verified. Please check your inbox and verify your email before logging in.");
        }

        String accessToken = jwtTokenService.generateToken(userDetails);

        // Create (or replace) the refresh token for this user
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());

        log.info("User logged in: {}", req.getEmail());
        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                jwtTokenService.getExpirationMs(),
                refreshToken.getToken(),
                refreshTokenService.getRefreshTokenExpiryMs(),
                userDetails.getUser().getRole().name()
        ));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Exchange a valid refresh token for a new JWT access token. " +
                          "The refresh token remains valid for 7 days. " +
                          "If the refresh token has expired, the user must log in again. " +
                          "No Authorization header required for this endpoint."
    )
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        RefreshToken refreshToken = refreshTokenService.findByToken(req.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtTokenService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(
                newAccessToken,
                jwtTokenService.getExpirationMs(),
                refreshToken.getToken(),
                refreshTokenService.getRefreshTokenExpiryMs(),
                user.getRole().name()
        ));
    }

    @Operation(
            summary = "Logout",
            description = "Invalidates the current user's refresh token. " +
                          "After logout, the refresh token can no longer be used to get new access tokens. " +
                          "The access token will still work until it naturally expires (up to 24h), " +
                          "so the frontend should discard it immediately on logout. " +
                          "Requires a valid Authorization header."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        User currentUser = authFacade.getCurrentUser();
        refreshTokenService.deleteByUser(currentUser);
        log.info("User logged out: {}", currentUser.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out successfully. Refresh token has been invalidated.", null));
    }

    @Operation(
            summary = "Forgot password",
            description = "Sends a password reset email to the provided address if an account exists. " +
                          "The reset link is valid for 15 minutes. " +
                          "For security reasons, the response is always the same regardless of whether the email exists."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            PasswordResetToken resetToken = passwordResetTokenService.createToken(user);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);
        });
        // Always return success to avoid user enumeration
        return ResponseEntity.ok(new ApiResponse<>(true,
                "If that email is registered, a password reset link has been sent. Please check your inbox.", null));
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using the token received by email. " +
                          "The token is single-use and expires after 15 minutes. " +
                          "After a successful reset all active sessions are invalidated — " +
                          "log in again with the new password."
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        PasswordResetToken resetToken = passwordResetTokenService.findByToken(req.getToken());
        passwordResetTokenService.verifyExpiration(resetToken);

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Invalidate the reset token — one-time use
        passwordResetTokenService.deleteToken(resetToken);

        // Invalidate all active refresh tokens — forces re-login on all devices.
        // Any session using the old password (e.g. a stolen refresh token) becomes useless.
        refreshTokenService.deleteByUser(user);

        return ResponseEntity.ok(new ApiResponse<>(true,
                "Password has been reset successfully. You can now log in with your new password.", null));
    }
}
