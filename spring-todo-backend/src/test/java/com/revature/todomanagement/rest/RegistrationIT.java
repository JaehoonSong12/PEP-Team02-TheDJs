package com.revature.todomanagement.rest;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;

/**
 * Integration tests for the POST /api/auth/register endpoint.
 * Validates username and password validation rules, successful registration,
 * and duplicate username detection.
 * 
 * @see "docs/module/05-api-contract.tex - Endpoint: POST /api/auth/register"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationIT extends BaseIntegrationTest {

    private static final String REGISTER_PATH = "/api/auth/register";

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "",
     *     "password": "Valid1Pass!"
     * }
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: text/plain
     * 
     * Username must not be blank.
     * </pre>
     */
    @Test
    @Order(1)
    @DisplayName("Blank username returns 400 with appropriate message")
    void blankUsername_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "", "password", "Valid1Pass!"))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(400)
                .body(containsString("Username must not be blank."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "abcd",
     *     "password": "Valid1Pass!"
     * }
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: text/plain
     * 
     * Username must be between 5 and 18 characters.
     * </pre>
     */
    @Test
    @Order(2)
    @DisplayName("Short username (less than 5 chars) returns 400")
    void shortUsername_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "abcd", "password", "Valid1Pass!"))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(400)
                .body(containsString("Username must be between 5 and 18 characters."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "thisusernameiswaytoolong",
     *     "password": "Valid1Pass!"
     * }
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: text/plain
     * 
     * Username must be between 5 and 18 characters.
     * </pre>
     */
    @Test
    @Order(3)
    @DisplayName("Long username (more than 18 chars) returns 400")
    void longUsername_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "thisusernameiswaytoolong", "password", "Valid1Pass!"))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(400)
                .body(containsString("Username must be between 5 and 18 characters."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "validuser",
     *     "password": ""
     * }
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: text/plain
     * 
     * Password must not be blank.
     * </pre>
     */
    @Test
    @Order(4)
    @DisplayName("Blank password returns 400 with appropriate message")
    void blankPassword_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "validuser", "password", ""))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(400)
                .body(containsString("Password must not be blank."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "validuser",
     *     "password": "NoSpecial1a"
     * }
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: text/plain
     * 
     * Password must contain at least one special character (!@#$%^&*).
     * </pre>
     */
    @Test
    @Order(5)
    @DisplayName("Password without special character returns 400")
    void noSpecialCharPassword_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "validuser", "password", "NoSpecial1a"))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(400)
                .body(containsString("Password must contain at least one special character (!@#$%^&*)."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "regituser",
     *     "password": "Valid1Pass!"
     * }
     * 
     * HTTP/1.1 201 Created
     * (empty body)
     * </pre>
     */
    @Test
    @Order(6)
    @DisplayName("Valid registration returns 201 with empty body")
    void validRegistration_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "regituser", "password", "Valid1Pass!"))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(201)
                .body(emptyString());
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/auth/register HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *     "username": "regituser",
     *     "password": "Valid1Pass!"
     * }
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: text/plain
     * 
     * Username 'regituser' is already taken.
     * </pre>
     */
    @Test
    @Order(7)
    @DisplayName("Duplicate username returns 400 with 'is already taken.'")
    void duplicateUsername_returns400() {
        // Attempt to register the same username that was registered in validRegistration_returns201
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "regituser", "password", "Valid1Pass!"))
        .when()
                .post(REGISTER_PATH)
        .then()
                .statusCode(400)
                .body(containsString("is already taken."));
    }
}
