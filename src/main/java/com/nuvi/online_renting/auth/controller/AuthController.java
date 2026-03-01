package com.nuvi.online_renting.auth.controller;

import com.nuvi.online_renting.auth.dto.AuthRequest;
import com.nuvi.online_renting.auth.dto.AuthResponse;
import com.nuvi.online_renting.auth.dto.RegisterRequest;
import com.nuvi.online_renting.auth.service.CustomUserDetailsService;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.security.CustomUserDetails;
import com.nuvi.online_renting.common.security.JwtTokenService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints to register a new account and log in to receive a JWT token for accessing secured APIs.")
public class AuthController {

    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authManager,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          JwtTokenService jwtTokenService,
                          CustomUserDetailsService userDetailsService) {
        this.authManager = authManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Operation(
            summary = "Register a new user account",
            description = "Creates a new account with the USER role. After registration, log in to get a JWT token. " +
                          "To become a SELLER, submit a seller application via POST /api/sellers/apply after logging in."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Email already in use or invalid request body")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(Role.USER);
        u.setPhone(req.getPhone());
        u.setEnabled(true);
        userRepository.save(u);
        return ResponseEntity.ok("Registered");
    }

    @Operation(
            summary = "Login and get a JWT token",
            description = "Authenticate with your email and password. Returns a JWT bearer token, its expiry in milliseconds, " +
                          "and your role (USER / SELLER / ADMIN). Use the token in the Authorization header as: Bearer <token>."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtTokenService.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponse(
                token,
                jwtTokenService.getExpirationMs(),
                userDetails.getUser().getRole().name()   // "USER" / "SELLER" / "ADMIN" — clean
        ));
    }

}