package com.nuvi.online_renting.auth.repository;

import com.nuvi.online_renting.auth.model.RefreshToken;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Used on logout and on new login to remove the old token.
    // @Query executes a single bulk DELETE immediately (instead of load-then-remove),
    // and clearAutomatically clears the stale persistence context so the subsequent
    // INSERT does not hit the unique constraint on user_id.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteByUser(@Param("user") User user);
}
