package com.revature.todomanagement.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ownership enforcement.
 * Verifies that cross-user access to tasks and subtasks is denied with HTTP 403,
 * and that User B's task list does not leak data from User A.
 * 
 * @see "docs/module/05-api-contract.tex - Section: Ownership"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnershipIT extends BaseIntegrationTest {

    private static final String USER_A_USERNAME = "ownerUserA!";
    private static final String USER_A_PASSWORD = "OwnerA@123";

    private static final String USER_B_USERNAME = "ownerUserB!";
    private static final String USER_B_PASSWORD = "OwnerB@123";

    private String tokenA;
    private String tokenB;
    private String userATaskId;
    private String userASubtaskId;

    /**
     * Lazily registers both users, acquires tokens, and creates a task as User A.
     * Called at the start of the first test that needs it.
     */
    private void ensureSetup() {
        if (tokenA == null) {
            tokenA = getAuthToken(USER_A_USERNAME, USER_A_PASSWORD);
            tokenB = getAuthToken(USER_B_USERNAME, USER_B_PASSWORD);

            // Create a task as User A
            Response response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenA)
                    .body(Map.of("title", "User A private task"))
            .when()
                    .post("/api/todos");

            response.then()
                    .statusCode(200)
                    .body("id", notNullValue());

            userATaskId = response.jsonPath().getString("id");

            // Create a subtask as User A
            Response subtaskResponse = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenA)
                    .body(Map.of("title", "User A private subtask", "completed", false))
            .when()
                    .post("/api/todos/" + userATaskId + "/subtasks");

            subtaskResponse.then().statusCode(200);
            userASubtaskId = subtaskResponse.jsonPath().getString("id");
        }
    }

    // ------------------------------------------------------------------ //
    //  Cross-user access tests                                             //
    // ------------------------------------------------------------------ //

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(1)
    @DisplayName("User B GET User A's task returns 403")
    void crossUserGet_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .get("/api/todos/" + userATaskId)
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(2)
    @DisplayName("User B PUT User A's task returns 403")
    void crossUserUpdate_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
                .body(Map.of("title", "Hijacked title", "completed", true))
        .when()
                .put("/api/todos/" + userATaskId)
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * DELETE /api/todos/{id} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(3)
    @DisplayName("User B DELETE User A's task returns 403 and preserves task")
    void crossUserDelete_returns403AndPreservesTask() {
        ensureSetup();

        // User B attempts to delete User A's task — should be denied
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .delete("/api/todos/" + userATaskId)
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));

        // Verify User A can still access the task
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenA)
        .when()
                .get("/api/todos/" + userATaskId)
        .then()
                .statusCode(200)
                .body("id", equalTo(userATaskId));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(4)
    @DisplayName("User B GET subtasks of User A's task returns 403")
    void crossUserListSubtasks_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .get("/api/todos/" + userATaskId + "/subtasks")
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * POST /api/todos/{id}/subtasks HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(5)
    @DisplayName("User B POST subtask to User A's task returns 403")
    void crossUserCreateSubtask_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
                .body(Map.of("title", "Hijacked subtask", "completed", false))
        .when()
                .post("/api/todos/" + userATaskId + "/subtasks")
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * GET /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(6)
    @DisplayName("User B GET User A's subtask returns 403")
    void crossUserGetSubtask_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .get("/api/todos/" + userATaskId + "/subtasks/" + userASubtaskId)
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * PUT /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(7)
    @DisplayName("User B PUT User A's subtask returns 403")
    void crossUserUpdateSubtask_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
                .body(Map.of("title", "Hijacked subtask update", "completed", false))
        .when()
                .put("/api/todos/" + userATaskId + "/subtasks/" + userASubtaskId)
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    /**
     * Validates the following exchange from docs/module/05-api-contract.tex:
     * <pre>
     * DELETE /api/todos/{id}/subtasks/{subtaskId} HTTP/1.1
     * ...
     * 
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     * 
     * {
     *     "status": 403,
     *     "message": "User &lt;userId&gt; does not own task &lt;taskId&gt;"
     * }
     * </pre>
     */
    @Test
    @Order(8)
    @DisplayName("User B DELETE User A's subtask returns 403")
    void crossUserDeleteSubtask_returns403() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .delete("/api/todos/" + userATaskId + "/subtasks/" + userASubtaskId)
        .then()
                .statusCode(403)
                .body("message", containsString("does not own task"));
    }

    @Test
    @Order(9)
    @DisplayName("User B list own tasks returns empty array")
    void userBListOwnTasks_returnsEmpty() {
        ensureSetup();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .get("/api/todos")
        .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
