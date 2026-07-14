package com.revature.todomanagement;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for subtask CRUD operations.
 * Validates create, read, update, and delete of subtasks nested under a parent task.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubtaskCrudIT extends BaseIntegrationTest {

    private static final String USERNAME = "subtaskUser!";
    private static final String PASSWORD = "SubPass@1";

    private String token;
    private String parentTaskId;
    private String emptyTaskId; // A second parent task with no subtasks
    private String subtaskId;

    @BeforeAll
    void setup() {
        // Configure REST Assured before @BeforeEach runs (needed for @BeforeAll in PER_CLASS mode)
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        token = getAuthToken(USERNAME, PASSWORD);

        // Create a parent task for most subtask tests
        Response taskResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Parent Task for Subtasks"))
        .when()
                .post("/api/todos");

        taskResponse.then().statusCode(200);
        parentTaskId = taskResponse.jsonPath().getString("id");

        // Create a second parent task (no subtasks will be added) for the empty list test
        Response emptyTaskResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Empty Parent Task"))
        .when()
                .post("/api/todos");

        emptyTaskResponse.then().statusCode(200);
        emptyTaskId = emptyTaskResponse.jsonPath().getString("id");
    }

    // ------------------------------------------------------------------ //
    //  Create Subtask Tests                                                //
    // ------------------------------------------------------------------ //

    @Test
    @Order(1)
    @DisplayName("Create subtask with valid title returns 200")
    void createWithValidTitle_returns200() {
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Buy groceries", "completed", false))
        .when()
                .post("/api/todos/" + parentTaskId + "/subtasks");

        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("taskId", equalTo(parentTaskId))
                .body("title", equalTo("Buy groceries"))
                .body("completed", equalTo(false));

        // Store the subtask ID for later tests
        subtaskId = response.jsonPath().getString("id");
    }

    @Test
    @Order(2)
    @DisplayName("Create subtask with blank title returns 400")
    void createWithBlankTitle_returns400() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "", "completed", false))
        .when()
                .post("/api/todos/" + parentTaskId + "/subtasks")
        .then()
                .statusCode(400)
                .body("message", containsString("Subtask title must not be blank."));
    }

    // ------------------------------------------------------------------ //
    //  List Subtasks Tests                                                 //
    // ------------------------------------------------------------------ //

    @Test
    @Order(3)
    @DisplayName("List subtasks returns all subtasks for task")
    void listSubtasks_returnsAll() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + parentTaskId + "/subtasks")
        .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1))
                .body("[0].id", notNullValue())
                .body("[0].taskId", equalTo(parentTaskId))
                .body("[0].title", notNullValue())
                .body("[0].completed", equalTo(false));
    }

    @Test
    @Order(4)
    @DisplayName("List subtasks for task with no subtasks returns empty array")
    void listSubtasksEmpty_returnsEmptyArray() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + emptyTaskId + "/subtasks")
        .then()
                .statusCode(200)
                .body("$.size()", equalTo(0));
    }

    // ------------------------------------------------------------------ //
    //  Get Subtask by ID Tests                                             //
    // ------------------------------------------------------------------ //

    @Test
    @Order(5)
    @DisplayName("Get subtask by ID returns 200")
    void getById_returns200() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + parentTaskId + "/subtasks/" + subtaskId)
        .then()
                .statusCode(200)
                .body("id", equalTo(subtaskId))
                .body("taskId", equalTo(parentTaskId))
                .body("title", equalTo("Buy groceries"))
                .body("completed", equalTo(false));
    }

    @Test
    @Order(6)
    @DisplayName("Get subtask by non-existent ID returns 404")
    void getByNonExistentId_returns404() {
        String randomId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + parentTaskId + "/subtasks/" + randomId)
        .then()
                .statusCode(404)
                .body("message", containsString("Subtask not found"));
    }

    // ------------------------------------------------------------------ //
    //  Update Subtask Tests                                                //
    // ------------------------------------------------------------------ //

    @Test
    @Order(7)
    @DisplayName("Update subtask returns 200")
    void updateSubtask_returns200() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Buy organic groceries", "completed", true))
        .when()
                .put("/api/todos/" + parentTaskId + "/subtasks/" + subtaskId)
        .then()
                .statusCode(200)
                .body("id", equalTo(subtaskId))
                .body("taskId", equalTo(parentTaskId))
                .body("title", equalTo("Buy organic groceries"))
                .body("completed", equalTo(true));
    }

    @Test
    @Order(8)
    @DisplayName("Update subtask with blank title returns 400")
    void updateWithBlankTitle_returns400() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "   ", "completed", false))
        .when()
                .put("/api/todos/" + parentTaskId + "/subtasks/" + subtaskId)
        .then()
                .statusCode(400)
                .body("message", containsString("Subtask title must not be blank."));
    }

    // ------------------------------------------------------------------ //
    //  Delete Subtask Tests                                                //
    // ------------------------------------------------------------------ //

    @Test
    @Order(9)
    @DisplayName("Delete subtask returns 204")
    void deleteSubtask_returns204() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/todos/" + parentTaskId + "/subtasks/" + subtaskId)
        .then()
                .statusCode(204);
    }

    @Test
    @Order(10)
    @DisplayName("Delete already deleted subtask returns 404")
    void deleteAlreadyDeleted_returns404() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/todos/" + parentTaskId + "/subtasks/" + subtaskId)
        .then()
                .statusCode(404)
                .body("message", containsString("Subtask not found"));
    }

    // ------------------------------------------------------------------ //
    //  Non-existent Parent Task Test                                        //
    // ------------------------------------------------------------------ //

    @Test
    @Order(11)
    @DisplayName("List subtasks for non-existent task returns 404")
    void listSubtasksNonExistentTask_returns404() {
        String randomTaskId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + randomTaskId + "/subtasks")
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }
}
