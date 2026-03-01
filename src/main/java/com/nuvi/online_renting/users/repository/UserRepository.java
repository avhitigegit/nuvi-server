package com.nuvi.online_renting.users.repository;

import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Filter by name, role, enabled with pagination
    @Query("SELECT u FROM User u WHERE " +
            "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> filterUsers(@Param("name") String name,
                           @Param("role") Role role,
                           @Param("enabled") Boolean enabled,
                           Pageable pageable);
}
