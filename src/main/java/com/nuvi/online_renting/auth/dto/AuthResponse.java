package com.nuvi.online_renting.auth.dto;

public class AuthResponse {
    private String token;
    private long expiresInMs;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, long expiresInMs, String role) {
        this.token = token;
        this.expiresInMs = expiresInMs;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
