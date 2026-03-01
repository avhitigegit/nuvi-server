package com.nuvi.online_renting.common.security;

import com.nuvi.online_renting.users.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // Convenience helpers used in services/controllers
    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // ROLE_USER / ROLE_SELLER / ROLE_ADMIN — required for hasRole() / hasAnyRole()
        // checks in SecurityConfig (e.g. .hasRole("ADMIN"))
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Permission-based authorities — used by @PreAuthorize("hasAuthority('...')")
        // on controller methods
        RolePermissionMapper.getPermissions(user.getRole())
                .stream()
                .map(p -> new SimpleGrantedAuthority(p.name()))
                .forEach(authorities::add);

        return authorities;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
