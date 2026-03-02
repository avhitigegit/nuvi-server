package com.nuvi.online_renting.auth.repository;

import com.nuvi.online_renting.auth.model.RefreshToken;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Used on logout and on new login to remove the old token
    @Modifying
    int deleteByUser(User user);
}
