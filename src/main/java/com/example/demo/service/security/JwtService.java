package com.example.demo.service.security;

import com.example.demo.data.security.JwtToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static com.example.demo.utils.security.SecurityConstants.*;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;
    private final long refreshExpiration;

    JwtService(@Value("${security.jwt.secret-key}") String secretKey,
               @Value("${security.jwt.expiration}") long expiration,
               @Value("${security.jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(Map<String, ?> claims, String username) {
        return generateToken(claims, ACCESS_TYPE_CLAIM, username, expiration);
    }

    public String generateRefreshToken(Map<String, ?> claims, String username) {
        return generateToken(claims, REFRESH_TYPE_CLAIM, username, refreshExpiration);
    }

    private String generateToken(Map<String, ?> claims, String type, String username, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .claim(type, TYPE_CLAIM_VALUE)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public JwtToken validateAndGetToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtToken(token, claims);
    }
}
