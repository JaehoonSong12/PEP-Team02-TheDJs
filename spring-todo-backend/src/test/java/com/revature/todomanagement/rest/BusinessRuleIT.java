package com.revature.todomanagement.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the business rule: no subtasks on completed tasks.
 * Verifies that marking a task as completed succeeds, adding a subtask to a completed task
 * is rejected with 400, and adding a subtask to an incomplete task succeeds.
 * 
 * @see "docs/module/05-api-contract.tex - Endpoint: POST /api/todos/{id}/subtasks (Business Rule)"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BusinessRuleIT extends BaseIntegrationTest {

    private static final String USERNAME = "bizRuleUser!";
    private static final String PASSWORD = "BizPass@1";

    private String token;
    private String taskId;

    /**
     * Acquires token lazily (called at the start of each test that needs it).
     * REST Assured is configured by the @BeforeEach in BaseIntegrationTest,
     * so we cannot use @BeforeAll for HTTP calls.
     */
    private void ensureSetup() {
        if (token == null) {
            token = getAuthToken(USERNAME, PASSWORD);

            // Create a task to use for business rule tests
            Response response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("title", "Business Rule Task"))
            .when()
                    .post("/api/todos");

            response.then().statusCode(200);
            taskId = response.jsonPath().getString("id");
        }
    }

    // ------------------------------------------------------------------ //
    //  Business Rule Tests                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id} HTTP/1.1
     * Content-Type: application/json
     * Authorization: Bearer &lt;token&gt;
     * 
     * {
     *     "title": "Business Rule Task",
     *     "completed": true
     * }
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * {
     *     ...
     *     "completed": true
     * }
     * </pre>
     */
    @Test
    @Order(1)
    @DisplayName("Mark task as completed returns 200 with completed: true")
    void markTaskCompleted_returns200() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Business Rule Task", "completed", true))
        .when()
                .put("/api/todos/" + taskId)
        .then()
                .statusCode(200)
                .body("completed", equalTo(true));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos/{id}/subtasks HTTP/1.1
     * ...
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: application/json
     * 
     * {
     *     "status": 400,
     *     "message": "Cannot add subtasks to a completed task."
     * }
     * </pre>
     */
    @Test
    @Order(2)
    @DisplayName("Create subtask on completed task returns 400")
    void createSubtaskOnCompleted_returns400() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "New Subtask", "completed", false))
        .when()
                .post("/api/todos/" + taskId + "/subtasks")
        .then()
                .statusCode(400)
                .body("message", containsString("Cannot add subtasks to a completed task."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos/{id}/subtasks HTTP/1.1
     * Content-Type: application/json
     * Authorization: Bearer &lt;token&gt;
     * 
     * {
     *     "title": "Valid Subtask",
     *     "completed": false
     * }
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * {
     *     "id": "UUID",
     *     "taskId": "UUID",
     *     "title": "Valid Subtask",
     *     "completed": false
     * }
     * </pre>
     */
    @Test
    @Order(3)
    @DisplayName("Create subtask on incomplete task returns 200")
    void createSubtaskOnIncomplete_returns200() {
        ensureSetup();

        // Create a NEW task that is not completed
        Response newTaskResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Incomplete Task For Subtask"))
        .when()
                .post("/api/todos");

        newTaskResponse.then().statusCode(200);
        String newTaskId = newTaskResponse.jsonPath().getString("id");

        // Create a subtask on the non-completed task — should succeed
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Valid Subtask", "completed", false))
        .when()
                .post("/api/todos/" + newTaskId + "/subtasks")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("title", equalTo("Valid Subtask"))
                .body("completed", equalTo(false));
    }
}
