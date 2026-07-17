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

/**
 * JWT utility for generating, parsing, and validating JSON Web Tokens.
 *
 * <p>Uses HMAC-SHA256 (HS256) signing with a secret key injected from the
 * {@code jwt.secret} application property.</p>
 *
 * <h3>Relaxed Binding (Spring Boot)</h3>
 * <p>The {@code @Value("${jwt.secret}")} annotation reads the property {@code jwt.secret}
 * from the Spring Environment. Spring Boot applies <em>relaxed binding</em> when resolving
 * properties, meaning the following sources all map to the same property:</p>
 * <ul>
 *   <li>{@code jwt.secret} in {@code application.properties}</li>
 *   <li>{@code JWT_SECRET} as an OS environment variable</li>
 *   <li>{@code jwt_secret} or {@code JWT-SECRET} (any case/separator variant)</li>
 *   <li>{@code --jwt.secret=value} as a command-line argument</li>
 * </ul>
 * <p>Resolution priority (first match wins): command-line args > environment variables >
 * application.properties > default values. In production, the environment variable
 * {@code JWT_SECRET} is set via the Docker {@code --env-file} flag, which overrides the
 * default value hardcoded in {@code application.properties}.</p>
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/features/external-config.html">
 *      Spring Boot Externalized Configuration</a>
 */
@Component
public class JwtUtil {

    private final SecretKey key;

    /**
     * Constructs a JwtUtil with the signing key derived from the configured secret.
     *
     * @param secret the HS256 secret string (minimum 32 characters for 256-bit security).
     *               Injected via Spring's {@code @Value} from property {@code jwt.secret},
     *               which is overridden by environment variable {@code JWT_SECRET} in production.
     */
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
