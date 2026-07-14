package com.revature.todomanagement;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for cascade delete behavior.
 * Verifies that deleting a parent task also removes all associated subtasks.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CascadeDeleteIT extends BaseIntegrationTest {

    private static final String USERNAME = "cascadeUser!";
    private static final String PASSWORD = "CascPass@1";

    private String token;
    private String taskId;
    private String subtaskId;

    @BeforeAll
    void setup() {
        // Configure REST Assured before @BeforeEach runs (needed for @BeforeAll in PER_CLASS mode)
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Register and authenticate
        token = getAuthToken(USERNAME, PASSWORD);

        // Create a parent task
        Response taskResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Task with subtasks"))
        .when()
                .post("/api/todos");

        taskResponse.then().statusCode(200);
        taskId = taskResponse.jsonPath().getString("id");

        // Create a subtask on that task
        Response subtaskResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Subtask to be cascaded", "completed", false))
        .when()
                .post("/api/todos/" + taskId + "/subtasks");

        subtaskResponse.then().statusCode(200);
        subtaskId = subtaskResponse.jsonPath().getString("id");
    }

    // ------------------------------------------------------------------ //
    //  Cascade Delete Tests                                                //
    // ------------------------------------------------------------------ //

    @Test
    @Order(1)
    @DisplayName("Subtasks exist before delete")
    void subtasksExistBeforeDelete() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + taskId + "/subtasks")
        .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(2)
    @DisplayName("Delete task with subtasks returns 204")
    void deleteTaskWithSubtasks_returns204() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/todos/" + taskId)
        .then()
                .statusCode(204);
    }

    @Test
    @Order(3)
    @DisplayName("Get deleted task returns 404")
    void getDeletedTask_returns404() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + taskId)
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }

    @Test
    @Order(4)
    @DisplayName("List tasks excludes deleted task")
    void listTasksExcludesDeleted() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos")
        .then()
                .statusCode(200)
                .body("id", not(hasItem(taskId)));
    }

    @Test
    @Order(5)
    @DisplayName("Get subtasks of deleted task returns 404")
    void getSubtasksOfDeletedTask_returns404() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + taskId + "/subtasks")
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }
}
