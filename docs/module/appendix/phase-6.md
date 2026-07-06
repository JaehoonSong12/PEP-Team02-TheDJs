# Phase 1: Automated Testing Suite

## Objective
The goal of this phase is to implement a rigorous automated testing layer. Before moving to deployment, you must prove that the application is functionally correct and resilient to regressions. This phase focuses on two distinct levels of testing: API-level validation and End-to-End (E2E) user journey validation.

## Checkpoints
- [ ] **API Testing Implementation:** Develop a suite of tests using REST Assured to validate all backend endpoints.
- [ ] **E2E Framework Setup:** Configure Selenium, Cucumber, and JUnit 5 to support browser-based testing.
- [ ] **User Story Translation:** Convert your original user stories into Gherkin (Given/When/Then) feature files.
- [ ] **Test Automation:** Execute the full test suite and ensure all tests pass against your local environment.

## Technical Requirements

### 1. API Testing (REST Assured)
Your API tests must go beyond simple "200 OK" checks. They must validate:
*   **Status Codes:** Ensure correct error codes (400, 401, 404, etc.) are returned for invalid requests.
*   **Payload Integrity:** Validate that the JSON response body matches the expected schema and contains correct data types.
*   **Business Logic:** Verify that specific actions (e.g., creating a subtask) result in the correct state change in the database.

### 2. End-to-End Testing (Selenium, Cucumber, JUnit 5)
You must simulate real user behavior to ensure the frontend and backend are communicating correctly.
*   **Behavior Driven Development (BDD):** Use Cucumber to write tests in plain English using the Gherkin syntax.
*   **Scenario Coverage:** Every user story from Project 1 must have at least one automated E2E scenario (e.g., "Successful User Registration" or "Task Deletion Workflow").
*   **Browser Automation:** Use Selenium to drive the browser through the UI, interacting with forms, buttons, and navigation elements.

## Deliverables for Phase 1
By the end of this phase, your team should have:
1.  **Automated Test Suite:** A repository of code that can be executed to run both API and E2E tests.
2.  **Feature Files:** A set of `feature` files containing the Gherkin definitions for your user stories.
3.  **Test Reports:** Documentation or logs showing successful execution of all test cases.

## Moving to Phase 2
Once the automated test suite is passing consistently and covers all core functionality, proceed to **Phase 2: Containerization**.

---

[Return to README.md](../README.md)