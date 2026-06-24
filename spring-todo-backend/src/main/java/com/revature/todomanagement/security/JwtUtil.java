package com.revature.todomanagement.security;

import com.revature.todomanagement.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT.
     * Subject = username, userId claim = user's UUID, expiry = 24 hours.
     */
    public String generateToken(User user) {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + 86_400_000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts the username (subject) from a token.
     * Returns null on any parse failure — never throws.
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts the userId claim from a token.
     * Returns null on any parse failure — never throws.
     */
    public String extractUserId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("userId", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns true iff the token is valid, non-expired, and the subject matches the given username.
     */
    public boolean isTokenValid(String token, String username) {
        String extracted = extractUsername(token);
        return username != null && username.equals(extracted);
    }
}
