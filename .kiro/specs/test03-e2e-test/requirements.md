# Requirements Document

## Introduction

Complete the Cucumber BDD end-to-end testing implementation for the Todo Management Application. The project already has feature files, a Cucumber test runner, and step definitions for the Registration user story. This spec covers implementing the remaining step definitions, Page Object Model classes, and fixing test runner configuration so that all four user stories (Registration, Login, Task Management, Subtask Organization) are fully covered with Selenium-driven browser tests.

## Glossary

- **Test_Runner**: The `CucumberRunner` class that configures and orchestrates Cucumber test execution with JUnit Platform, Spring Boot context, and WebDriver lifecycle management.
- **Step_Definition**: A Java class that maps Gherkin steps (Given/When/Then) to Selenium WebDriver actions against the Angular frontend.
- **Page_Object_Model**: A Java class encapsulating web page element locators and interaction methods for a specific page, following the Page Object Model design pattern.
- **Login_POM**: The Page Object Model class for the Login page (`/login`), providing methods to enter credentials, click buttons, and read error messages.
- **Dashboard_POM**: The Page Object Model class for the Dashboard page (`/dashboard`), providing methods for task creation, task list interaction, subtask expansion, and subtask creation.
- **Feature_File**: A `.feature` file written in Gherkin syntax that declares scenarios and acceptance criteria for a given user story.
- **WebDriver**: The Selenium Firefox WebDriver instance used to automate browser interactions during E2E tests.
- **Frontend_App**: The Angular application served at `http://localhost:4200`.
- **Backend_App**: The Spring Boot application served at `http://localhost:8080`.

## Requirements

### Requirement 1: Login Step Definitions

**User Story:** As a tester, I want step definitions that automate the Login feature file scenarios, so that login functionality is validated end-to-end through the browser.

#### Acceptance Criteria

1. WHEN the Login feature file is executed, THE Step_Definition SHALL map all Gherkin steps in the login Background and both Scenarios (valid login and invalid login) to WebDriver actions against the Frontend_App running at http://localhost:4200, reusing the shared Background step "The user is on the login page" already defined in SharedSteps.
2. WHEN the step "The user enters valid login credentials" is executed, THE Step_Definition SHALL enter a pre-registered username and password (matching credentials previously created via the registration flow or test setup) into the `#username` and `#password` input fields on the Login page, waiting up to 5 seconds for each element to be present before interaction.
3. WHEN the step "The user clicks login button" is executed, THE Step_Definition SHALL click the `button[type='submit']` element on the Login page.
4. WHEN the step "The user should be redirected to the dashboard page" is executed, THE Step_Definition SHALL wait a maximum of 5 seconds for the URL to contain `/dashboard` and then assert the current URL includes `/dashboard`.
5. WHEN the step "The user enters invalid login credentials" is executed, THE Step_Definition SHALL enter a username that does not exist in the system and an arbitrary non-empty password into the `#username` and `#password` input fields on the Login page.
6. WHEN the step "The user should remain on the login page" is executed, THE Step_Definition SHALL assert the current URL contains `/login`.
7. IF any WebDriver element lookup fails to locate the target element within 5 seconds, THEN THE Step_Definition SHALL fail the step with an assertion error indicating which element selector was not found.

### Requirement 2: Login Page Object Model

**User Story:** As a tester, I want a Page Object Model for the Login page, so that Login step definitions use encapsulated element locators and interaction methods.

#### Acceptance Criteria

1. THE Login_POM SHALL locate the username input via `@FindBy(id = "username")`, the password input via `@FindBy(id = "password")`, the submit button via `@FindBy(css = "button[type='submit']")`, and the error message element via `@FindBy(css = "[data-testid='error-message']")`, each stored as a private `WebElement` field.
2. THE Login_POM SHALL provide a public method that accepts a username string and a password string and enters them into the respective input fields using `sendKeys`.
3. THE Login_POM SHALL provide a public method that clicks the submit button using the WebElement `click()` action.
4. THE Login_POM SHALL provide a public method that returns the visible text content of the error message element as a `String` via `getText()`.
5. THE Login_POM SHALL accept a `WebDriver` instance as its sole constructor parameter, assign it to a private field, and call `PageFactory.initElements(driver, this)` to initialize all `@FindBy`-annotated elements.
6. IF the error message element is not present in the DOM when its text is retrieved, THEN THE Login_POM SHALL allow the resulting `NoSuchElementException` to propagate to the calling step definition.

### Requirement 3: Task Step Definitions

**User Story:** As a tester, I want step definitions that automate the Task feature file scenarios, so that task creation functionality is validated end-to-end through the browser.

#### Acceptance Criteria

1. WHEN the Task feature file is executed, THE Step_Definition SHALL map all Gherkin steps in the task Background and both Scenarios to WebDriver actions against the Frontend_App, using the DashboardPom page object for element interactions and the shared WebDriver instance from CucumberRunner.
2. WHEN the step "The user enters {string} in the title input field" is executed, THE Step_Definition SHALL locate the `.input-field` element with placeholder "Enter new task title..." on the Dashboard page and type the provided string value into that element.
3. WHEN the step "The user clicks the Add task button" is executed, THE Step_Definition SHALL click the element matching `.btn-primary` with text "Add Task" on the Dashboard page.
4. WHEN the step "The task {string} should appear in the task list" is executed, THE Step_Definition SHALL wait a maximum of 5 seconds for an element with class `.task-title` containing the specified text to become visible within the `.task-list`, and assert that such an element is present.
5. WHEN the step "The user leaves the task input field empty" is executed, THE Step_Definition SHALL locate the `.input-field` element with placeholder "Enter new task title..." and clear its content so that the field value is an empty string.
6. WHEN the step "No new task is added to the task list" is executed, THE Step_Definition SHALL capture the count of `.task-item` elements within `.task-list` and assert that no new `.task-item` has been added compared to the count before the "Add Task" action was performed.
7. IF any WebDriver wait exceeds its 5-second timeout during step execution, THEN THE Step_Definition SHALL allow the timeout exception to propagate, causing the Cucumber scenario to fail with a clear indication of which element was not found or not visible.

### Requirement 4: Dashboard Page Object Model

**User Story:** As a tester, I want a Page Object Model for the Dashboard page, so that Task and Subtask step definitions use encapsulated element locators and interaction methods.

#### Acceptance Criteria

1. THE Dashboard_POM SHALL locate the task title input via `.input-field` with placeholder "Enter new task title...", the Add Task button via the button with text "Add Task", and the task list via `.task-list`.
2. THE Dashboard_POM SHALL provide a method to clear the task title input field and enter the specified text string.
3. THE Dashboard_POM SHALL provide a method to click the Add Task button.
4. THE Dashboard_POM SHALL provide a method that returns a boolean indicating whether a task with the exact given title text exists as a `.task-title` element within the `.task-list`.
5. THE Dashboard_POM SHALL locate the subtask input via `.subtask-input` within a task's expanded subtask section, the Add Subtask button via the button with text "Add Subtask", and the subtask list via `.subtask-list`.
6. THE Dashboard_POM SHALL provide a method to expand a task identified by its exact title text by locating the corresponding `.task-item` and clicking its `.btn-expand` button.
7. THE Dashboard_POM SHALL provide a method to clear the subtask input field within the expanded section of the task identified by its exact title text and enter the specified text string.
8. THE Dashboard_POM SHALL provide a method to click the Add Subtask button within the expanded section of the task identified by its exact title text.
9. THE Dashboard_POM SHALL provide a method that returns a boolean indicating whether a subtask with the exact given title text exists as a `.subtask-title` element within the `.subtask-list` of the `.task-item` identified by the specified parent task title.
10. IF the `.error-message` element is not present on the page, THEN THE Dashboard_POM SHALL return an empty string from the error message retrieval method; otherwise it SHALL return the text content of the `.error-message` element.
11. THE Dashboard_POM SHALL accept a WebDriver instance via its constructor and initialize all annotated elements using Selenium PageFactory.
12. THE Dashboard_POM SHALL use an explicit wait of up to 10 seconds when locating dynamically rendered elements (task list items, subtask sections, and error messages) before interacting with or reading them.

### Requirement 5: Subtask Step Definitions

**User Story:** As a tester, I want step definitions that automate the Subtask feature file scenarios, so that subtask creation functionality is validated end-to-end through the browser.

#### Acceptance Criteria

1. WHEN the Subtask feature file is executed, THE Step_Definition SHALL implement step methods in `SubtaskSteps.java` that map every Gherkin step in the subtask Background and both Scenarios to WebDriver actions against the Frontend_App, such that the entire feature file runs without undefined or pending step errors.
2. WHEN the step "A task {string} exists in the task list" is executed, THE Step_Definition SHALL type the specified task title into the `.input-field` within `.create-task-form`, click the Add Task button, and then wait up to 5 seconds for the task title to appear within the `.task-list` element before proceeding.
3. WHEN the step "The user expands the task {string}" is executed, THE Step_Definition SHALL locate the `.task-item` containing the specified task title and click its `.btn-expand` button, then wait up to 5 seconds for the `.subtask-section` to become visible within that task item.
4. WHEN the step "The user enters {string} in the subtask title input field" is executed, THE Step_Definition SHALL type the provided string into the `.subtask-input` element within the visible `.subtask-section` of the currently expanded task.
5. WHEN the step "The user clicks the Add subtask button" is executed, THE Step_Definition SHALL click the "Add Subtask" button within the `.create-subtask-form` of the currently expanded task's `.subtask-section`.
6. WHEN the step "The subtask {string} should appear under {string}" is executed, THE Step_Definition SHALL wait up to 5 seconds for a `.subtask-item` containing the specified subtask text (within its `.subtask-title`) to be visible in the `.subtask-list` of the specified parent task, and fail the assertion if not found within that timeout.
7. WHEN the step "The user leaves the subtask title input field empty" is executed, THE Step_Definition SHALL clear the `.subtask-input` field within the expanded task's `.subtask-section` so that its value is an empty string.
8. WHEN the step "No new subtask is added under {string}" is executed, THE Step_Definition SHALL capture the count of `.subtask-item` elements in the specified parent task's `.subtask-list` before the action and assert that the count has not increased after clicking Add Subtask.

### Requirement 6: Cucumber Runner Configuration

**User Story:** As a tester, I want the Cucumber test runner to correctly discover feature files and step definitions, so that all scenarios execute without configuration errors.

#### Acceptance Criteria

1. THE Test_Runner SHALL configure the `FEATURES_PROPERTY_NAME` to `classpath:features/` so the Cucumber engine discovers all `.feature` files located under `src/test/resources/features/`.
2. THE Test_Runner SHALL configure the `GLUE_PROPERTY_NAME` to `com.revature.todomanagement.cucumber` so the Cucumber engine discovers all step definition classes and hooks within that package and its sub-packages.
3. WHEN a scenario begins execution, THE Test_Runner SHALL initialize a new Firefox WebDriver instance and assign it to a shared static field accessible by step definition classes, ensuring no WebDriver state carries over from a previous scenario.
4. WHEN the WebDriver is initialized, THE Test_Runner SHALL maximize the browser window before the scenario's first step executes.
5. WHEN a scenario completes (regardless of pass or fail outcome), THE Test_Runner SHALL quit the WebDriver instance and set the shared static field to null, releasing all browser resources.
6. IF the WebDriver fails to initialize (e.g., Firefox binary not found, GeckoDriver unavailable), THEN THE Test_Runner SHALL allow the exception to propagate unhandled so the scenario is marked as failed with a descriptive error indicating the initialization failure.
7. THE Test_Runner SHALL configure the `PLUGIN_PROPERTY_NAME` to generate an HTML test report so that scenario results are persisted after execution.

### Requirement 7: Test Infrastructure Prerequisites

**User Story:** As a tester, I want clear preconditions for running the E2E test suite, so that the test environment is properly set up before execution.

#### Acceptance Criteria

1. WHILE the E2E test suite is executing, THE Frontend_App SHALL be accessible at `http://localhost:4200`.
2. WHILE the E2E test suite is executing, THE Backend_App SHALL be accessible at `http://localhost:8080`.
3. THE Test_Runner SHALL use `SpringBootTest.WebEnvironment.DEFINED_PORT` to start the Backend_App on port 8080 during test execution.
4. THE Step_Definition classes SHALL use `http://localhost:4200` as the base URL for all WebDriver navigation.
5. WHEN a step definition requires an authenticated user, THE Step_Definition SHALL register a test user with a unique username and a valid password through the UI registration form, then log in through the UI login form, and verify the dashboard is displayed before proceeding with the scenario actions.
6. THE Test_Runner SHALL execute each test scenario against a fresh database state, relying on the `ddl-auto=create-drop` configuration to reset the schema on each backend startup.

### Requirement 8: Shared Step Definitions

**User Story:** As a tester, I want shared step definitions for steps that are reused across multiple feature files, so that Cucumber does not encounter duplicate step definition errors.

#### Acceptance Criteria

1. WHEN the step "The user should be given an error message" is used in multiple feature files, THE Step_Definition SHALL define this step exactly once in a SharedSteps class located within the Cucumber glue path, and SHALL remove any duplicate definitions from other step definition classes (e.g., RegistrationSteps).
2. THE shared Step_Definition class SHALL wait up to 5 seconds for the error message element (`[data-testid='error-message']` or `.error-message`) to become visible and SHALL assert its text content is not empty.
3. WHEN the step "The user is logged in and on the dashboard" is used in multiple feature files, THE Step_Definition SHALL define this step exactly once in the SharedSteps class, and the step SHALL navigate to the login page, enter valid credentials, submit the login form, and wait up to 5 seconds for the dashboard page to be displayed.
4. WHEN the step "The user is on the login page" is used in multiple feature files, THE Step_Definition SHALL define this step exactly once in the SharedSteps class and SHALL remove the duplicate definition from RegistrationSteps.
