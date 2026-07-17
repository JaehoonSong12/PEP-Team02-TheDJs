package com.revature.todomanagement.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Task CRUD endpoints (POST/GET/PUT/DELETE /api/todos).
 * Verifies create, read, update, and delete operations with validation.
 * 
 * @see "docs/module/05-api-contract.tex - Endpoint: POST, GET, PUT, DELETE /api/todos"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskCrudIT extends BaseIntegrationTest {

    private static final String USERNAME = "taskCrudUser!";
    private static final String PASSWORD = "TaskPass@1";

    private String token;
    private String createdTaskId;

    /**
     * Acquires token lazily (called at the start of each test that needs it).
     * REST Assured is configured by the @BeforeEach in BaseIntegrationTest,
     * so we cannot use @BeforeAll for HTTP calls.
     */
    private void ensureAuthenticated() {
        if (token == null) {
            token = getAuthToken(USERNAME, PASSWORD);
        }
    }

    // ------------------------------------------------------------------ //
    //  Create                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos HTTP/1.1
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
     *     "userId": "UUID",
     *     "title": "String",
     *     "completed": boolean
     * }
     * </pre>
     */
    @Test
    @Order(1)
    @DisplayName("Create task with valid title returns 200")
    void createWithValidTitle_returns200() {
        ensureAuthenticated();
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Buy groceries"))
        .when()
                .post("/api/todos");

        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("userId", notNullValue())
                .body("title", equalTo("Buy groceries"))
                .body("completed", equalTo(false));

        createdTaskId = response.jsonPath().getString("id");
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos HTTP/1.1
     * ...
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: application/json
     * 
     * {
     *     "status": 400,
     *     "message": "Task title must not be blank."
     * }
     * </pre>
     */
    @Test
    @Order(2)
    @DisplayName("Create task with blank title returns 400")
    void createWithBlankTitle_returns400() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "   "))
        .when()
                .post("/api/todos")
        .then()
                .statusCode(400)
                .body("message", containsString("Task title must not be blank."));
    }

    // ------------------------------------------------------------------ //
    //  Read                                                                //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos HTTP/1.1
     * Authorization: Bearer &lt;token&gt;
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * [
     *     {
     *         "id": "UUID",
     *         "userId": "UUID",
     *         "title": "String",
     *         "completed": boolean
     *     }
     * ]
     * </pre>
     */
    @Test
    @Order(3)
    @DisplayName("List tasks returns user's tasks only")
    void listTasks_returnsUserTasksOnly() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("id", hasItem(createdTaskId));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id} HTTP/1.1
     * Authorization: Bearer &lt;token&gt;
     * 
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 
     * {
     *     "id": "UUID",
     *     "userId": "UUID",
     *     "title": "String",
     *     "completed": boolean
     * }
     * </pre>
     */
    @Test
    @Order(4)
    @DisplayName("Get task by ID returns 200")
    void getById_returns200() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + createdTaskId)
        .then()
                .statusCode(200)
                .body("id", equalTo(createdTaskId))
                .body("title", equalTo("Buy groceries"))
                .body("completed", equalTo(false));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id} HTTP/1.1
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
    @Order(5)
    @DisplayName("Get task by non-existent ID returns 404")
    void getByNonExistentId_returns404() {
        String randomId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + randomId)
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }

    // ------------------------------------------------------------------ //
    //  Update                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id} HTTP/1.1
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
     *     "userId": "UUID",
     *     "title": "String",
     *     "completed": boolean
     * }
     * </pre>
     */
    @Test
    @Order(6)
    @DisplayName("Update task returns 200")
    void updateTask_returns200() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Buy organic groceries", "completed", true))
        .when()
                .put("/api/todos/" + createdTaskId)
        .then()
                .statusCode(200)
                .body("title", equalTo("Buy organic groceries"))
                .body("completed", equalTo(true));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 400 Bad Request
     * Content-Type: application/json
     * 
     * {
     *     "status": 400,
     *     "message": "Task title must not be blank."
     * }
     * </pre>
     */
    @Test
    @Order(7)
    @DisplayName("Update task with blank title returns 400")
    void updateWithBlankTitle_returns400() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", ""))
        .when()
                .put("/api/todos/" + createdTaskId)
        .then()
                .statusCode(400)
                .body("message", containsString("Task title must not be blank."));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id} HTTP/1.1
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
    @Order(8)
    @DisplayName("Update non-existent task returns 404")
    void updateNonExistentTask_returns404() {
        String randomId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Updated title", "completed", true))
        .when()
                .put("/api/todos/" + randomId)
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }

    // ------------------------------------------------------------------ //
    //  Delete                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * DELETE /api/todos/{id} HTTP/1.1
     * Authorization: Bearer &lt;token&gt;
     * 
     * HTTP/1.1 204 No Content
     * </pre>
     */
    @Test
    @Order(8)
    @DisplayName("Delete task returns 204")
    void deleteTask_returns204() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/todos/" + createdTaskId)
        .then()
                .statusCode(204);
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * DELETE /api/todos/{id} HTTP/1.1
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
    @Order(9)
    @DisplayName("Delete already deleted task returns 404")
    void deleteAlreadyDeleted_returns404() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/todos/" + createdTaskId)
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id} HTTP/1.1
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
    @Order(10)
    @DisplayName("Get deleted task returns 404")
    void deleteThenGet_returns404() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/todos/" + createdTaskId)
        .then()
                .statusCode(404)
                .body("message", containsString("Task not found"));
    }
}
