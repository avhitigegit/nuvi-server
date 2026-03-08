package com.nuvi.online_renting.auth.service;

import com.nuvi.online_renting.auth.model.PasswordResetToken;
import com.nuvi.online_renting.auth.repository.PasswordResetTokenRepository;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.users.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    @Value("${app.password-reset.expiry-ms}")
    private long expiryMs;

    private final PasswordResetTokenRepository tokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Creates a new reset token for the user.
     * Any existing token for the same user is deleted first.
     */
    @Transactional
    public PasswordResetToken createToken(User user) {
        tokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiryDate(Instant.now().plusMillis(expiryMs));

        return tokenRepository.save(resetToken);
    }

    /**
     * Finds a reset token by its string value.
     * Throws 404 if not found — invalid or already used.
     */
    public PasswordResetToken findByToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invalid or expired password reset link. Please request a new one."));
    }

    /**
     * Verifies the token has not expired. Deletes it if expired.
     */
    @Transactional
    public void verifyExpiration(PasswordResetToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new BadRequestException(
                    "Password reset link has expired (valid for 15 minutes). Please request a new one.");
        }
    }

    /**
     * Deletes the token after successful password reset — one-time use.
     */
    @Transactional
    public void deleteToken(PasswordResetToken token) {
        tokenRepository.delete(token);
    }
}
