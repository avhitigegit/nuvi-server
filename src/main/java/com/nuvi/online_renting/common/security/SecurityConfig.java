package com.nuvi.online_renting.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize on methods
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final Environment environment;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, Environment environment) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ─── Security Headers ─────────────────────────────────────────
                // Block lambda used intentionally — avoids method-chaining ambiguity
                // introduced in Spring Security 6.x where permissionsPolicy() returns
                // its own config type rather than HeadersConfigurer.
                .headers(headers -> {

                    // Prevents browsers from MIME-sniffing a response away from the declared
                    // content-type. Stops browsers treating a plain-text file as executable HTML/JS.
                    headers.contentTypeOptions(Customizer.withDefaults());

                    // Blocks the page from being embedded in an <iframe> — prevents clickjacking.
                    headers.frameOptions(frame -> frame.deny());

                    // Disables the legacy XSS auditor. Modern browsers ignore it and leaving it
                    // enabled can actually introduce new vulnerabilities.
                    headers.xssProtection(xss -> xss
                            .headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED));

                    // Tells browsers not to send a Referrer header when navigating to a different
                    // origin — prevents leaking internal API paths in third-party request logs.
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

                    // Restricts which browser features the frontend can request.
                    // Disables camera, microphone, geolocation and payment — none needed for a rental API.
                    headers.permissionsPolicy(permissions -> permissions
                            .policy("camera=(), microphone=(), geolocation=(), payment=()"));

                    // Content-Security-Policy — restricts resource loading to same origin only.
                    // Adjust default-src when a frontend CDN domain is added.
                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives(
                                    "default-src 'self'; " +
                                    "frame-ancestors 'none'; " +
                                    "object-src 'none'"));

                    // HTTP Strict Transport Security — active in prod only.
                    // Tells browsers to always use HTTPS for 1 year.
                    // includeSubDomains covers api., www., and any other subdomains.
                    if (isProd) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000) // 1 year
                                .preload(true));
                    } else {
                        headers.httpStrictTransportSecurity(hsts -> hsts.disable());
                    }
                })

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                                         "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                         "/api/v1/auth/verify-email", "/api/v1/auth/resend-verification").permitAll()
                        .requestMatchers("/actuator/health").hasRole("ADMIN")
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/items/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sellers/apply").hasAnyRole("USER","SELLER")
                        .requestMatchers("/api/v1/sellers/**").hasAnyRole("USER","SELLER","ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Enforce HTTPS in production — HTTP requests are blocked at the channel level
        if (isProd) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
