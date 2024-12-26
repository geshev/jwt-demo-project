package com.example.demo.data.security;

import io.jsonwebtoken.Claims;

import static com.example.demo.utils.security.SecurityConstants.ACCESS_TYPE_CLAIM;
import static com.example.demo.utils.security.SecurityConstants.REFRESH_TYPE_CLAIM;

public record JwtToken(String token, Claims claims) {

    public String subject() {
        return claims().getSubject();
    }

    public boolean isAccessToken() {
        return claims().containsKey(ACCESS_TYPE_CLAIM);
    }

    public boolean isRefreshToken() {
        return claims().containsKey(REFRESH_TYPE_CLAIM);
    }
}
