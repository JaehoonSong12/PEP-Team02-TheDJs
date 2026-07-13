# Design Document: E2E Cucumber Testing

## Overview

This design completes the Cucumber BDD end-to-end testing infrastructure for the Todo Management Application. The existing codebase has a working `CucumberRunner`, the Registration feature's step definitions, and `RegistrationPom`. This design adds:

- **LoginPom** and **DashboardPom** Page Object Model classes
- **LoginSteps**, **TaskSteps**, and **SubtaskSteps** step definition classes
- A **SharedSteps** class to eliminate duplicate step definitions across feature files
- A **fix to CucumberRunner** configuration to properly discover feature files via `FEATURES_PROPERTY_NAME`

The goal is for all four feature files (registration, login, task, subtask) to run successfully against the Angular frontend and Spring Boot backend using Selenium WebDriver with Firefox.

## Architecture

```mermaid
graph TD
    subgraph Test Execution
        CR[CucumberRunner<br/>@Suite + @CucumberContextConfiguration]
        CR -->|Before/After hooks| WD[WebDriver Lifecycle]
    end

    subgraph Feature Files
        RF[registration.feature]
        LF[login.feature]
        TF[task.feature]
        SF[subtask.feature]
    end

    subgraph Step Definitions
        SS[SharedSteps.java<br/>Shared Given/Then steps]
        RS[RegistrationSteps.java]
        LS[LoginSteps.java]
        TS[TaskSteps.java]
        STS[SubtaskSteps.java]
    end

    subgraph Page Object Models
        RP[RegistrationPom]
        LP[LoginPom]
        DP[DashboardPom]
    end

    subgraph Applications Under Test
        FE[Angular Frontend<br/>localhost:4200]
        BE[Spring Boot Backend<br/>localhost:8080]
    end

    RF --> RS
    LF --> LS
    TF --> TS
    SF --> STS
    RF --> SS
    LF --> SS
    TF --> SS
    SF --> SS

    RS --> RP
    LS --> LP
    TS --> DP
    STS --> DP

    RP --> WD
    LP --> WD
    DP --> WD

    WD --> FE
    FE --> BE
```

### Key Architectural Decisions

1. **Static WebDriver sharing**: Step definitions access the WebDriver via `CucumberRunner.driver` (static field). This is the established pattern in the project — all steps share a single browser session per scenario.

2. **SharedSteps for deduplication**: Steps reused across feature files ("The user is on the login page", "The user should be given an error message", "The user is logged in and on the dashboard") live in a single `SharedSteps.java` class to avoid Cucumber's `DuplicateStepDefinitionException`.

3. **Page Object Model pattern**: Each page of the Angular app gets a POM class that encapsulates locators (`@FindBy`) and interaction methods. Step definitions instantiate POMs with the shared WebDriver and delegate actions.

4. **Explicit waits over implicit waits**: Step definitions use `WebDriverWait` with 5-second timeouts; POM methods that interact with dynamically rendered elements use 10-second waits. No implicit waits are configured globally.

5. **Feature file discovery fix**: Add `FEATURES_PROPERTY_NAME` pointing to `classpath:features/` so the Cucumber engine finds `.feature` files under `src/test/resources/features/`.

## Components and Interfaces

### CucumberRunner (Modified)

**Location:** `com.revature.todomanagement.cucumber.CucumberRunner`

```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("com.revature.todomanagement.cucumber")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "classpath:features/")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.revature.todomanagement.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:reports/cucumber-report.html")
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public class CucumberRunner {
    public static WebDriver driver;

    @Before
    public void setUp() { /* FirefoxDriver init + maximize */ }

    @After
    public void tearDown() { /* driver.quit() + null assignment */ }
}
```

**Changes from current:**
- Add `FEATURES_PROPERTY_NAME` configuration parameter with value `classpath:features/`
- Remove `"features"` from `@SelectPackages` (it only needs the step definition package)

---

### LoginPom

**Location:** `com.revature.todomanagement.cucumber.poms.LoginPom`

| Method | Description |
|--------|-------------|
| `LoginPom(WebDriver driver)` | Constructor; calls `PageFactory.initElements` |
| `void enterCredentials(String username, String password)` | Types into `#username` and `#password` |
| `void clickSubmitButton()` | Clicks `button[type='submit']` |
| `String getErrorMessage()` | Returns text of `[data-testid='error-message']` |

**Fields (via @FindBy):**
- `@FindBy(id = "username")` — `WebElement usernameInput`
- `@FindBy(id = "password")` — `WebElement passwordInput`
- `@FindBy(css = "button[type='submit']")` — `WebElement submitButton`
- `@FindBy(css = "[data-testid='error-message']")` — `WebElement errorMessage`

---

### DashboardPom

**Location:** `com.revature.todomanagement.cucumber.poms.DashboardPom`

| Method | Description |
|--------|-------------|
| `DashboardPom(WebDriver driver)` | Constructor; calls `PageFactory.initElements`; stores driver for explicit waits |
| `void enterTaskTitle(String title)` | Clears and types into `.input-field[placeholder='Enter new task title...']` |
| `void clickAddTaskButton()` | Clicks the "Add Task" button (`.btn-primary`) |
| `boolean isTaskVisible(String title)` | Waits 10s for `.task-title` with text matching `title` in `.task-list` |
| `int getTaskCount()` | Returns count of `.task-item` elements in `.task-list` |
| `void expandTask(String taskTitle)` | Finds the `.task-item` with matching title, clicks its `.btn-expand` |
| `void enterSubtaskTitle(String taskTitle, String subtaskTitle)` | Clears and types into `.subtask-input` within the expanded task's `.subtask-section` |
| `void clickAddSubtaskButton(String taskTitle)` | Clicks "Add Subtask" button within the task's `.subtask-section` |
| `boolean isSubtaskVisible(String subtaskTitle, String parentTaskTitle)` | Waits 10s for `.subtask-title` with text in parent's `.subtask-list` |
| `int getSubtaskCount(String parentTaskTitle)` | Returns count of `.subtask-item` in specified task's `.subtask-list` |
| `String getErrorMessage()` | Returns `.error-message` text, or empty string if not present |
| `void clearTaskInput()` | Clears the task title input field |
| `void clearSubtaskInput(String taskTitle)` | Clears the subtask input within the specified task's expanded section |

**Locator strategy for dynamic elements:** The DashboardPom uses `WebDriverWait` (10s) with `ExpectedConditions` for dynamically rendered elements (task items appearing after creation, subtask sections after expansion, error messages).

---

### SharedSteps

**Location:** `com.revature.todomanagement.cucumber.steps.SharedSteps`

| Step | Method |
|------|--------|
| `Given The user is on the login page` | Navigates to `http://localhost:4200/login` |
| `Then The user should be given an error message` | Waits 5s for error element, asserts text is not empty |
| `Given The user is logged in and on the dashboard` | Registers a unique test user, logs in via UI, waits for dashboard URL |

**Rationale:** These steps appear in multiple feature files. Defining them in a single class avoids `DuplicateStepDefinitionException`. The existing duplicate in `RegistrationSteps` must be removed.

---

### LoginSteps

**Location:** `com.revature.todomanagement.cucumber.steps.LoginSteps`

| Step | Action |
|------|--------|
| `When The user enters valid login credentials` | Uses LoginPom to enter pre-registered credentials |
| `When The user enters invalid login credentials` | Uses LoginPom to enter non-existent username/password |
| `When The user clicks login button` | Uses LoginPom to click submit |
| `Then The user should be redirected to the dashboard page` | Waits 5s for URL to contain `/dashboard` |
| `Then The user should remain on the login page` | Asserts URL contains `/login` |

**Test user setup:** The Login feature's Background uses shared step "The user is on the login page". The valid login scenario needs a pre-registered user. `LoginSteps` will register a test user in a `@Before("@login")` hook or the valid credentials step will first register the user programmatically through the UI.

**Design decision:** Since the login feature's Background is just "Given The user is on the login page" (handled by SharedSteps), and the valid login step needs credentials that actually exist in the system, the `LoginSteps` class will maintain a test username/password pair. A `@Before` tagged hook or a helper method will register the user through the registration page before the login attempt. This keeps the test self-contained without relying on direct database seeding.

---

### TaskSteps

**Location:** `com.revature.todomanagement.cucumber.steps.TaskSteps`

| Step | Action |
|------|--------|
| `When The user enters {string} in the title input field` | DashboardPom.enterTaskTitle(title) |
| `When The user clicks the Add task button` | DashboardPom.clickAddTaskButton() |
| `Then The task {string} should appear in the task list` | DashboardPom.isTaskVisible(title), assert true |
| `When The user leaves the task input field empty` | DashboardPom.clearTaskInput() |
| `Then No new task is added to the task list` | Assert task count unchanged |

**State tracking:** `TaskSteps` captures the `.task-item` count before actions that should not add tasks, then asserts the count is unchanged afterward.

---

### SubtaskSteps

**Location:** `com.revature.todomanagement.cucumber.steps.SubtaskSteps`

| Step | Action |
|------|--------|
| `And A task {string} exists in the task list` | Creates the task via UI (type title + click Add Task), waits for it |
| `When The user expands the task {string}` | DashboardPom.expandTask(title); waits for subtask section |
| `And The user enters {string} in the subtask title input field` | DashboardPom.enterSubtaskTitle(expandedTask, title) |
| `And The user clicks the Add subtask button` | DashboardPom.clickAddSubtaskButton(expandedTask) |
| `Then The subtask {string} should appear under {string}` | DashboardPom.isSubtaskVisible(subtask, parent), assert true |
| `And The user leaves the subtask title input field empty` | DashboardPom.clearSubtaskInput(expandedTask) |
| `Then No new subtask is added under {string}` | Assert subtask count unchanged |

**State tracking:** `SubtaskSteps` captures the `.subtask-item` count before actions that should not add subtasks, then asserts the count is unchanged afterward.

---

## Data Models

This feature doesn't introduce new persistent data models. The tests interact with the existing `User`, `Task`, and `Subtask` entities through the Angular UI.

### Test Data Strategy

| Scenario Context | Data Approach |
|-----------------|---------------|
| Registration tests | Generate unique usernames with timestamp suffix |
| Login tests | Register a user first, then log in with those credentials |
| Task tests | Log in (shared step), create tasks through the UI |
| Subtask tests | Log in (shared step), create parent task, then create subtasks |

The `ddl-auto=create-drop` + H2 in-memory database ensures each test execution starts with a clean slate. However, within a single test run, scenarios share the same database state since the backend stays running.

### WebDriver Wait Configuration

| Context | Timeout | Rationale |
|---------|---------|-----------|
| Step definitions — element presence | 5 seconds | Fast feedback on missing elements |
| DashboardPom — dynamic elements | 10 seconds | Tasks/subtasks render after API calls complete |
| URL navigation assertions | 5 seconds | Route transitions should be near-instant |

## Error Handling

### WebDriver Initialization Failure

If Firefox/GeckoDriver is unavailable, the `@Before` hook in `CucumberRunner` will throw an unhandled exception. Cucumber marks the scenario as failed with the stack trace — no custom error handling needed.

### Element Not Found

- **Step definitions:** `WebDriverWait` throws `TimeoutException` after 5s. The exception propagates to Cucumber, which marks the step as failed and reports the selector that timed out.
- **POM methods:** `NoSuchElementException` from `@FindBy` elements propagates directly. Dynamic lookups via `WebDriverWait` throw `TimeoutException`.

### Error Message Elements Missing

- **LoginPom:** `getErrorMessage()` lets `NoSuchElementException` propagate if the element isn't in the DOM (per Requirement 2.6).
- **DashboardPom:** `getErrorMessage()` returns empty string if `.error-message` is not present (per Requirement 4.10). Uses a try-catch around the lookup.

### Test Isolation

Each scenario gets a fresh WebDriver (via `@Before`/`@After` hooks). If a scenario fails mid-execution, `@After` still closes the browser, preventing zombie processes.

## Testing Strategy

### Why Property-Based Testing Does NOT Apply

This feature is E2E browser automation testing. It is NOT suitable for property-based testing because:

- **Tests external systems**: Selenium drives a real browser interacting with a real Angular app and Spring Boot backend
- **Behavior doesn't vary meaningfully with generated inputs**: Scenarios are specific acceptance criteria (login with valid/invalid creds, create task with title/empty title)
- **High cost per execution**: Each test opens a browser, makes HTTP requests, waits for DOM rendering — running 100+ iterations is impractical
- **No pure functions**: Step definitions are imperative browser automation code, not functions with input/output that can be property-tested

### Testing Approach

**Integration/E2E Tests (Cucumber Scenarios):**
All testing for this feature IS the E2E test suite itself. The feature files define the scenarios; the step definitions automate them.

| Feature File | Scenarios | Validates |
|-------------|-----------|-----------|
| `registration.feature` | 2 (valid/invalid registration) | User Story 1 |
| `login.feature` | 2 (valid/invalid login) | User Story 2 |
| `task.feature` | 2 (create with title / empty title) | User Story 3 |
| `subtask.feature` | 2 (create with title / empty title) | User Story 4 |

**Manual Verification Checklist:**
- [ ] All 8 scenarios pass with both apps running
- [ ] `reports/cucumber-report.html` is generated after test run
- [ ] No `DuplicateStepDefinitionException` errors
- [ ] No undefined/pending steps in console output
- [ ] WebDriver properly quits after each scenario (no orphan Firefox processes)

**Execution Command:**
```bash
# From spring-todo-backend/
gradlew.bat test
```

Prerequisites: Angular frontend running at localhost:4200, GeckoDriver on PATH.
