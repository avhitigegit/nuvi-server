package com.nuvi.online_renting.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    // Allowed origins are configured per environment in application-{profile}.properties
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow frontend origins defined in the active profile
        config.setAllowedOrigins(allowedOrigins);

        // Standard HTTP methods used by the REST API
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow Authorization header for JWT and Content-Type for JSON/multipart
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Expose Authorization header to the frontend if needed
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies/credentials if needed in future (e.g. refresh token cookie)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour to reduce OPTIONS requests
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
