package com.nuvi.online_renting.auth.dto;

public class AuthResponse {

    // Short-lived JWT access token (24 hours)
    private String accessToken;
    private long accessTokenExpiresInMs;

    // Long-lived refresh token (7 days) — use to get a new access token without re-login
    private String refreshToken;
    private long refreshTokenExpiresInMs;

    private String role;

    public AuthResponse() {}

    public AuthResponse(String accessToken, long accessTokenExpiresInMs,
                        String refreshToken, long refreshTokenExpiresInMs,
                        String role) {
        this.accessToken = accessToken;
        this.accessTokenExpiresInMs = accessTokenExpiresInMs;
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresInMs = refreshTokenExpiresInMs;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public long getAccessTokenExpiresInMs() { return accessTokenExpiresInMs; }
    public void setAccessTokenExpiresInMs(long accessTokenExpiresInMs) { this.accessTokenExpiresInMs = accessTokenExpiresInMs; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getRefreshTokenExpiresInMs() { return refreshTokenExpiresInMs; }
    public void setRefreshTokenExpiresInMs(long refreshTokenExpiresInMs) { this.refreshTokenExpiresInMs = refreshTokenExpiresInMs; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
