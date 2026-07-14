package com.revature.todomanagement;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Abstract base class for all REST Assured integration tests.
 * Configures REST Assured with the embedded server's random port and H2 database.
 * Provides shared setup, teardown, and authentication helper methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    /**
     * Configures REST Assured before each test method.
     * Sets base URI, port, default content type to JSON,
     * and enables logging of request/response on validation failure.
     */
    @BeforeEach
    void setupRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.defaultParser = io.restassured.parsing.Parser.JSON;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Resets REST Assured configuration after all tests in a subclass complete.
     * Ensures no state leaks between test classes.
     */
    @AfterAll
    static void teardownRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = -1;
        RestAssured.basePath = "";
        RestAssured.filters(Collections.emptyList());
        RestAssured.requestSpecification = null;
    }

    /**
     * Registers a user and logs in, returning the extracted JWT token string.
     *
     * @param username the username to register and log in with
     * @param password the password to register and log in with
     * @return the plain JWT token string (without "Bearer " prefix)
     * @throws AssertionError if registration does not return 201 or login does not return 200
     */
    protected String getAuthToken(String username, String password) {
        Map<String, String> credentials = Map.of("username", username, "password", password);

        // Step 1: Register the user
        Response registerResponse = given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/auth/register");

        if (registerResponse.statusCode() != 201) {
            throw new AssertionError(
                    "Registration failed: expected status 201 but got " + registerResponse.statusCode()
                            + ". Body: " + registerResponse.body().asString());
        }

        // Step 2: Log in with the same credentials
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/auth/login");

        if (loginResponse.statusCode() != 200) {
            throw new AssertionError(
                    "Login failed: expected status 200 but got " + loginResponse.statusCode()
                            + ". Body: " + loginResponse.body().asString());
        }

        // Step 3: Extract the Authorization header and strip "Bearer " prefix
        String authHeader = loginResponse.header("Authorization");
        if (authHeader == null || authHeader.length() < 7) {
            throw new AssertionError(
                    "Login response missing or invalid Authorization header: " + authHeader);
        }

        return authHeader.substring(7); // Strip "Bearer " prefix
    }
}
