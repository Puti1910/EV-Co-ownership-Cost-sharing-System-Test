package com.example.ui_service.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple authenticated user representation used as the principal that is stored
 * in the {@link org.springframework.security.core.context.SecurityContext}.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long userId,
                             String email,
                             String role,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

