# Implementation Plan: E2E Cucumber Testing

## Overview

Complete the Cucumber BDD end-to-end testing infrastructure by fixing the test runner configuration, creating shared step definitions, building Page Object Model classes for Login and Dashboard pages, and implementing step definitions for Login, Task, and Subtask feature files. All code is Java using Selenium WebDriver, Cucumber, and JUnit Platform within the existing Spring Boot test context.

## Tasks

- [ ] 1. Fix CucumberRunner configuration and create SharedSteps
  - [ ] 1.1 Add FEATURES_PROPERTY_NAME to CucumberRunner and clean up @SelectPackages
    - Import `FEATURES_PROPERTY_NAME` from `io.cucumber.junit.platform.engine.Constants`
    - Add `@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "classpath:features/")` annotation
    - Change `@SelectPackages({"features", "com.revature.todomanagement.cucumber"})` to `@SelectPackages("com.revature.todomanagement.cucumber")`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [ ] 1.2 Create SharedSteps.java with shared step definitions
    - Create `src/test/java/com/revature/todomanagement/cucumber/steps/SharedSteps.java`
    - Implement `@Given("The user is on the login page")` — navigates to `http://localhost:4200/login`
    - Implement `@Then("The user should be given an error message")` — waits 5s for `[data-testid='error-message']` or `.error-message`, asserts text not empty
    - Implement `@Given("The user is logged in and on the dashboard")` — registers a unique test user via UI, logs in, waits for `/dashboard` URL
    - Access WebDriver via `CucumberRunner.driver`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 7.4, 7.5_

  - [ ] 1.3 Refactor RegistrationSteps.java to remove duplicate step definitions
    - Remove `the_user_is_on_the_login_page()` method (moved to SharedSteps)
    - Remove `the_user_should_be_given_an_error_message()` method (moved to SharedSteps)
    - Ensure remaining registration-specific steps still compile and reference correct POMs
    - _Requirements: 8.1, 8.4_

- [ ] 2. Create Page Object Model classes
  - [ ] 2.1 Create LoginPom.java
    - Create `src/test/java/com/revature/todomanagement/cucumber/poms/LoginPom.java`
    - Add `@FindBy(id = "username")` private WebElement usernameInput
    - Add `@FindBy(id = "password")` private WebElement passwordInput
    - Add `@FindBy(css = "button[type='submit']")` private WebElement submitButton
    - Add `@FindBy(css = "[data-testid='error-message']")` private WebElement errorMessage
    - Constructor accepts WebDriver, calls `PageFactory.initElements(driver, this)`
    - Implement `enterCredentials(String username, String password)` — sendKeys to both fields
    - Implement `clickSubmitButton()` — clicks submit button
    - Implement `getErrorMessage()` — returns errorMessage.getText(), lets NoSuchElementException propagate
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ] 2.2 Create DashboardPom.java
    - Create `src/test/java/com/revature/todomanagement/cucumber/poms/DashboardPom.java`
    - Constructor accepts WebDriver, stores it, calls `PageFactory.initElements(driver, this)`
    - Implement `enterTaskTitle(String title)` — clears and types into `.input-field` with placeholder "Enter new task title..."
    - Implement `clickAddTaskButton()` — clicks `.btn-primary` button with text "Add Task"
    - Implement `isTaskVisible(String title)` — WebDriverWait 10s for `.task-title` with matching text in `.task-list`
    - Implement `getTaskCount()` — returns count of `.task-item` elements
    - Implement `expandTask(String taskTitle)` — finds `.task-item` with title, clicks `.btn-expand`
    - Implement `enterSubtaskTitle(String taskTitle, String subtaskTitle)` — types into `.subtask-input` within expanded task
    - Implement `clickAddSubtaskButton(String taskTitle)` — clicks "Add Subtask" button in task's subtask section
    - Implement `isSubtaskVisible(String subtaskTitle, String parentTaskTitle)` — WebDriverWait 10s for `.subtask-title` in parent's `.subtask-list`
    - Implement `getSubtaskCount(String parentTaskTitle)` — returns count of `.subtask-item` in task's subtask list
    - Implement `getErrorMessage()` — returns `.error-message` text or empty string if not present (try-catch)
    - Implement `clearTaskInput()` — clears the task input field
    - Implement `clearSubtaskInput(String taskTitle)` — clears subtask input in specified task's section
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12_

- [ ] 3. Checkpoint - Verify POMs and SharedSteps compile
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Implement Login step definitions
  - [ ] 4.1 Create LoginSteps.java
    - Create `src/test/java/com/revature/todomanagement/cucumber/steps/LoginSteps.java`
    - Access WebDriver via `CucumberRunner.driver`
    - Instantiate LoginPom when needed
    - Implement helper method to register a test user via the UI (navigate to register page, enter unique credentials, submit, wait for redirect to login)
    - Implement `@When("The user enters valid login credentials")` — registers a test user first, then enters those credentials via LoginPom
    - Implement `@When("The user enters invalid login credentials")` — enters non-existent username/password via LoginPom
    - Implement `@When("The user clicks login button")` — calls LoginPom.clickSubmitButton()
    - Implement `@Then("The user should be redirected to the dashboard page")` — WebDriverWait 5s for URL containing `/dashboard`
    - Implement `@Then("The user should remain on the login page")` — asserts URL contains `/login`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

- [ ] 5. Implement Task step definitions
  - [ ] 5.1 Create TaskSteps.java
    - Create `src/test/java/com/revature/todomanagement/cucumber/steps/TaskSteps.java`
    - Access WebDriver via `CucumberRunner.driver`
    - Instantiate DashboardPom when needed
    - Track task count before actions to validate "no new task" assertions
    - Implement `@When("The user enters {string} in the title input field")` — calls DashboardPom.enterTaskTitle(title)
    - Implement `@When("The user clicks the Add task button")` — calls DashboardPom.clickAddTaskButton()
    - Implement `@Then("The task {string} should appear in the task list")` — calls DashboardPom.isTaskVisible(title), assertTrue
    - Implement `@When("The user leaves the task input field empty")` — calls DashboardPom.clearTaskInput(), captures current task count
    - Implement `@Then("No new task is added to the task list")` — asserts task count unchanged
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [ ] 6. Implement Subtask step definitions
  - [ ] 6.1 Create SubtaskSteps.java
    - Create `src/test/java/com/revature/todomanagement/cucumber/steps/SubtaskSteps.java`
    - Access WebDriver via `CucumberRunner.driver`
    - Instantiate DashboardPom when needed
    - Track expanded task title and subtask count for assertions
    - Implement `@And("A task {string} exists in the task list")` — creates task via DashboardPom (enter title, click Add Task, wait for visibility)
    - Implement `@When("The user expands the task {string}")` — calls DashboardPom.expandTask(title), waits for subtask section
    - Implement `@And("The user enters {string} in the subtask title input field")` — calls DashboardPom.enterSubtaskTitle(expandedTask, title)
    - Implement `@And("The user clicks the Add subtask button")` — calls DashboardPom.clickAddSubtaskButton(expandedTask)
    - Implement `@Then("The subtask {string} should appear under {string}")` — calls DashboardPom.isSubtaskVisible(subtask, parent), assertTrue
    - Implement `@And("The user leaves the subtask title input field empty")` — calls DashboardPom.clearSubtaskInput(expandedTask), captures subtask count
    - Implement `@Then("No new subtask is added under {string}")` — asserts subtask count unchanged
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_

- [ ] 7. Final checkpoint - Ensure all tests compile and no duplicate steps exist
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All step definitions use the shared WebDriver from `CucumberRunner.driver` (static field)
- SharedSteps eliminates `DuplicateStepDefinitionException` by centralizing reused steps
- Test users are created via UI registration (no direct DB seeding) for full E2E coverage
- WebDriverWait: 5s in step definitions, 10s in DashboardPom for dynamic elements
- Prerequisites: Angular frontend at localhost:4200, GeckoDriver on PATH, Firefox installed
- Run tests with `gradlew.bat test` from `spring-todo-backend/`
- No property-based testing applies — this is browser automation E2E testing

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["4.1"] },
    { "id": 3, "tasks": ["5.1", "6.1"] }
  ]
}
```
