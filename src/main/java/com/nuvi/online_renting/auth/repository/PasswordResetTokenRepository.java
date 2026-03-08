package com.nuvi.online_renting.auth.repository;

import com.nuvi.online_renting.auth.model.PasswordResetToken;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}
