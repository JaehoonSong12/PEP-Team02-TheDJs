# Phase 5: Presentation

## Objective
The goal of this phase is the formal demonstration of project completion. The team must prove that the application meets the original user stories, functions reliably, and was developed using professional software engineering standards.

## Checkpoints
- [ ] **Functional Demo:** Perform a complete walkthrough of all user stories in a live environment.
- [ ] **Technical Review:** Explain the architectural choices, including the Spring Boot/Angular/JWT integration.
- [ ] **Build Verification:** Demonstrate a clean build and successful application startup from scratch.
- [ ] **Post-Mortem/Reflection:** Briefly summarize the team's Agile process and how blockers were handled.

## Presentation Requirements

### 1. The Functional Walkthrough
You must demonstrate the following "Happy Path" scenarios:
*   **Onboarding:** A new user registers and successfully logs in.
*   **Core Workflow:** A user creates a Todo, adds nested subtasks, edits them, and eventually deletes them.
*   **Security Check:** Demonstrate that an unauthenticated user cannot access the dashboard.
*   **Persistence:** Show that tasks remain in the system after the application is restarted.

### 2. Technical Deep-Dive
Be prepared to answer questions regarding:
*   **Data Flow:** How a request moves from an Angular component through the JWT interceptor to the Spring Boot controller and finally to SQLite.
*   **Schema Design:** Why you chose your specific relational structure for Todos and Subtasks.
*   **Challenges:** How the team utilized Git, resolved merge conflicts, or overcame technical blockers identified during Stand-ups.

### 3. Build & Deployment
*   **Clean Build:** You must be able to run `./gradlew build` (or equivalent) and `ng build` to prove the project is not running on "magic" local configurations.
*   **Startup:** The application must start successfully with a single command or set of commands.

## Success Criteria
A successful presentation is evaluated on:
*   **Alignment:** Does the software actually do what the User Stories required?
*   **Reliability:** Does the application run smoothly during the demo without unexpected crashes?
*   **Communication:** Can the team clearly articulate *how* the application works, not just *that* it works?
*   **Professionalism:** Did the team demonstrate a disciplined approach to the SDLC and Agile practices?

***

**Project Complete.**

---

[Return to README](../README.md)