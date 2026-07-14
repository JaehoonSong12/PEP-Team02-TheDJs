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
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationIT extends BaseIntegrationTest {

    private static final String REGISTER_PATH = "/api/auth/register";

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
