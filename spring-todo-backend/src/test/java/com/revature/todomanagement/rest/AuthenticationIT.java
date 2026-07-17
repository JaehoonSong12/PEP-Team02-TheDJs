package com.revature.todomanagement.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the authentication (login) endpoint.
 * Verifies valid login returns a Bearer token and invalid credentials return 401.
 * 
 * @see "docs/module/05-api-contract.tex - Endpoint: POST /api/auth/login"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationIT extends BaseIntegrationTest {

    private static final String USERNAME = "authUser01";
    private static final String PASSWORD = "Pass@123";

    /**
     * Registers a user to serve as the known-valid credential set for login tests.
     * Must run first.
     */
    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * (Setup step using the Account Creation endpoint)
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "authUser01",
     *     "password": "Pass@123"
     * }
     * 
     * HTTP/1.1 201 Created
     * (empty body)
     * </pre>
     */
    @Test
    @Order(1)
    @DisplayName("Setup: Register a user for login tests")
    void registerSetupUser_returns201() {
        // BDD
        given() // config
                .contentType(ContentType.JSON)
                .body(Map.of("username", USERNAME, "password", PASSWORD))
        .when() // execution
                .post("/api/auth/register")
        .then() // assertion
                .statusCode(201);
    }

    /**
     * Valid login with registered credentials returns 200 with Authorization header
     * containing a well-formed Bearer token (at least 27 chars total: "Bearer " + 20+ token chars).
     */
    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/login HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "authUser01",
     *     "password": "Pass@123"
     * }
     * 
     * HTTP/1.1 200 OK
     * Authorization: Bearer &lt;signed-jwt-token&gt;
     * Content-Length: 0
     * (empty body)
     * </pre>
     */
    @Test
    @Order(2)
    @DisplayName("Valid login returns 200 and Bearer token")
    void validLogin_returns200WithToken() {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", USERNAME, "password", PASSWORD))
        .when()
                .post("/api/auth/login");

        response.then().statusCode(200);

        String authHeader = response.header("Authorization");
        assertThat(authHeader, notNullValue());
        assertThat(authHeader, startsWith("Bearer "));
        assertTrue(authHeader.length() >= 27,
                "Authorization header should be at least 27 characters (Bearer + 20+ token chars), but was: " + authHeader.length());
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/login HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "authUser01",
     *     "password": "wrongPassword1!"
     * }
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Invalid username or password
     * </pre>
     */
    @Test
    @Order(3)
    @DisplayName("Login with wrong password returns 401")
    void wrongPassword_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", USERNAME, "password", "WrongPass@999"))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .body(containsString("Invalid username or password"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/login HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "unknownUser99",
     *     "password": "Pass@123"
     * }
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Invalid username or password
     * </pre>
     */
    @Test
    @Order(4)
    @DisplayName("Login with unknown username returns 401")
    void unknownUsername_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "noSuchUser99", "password", "Pass@123"))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .body(containsString("Invalid username or password"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/login HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "",
     *     "password": ""
     * }
     * 
     * HTTP/1.1 401 Unauthorized
     * Content-Type: text/plain
     * 
     * Invalid username or password
     * </pre>
     */
    @Test
    @Order(5)
    @DisplayName("Login with empty credentials returns 401")
    void emptyCredentials_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "", "password", ""))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .body(containsString("Invalid username or password"));
    }
}
