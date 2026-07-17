package com.revature.todomanagement.rest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for the AuthInterceptor.
 * Validates token absence, malformation, expiry, invalid signature,
 * and valid token scenarios.
 * 
 * @see "docs/module/05-api-contract.tex - Section: Authentication"
 */
public class AuthInterceptorIT extends BaseIntegrationTest {

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos HTTP/1.1
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Missing or malformed Authorization header
     * </pre>
     */
    @Test
    void noAuthHeader_returns401() {
        given()
                .header("Content-Type", "application/json")
        .when()
                .get("/api/todos")
        .then()
                .statusCode(401)
                .body(containsString("Missing or malformed Authorization header"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos HTTP/1.1
     * Authorization: Bearer &lt;invalid-signature-token&gt;
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Invalid or expired token
     * </pre>
     */
    @Test
    void invalidSignature_returns401() {
        // Craft a JWT signed with a DIFFERENT secret key
        String wrongSecret = "WrongSecretKeyThatIsAlsoAtLeast32Characters!!";
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

        String invalidToken = Jwts.builder()
                .subject("testuser")
                .claim("userId", "some-uuid")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        given()
                .header("Authorization", "Bearer " + invalidToken)
        .when()
                .get("/api/todos")
        .then()
                .statusCode(401)
                .body(containsString("Invalid or expired token"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos HTTP/1.1
     * Authorization: Token xyz
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Missing or malformed Authorization header
     * </pre>
     */
    @Test
    void malformedHeader_returns401() {
        given()
                .header("Authorization", "Token xyz")
        .when()
                .get("/api/todos")
        .then()
                .statusCode(401)
                .body(containsString("Missing or malformed Authorization header"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos HTTP/1.1
     * Authorization: Bearer &lt;expired-token&gt;
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Invalid or expired token
     * </pre>
     */
    @Test
    void expiredToken_returns401() {
        // Craft a JWT with expiration in the past (1 minute ago)
        String testSecret = "TestSecretKeyThatIsAtLeast32CharactersLong!!";
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .subject("testuser")
                .claim("userId", "some-uuid")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000L))
                .expiration(new Date(System.currentTimeMillis() - 60_000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        given()
                .header("Authorization", "Bearer " + expiredToken)
        .when()
                .get("/api/todos")
        .then()
                .statusCode(401)
                .body(containsString("Invalid or expired token"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos HTTP/1.1
     * Authorization: Bearer &lt;valid-token&gt;
     * 
     * HTTP/1.1 200 OK
     * ...
     * </pre>
     */
    @Test
    void validToken_proceeds() {
        // Register and login to get a valid token
        String token = getAuthToken("interceptUser!", "interceptPass@1");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos")
        .then()
                .statusCode(not(equalTo(401)));
    }
}
