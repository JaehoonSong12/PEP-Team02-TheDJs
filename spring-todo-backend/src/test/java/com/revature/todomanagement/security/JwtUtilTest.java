package com.revature.todomanagement.security;

import com.revature.todomanagement.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil")
class JwtUtilTest {

    // Constructed directly with a known 32-byte secret
    JwtUtil jwtUtil = new JwtUtil("my-test-secret-key-32-bytes-lon!");

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("returns non-null token with 3 segments")
        void generateToken_validUser_returnsNonNullTokenWithThreeSegments() {
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");

            String token = jwtUtil.generateToken(user);

            assertNotNull(token);
            assertTrue(token.length() >= 1);
            String[] segments = token.split("\\.");
            assertEquals(3, segments.length, "JWT should have exactly 3 Base64URL-encoded segments");
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("valid token returns username")
        void extractUsername_validToken_returnsUsername() {
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");
            String token = jwtUtil.generateToken(user);

            String username = jwtUtil.extractUsername(token);

            assertEquals("testuser", username);
        }

        @Test
        @DisplayName("different secret returns null")
        void extractUsername_differentSecret_returnsNull() {
            JwtUtil otherJwtUtil = new JwtUtil("another-secret-key-32-bytes-ok!!");
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");
            String token = otherJwtUtil.generateToken(user);

            String username = jwtUtil.extractUsername(token);

            assertNull(username, "Extracting username with a different secret should return null");
        }

        @Test
        @DisplayName("expired token returns null")
        void extractUsername_expiredToken_returnsNull() {
            // Build a token that's already expired using the same key directly
            SecretKey testKey = Keys.hmacShaKeyFor("my-test-secret-key-32-bytes-lon!".getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                    .subject("testuser")
                    .claim("userId", UUID.randomUUID().toString())
                    .issuedAt(new Date(System.currentTimeMillis() - 2000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(testKey, Jwts.SIG.HS256)
                    .compact();

            String username = jwtUtil.extractUsername(expiredToken);

            assertNull(username, "Extracting username from an expired token should return null");
        }
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("valid token returns userId")
        void extractUserId_validToken_returnsUserId() {
            UUID userId = UUID.randomUUID();
            User user = new User(userId, "testuser", "Password1!");
            String token = jwtUtil.generateToken(user);

            String extractedUserId = jwtUtil.extractUserId(token);

            assertEquals(userId.toString(), extractedUserId);
        }

        @Test
        @DisplayName("different secret returns null")
        void extractUserId_differentSecret_returnsNull() {
            JwtUtil otherJwtUtil = new JwtUtil("another-secret-key-32-bytes-ok!!");
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");
            String token = otherJwtUtil.generateToken(user);

            String extractedUserId = jwtUtil.extractUserId(token);

            assertNull(extractedUserId, "Extracting userId with a different secret should return null");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("valid token + correct username returns true")
        void isTokenValid_validTokenCorrectUsername_returnsTrue() {
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");
            String token = jwtUtil.generateToken(user);

            boolean result = jwtUtil.isTokenValid(token, "testuser");

            assertTrue(result);
        }

        @Test
        @DisplayName("valid token + wrong username returns false")
        void isTokenValid_validTokenWrongUsername_returnsFalse() {
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");
            String token = jwtUtil.generateToken(user);

            boolean result = jwtUtil.isTokenValid(token, "wronguser");

            assertFalse(result);
        }

        @Test
        @DisplayName("valid token + null username returns false")
        void isTokenValid_validTokenNullUsername_returnsFalse() {
            User user = new User(UUID.randomUUID(), "testuser", "Password1!");
            String token = jwtUtil.generateToken(user);

            boolean result = jwtUtil.isTokenValid(token, null);

            assertFalse(result);
        }
    }
}
