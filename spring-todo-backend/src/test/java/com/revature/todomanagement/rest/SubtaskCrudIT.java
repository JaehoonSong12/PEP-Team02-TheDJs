package com.revature.todomanagement.rest;

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
 * 
 * @see "docs/module/05-api-contract.tex - Endpoint: POST, GET, PUT, DELETE /api/todos/{id}/subtasks"
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos/{id}/subtasks HTTP/1.1
     * Content-Type: application/json
     * Authorization: Bearer &lt;token&gt;
     * 
     * {
     *     "title": "String",
     *     "completed": boolean
     * }
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * {
     *     "id": "UUID",
     *     "taskId": "UUID",
     *     "title": "String",
     *     "completed": boolean
     * }
     * </pre>
     */
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
     *     "message": "Subtask title must not be blank."
     * }
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos/{id}/subtasks HTTP/1.1
     * ...
     * 
     * HTTP/1.1 404 Not Found
     * Content-Type: application/json
     * 
     * {
     *     "status": 404,
     *     "message": "Task not found: &lt;id&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(2)
    @DisplayName("Create subtask on non-existent task returns 404")
    void createSubtaskNonExistentTask_returns404() {
        String randomId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Valid title", "completed", false))
        .when()
                .post("/api/todos/" + randomId + "/subtasks")
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }

    // ------------------------------------------------------------------ //
    //  List Subtasks Tests                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks HTTP/1.1
     * Authorization: Bearer &lt;token&gt;
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * [
     *     {
     *         "id": "UUID",
     *         "taskId": "UUID",
     *         "title": "String",
     *         "completed": boolean
     *     }
     * ]
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks HTTP/1.1
     * ...
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * []
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * Authorization: Bearer &lt;token&gt;
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * {
     *     "id": "UUID",
     *     "taskId": "UUID",
     *     "title": "String",
     *     "completed": boolean
     * }
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 404 Not Found
     * Content-Type: application/json
     * 
     * {
     *     "status": 404,
     *     "message": "Subtask not found: &lt;subtaskId&gt;"
     * }
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * Content-Type: application/json
     * Authorization: Bearer &lt;token&gt;
     * 
     * {
     *     "title": "String",
     *     "completed": boolean
     * }
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * {
     *     "id": "UUID",
     *     "taskId": "UUID",
     *     "title": "String",
     *     "completed": boolean
     * }
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: application/json
     * 
     * {
     *     "status": 400,
     *     "message": "Subtask title must not be blank."
     * }
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 404 Not Found
     * Content-Type: application/json
     * 
     * {
     *     "status": 404,
     *     "message": "Subtask not found: &lt;subtaskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(8)
    @DisplayName("Update non-existent subtask returns 404")
    void updateNonExistentSubtask_returns404() {
        String randomId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Valid update", "completed", false))
        .when()
                .put("/api/todos/" + parentTaskId + "/subtasks/" + randomId)
        .then()
                .statusCode(404)
                .body("message", containsString("Subtask not found"));
    }

    // ------------------------------------------------------------------ //
    //  Delete Subtask Tests                                                //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * DELETE /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * Authorization: Bearer &lt;token&gt;
     * 
     * HTTP/1.1 204 No Content
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * DELETE /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 404 Not Found
     * Content-Type: application/json
     * 
     * {
     *     "status": 404,
     *     "message": "Subtask not found: &lt;subtaskId&gt;"
     * }
     * </pre>
     */
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

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks HTTP/1.1
     * ...
     * 
     * HTTP/1.1 404 Not Found
     * Content-Type: application/json
     * 
     * {
     *     "status": 404,
     *     "message": "Task not found: &lt;id&gt;"
     * }
     * </pre>
     */
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
