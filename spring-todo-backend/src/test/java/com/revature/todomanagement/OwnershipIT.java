package com.revature.todomanagement;

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
        }
    }

    // ------------------------------------------------------------------ //
    //  Cross-user access tests                                             //
    // ------------------------------------------------------------------ //

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

    @Test
    @Order(5)
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
