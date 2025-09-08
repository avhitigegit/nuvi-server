package com.nuvi.online_renting.auth.controller;

import com.nuvi.online_renting.auth.dto.AuthRequest;
import com.nuvi.online_renting.auth.dto.AuthResponse;
import com.nuvi.online_renting.auth.dto.RegisterRequest;
import com.nuvi.online_renting.auth.service.CustomUserDetailsService;
import com.nuvi.online_renting.common.dto.Role;
import com.nuvi.online_renting.config.security.JwtTokenService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
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
        u.setEnabled(true);
        userRepository.save(u);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserDetails user = userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtTokenService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, jwtTokenService.getExpirationMs(),
                user.getAuthorities().iterator().next().getAuthority()));
    }
}