package com.nuvi.online_renting.common.security;

import com.nuvi.online_renting.users.repository.UserRepository;
import com.nuvi.online_renting.users.model.User;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@ComponentScan
public class AuthenticationFacade {
    private final UserRepository userRepository;

    public AuthenticationFacade(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public User getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null; // or throw custom exception
        }
        String email = authentication.getName(); // username is email
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}