package com.nuvi.online_renting.auth.service;

import com.nuvi.online_renting.auth.model.RefreshToken;
import com.nuvi.online_renting.auth.repository.RefreshTokenRepository;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.users.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for the user.
     * Any existing refresh token for the same user is deleted first
     * so only one active refresh token exists per user at a time.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Remove any existing token for this user
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiryMs));

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Looks up a refresh token by its string value.
     * Throws 404 if not found — meaning it was already used, deleted on logout, or never existed.
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Refresh token not found. It may have been used already or you have been logged out."));
    }

    /**
     * Verifies the refresh token has not expired.
     * If expired, it is deleted from DB and a 400 is thrown so the user must log in again.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    /**
     * Deletes the refresh token for a user — called on logout.
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }
}
