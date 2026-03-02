package com.nuvi.online_renting.auth.controller;

import com.nuvi.online_renting.auth.dto.AuthRequest;
import com.nuvi.online_renting.auth.dto.AuthResponse;
import com.nuvi.online_renting.auth.dto.RefreshTokenRequest;
import com.nuvi.online_renting.auth.dto.RegisterRequest;
import com.nuvi.online_renting.auth.model.RefreshToken;
import com.nuvi.online_renting.auth.service.CustomUserDetailsService;
import com.nuvi.online_renting.auth.service.RefreshTokenService;
import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.security.CustomUserDetails;
import com.nuvi.online_renting.common.security.JwtTokenService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints to register, log in, refresh access tokens, and log out.")
public class AuthController {

    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationFacade authFacade;

    public AuthController(AuthenticationManager authManager,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          JwtTokenService jwtTokenService,
                          CustomUserDetailsService userDetailsService,
                          RefreshTokenService refreshTokenService,
                          AuthenticationFacade authFacade) {
        this.authManager = authManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.authFacade = authFacade;
    }

    @Operation(
            summary = "Register a new user account",
            description = "Creates a new account with the USER role. After registration, log in to get tokens. " +
                          "To become a SELLER, submit a seller application via POST /api/sellers/apply after logging in."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email is already registered. Please use a different email or log in.");
        }
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(Role.USER);
        u.setPhone(req.getPhone());
        u.setEnabled(true);
        userRepository.save(u);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration successful. You can now log in.", null));
    }

    @Operation(
            summary = "Login",
            description = "Authenticate with email and password. Returns a short-lived JWT access token (24h) " +
                          "and a long-lived refresh token (7 days). " +
                          "Use the access token in the Authorization header as: Bearer <accessToken>. " +
                          "When the access token expires, call POST /api/auth/refresh with the refresh token to get a new one."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(req.getEmail());
        String accessToken = jwtTokenService.generateToken(userDetails);

        // Create (or replace) the refresh token for this user
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());

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
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out successfully. Refresh token has been invalidated.", null));
    }
}
