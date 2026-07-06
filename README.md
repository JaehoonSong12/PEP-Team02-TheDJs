# Project 1: Todo Management Application

## Objective
Build a full-stack, secure Todo Management application using Agile methodologies. You will move from initial requirement analysis to a functional product demonstration, simulating a real-world Software Development Lifecycle (SDLC).

## Tech Stack
| Layer | Technology |
| :--- | :--- |
| **Frontend** | Angular |
| **Backend** | Java (Spring Boot, Web, Data JPA) |
| **Database** | SQLite |
| **Build/Tools** | Gradle, Git, GitHub |

## User Stories (The "What")
*   **Registration:** As a new user, I can register an account.
*   **Authentication:** As a new user, I can log in/out securely.
*   **Task Management:** As a user, I can CRUD primary Todo items.
*   **Organization:** As a user, I can CRUD nested subtasks.

## Project Roadmap
*Click a phase to view specific checkpoints and requirements.*

### [Phase 1: Requirements Analysis](./docs/phase-1.md)
> **Goal:** Deconstruct user stories into technical tasks and database schemas.

### [Phase 2: Design](./docs/phase-2.md)
> **Goal:** Plan API contracts, UI wireframes, and system architecture.

### [Phase 3: Backend Development](./docs/phase-3.md)
> **Goal:** Build the REST API, Security layer, and Data Persistence.

### [Phase 4: Frontend Development](./docs/phase-4.md)
> **Goal:** Build the responsive Angular UI and integrate with the API.

### [Phase 5: Presentation](./docs/phase-5.md)
> **Goal:** Demonstrate working software and technical architecture.

## Agile & Collaboration Standards
To ensure professional delivery, all teams must adhere to:
*   **Daily Stand-ups:** Answer: *What did I do? What am I doing? Any blockers?*
*   **Git Workflow:** 
    - Feature branching (`feature/name`) or something similar
    - peer-reviewed Pull Requests
    - atomic commits
    - Utilizing GitHub Issues and Projects to organize and track work
*   **Success Criteria:** Alignment with design, effective Git collaboration, and application reliability.


# Project 2: DevOps & Automated Testing

## Objective
The goal of Project 2 is to transition your application from a local development environment to a production-ready cloud architecture. You will implement a robust automated testing suite and establish a Continuous Integration/Continuous Deployment (CI/CD) pipeline to automate the deployment of your application to AWS.

## Technology Stack
| Category | Technology |
| :--- | :--- |
| **Cloud Hosting** | AWS (EC2 for Backend, S3 for Frontend) |
| **Containerization** | Docker |
| **CI/CD Automation** | Jenkins (MVP: Manual setup; Stretch: Full Pipeline) |
| **API Testing** | REST Assured |
| **E2E Testing** | Selenium, Cucumber, JUnit 5 |

## Testing Requirements (Primary Goal)
Before deployment, your application must prove its reliability through two layers of testing:

1.  **API Layer (REST Assured):** Automated validation of all RESTful endpoints, ensuring correct status codes, JSON schemas, and business logic.
2.  **End-to-End Layer (Selenium/Cucumber/JUnit 5):** Automated "User Story" testing. Using Gherkin syntax (Given/When/Then), you will simulate real user behavior in a browser to ensure the frontend and backend work in unison.

## Project Roadmap
*Click a phase to view specific checkpoints and requirements.*

### [Phase 1: Automated Testing Suite](./docs/phase-1.md)
> **Goal:** Implement REST Assured for API testing and Selenium/Cucumber for E2E testing.

### [Phase 2: Containerization](./docs/phase-2.md)
> **Goal:** Dockerize the application components to ensure environmental consistency.

### [Phase 3: Cloud Infrastructure](./docs/phase-3.md)
> **Goal:** Provision AWS resources (EC2 and S3) to host the backend and frontend.

### [Phase 4: CI/CD Pipeline](./docs/phase-4.md)
> **Goal:** Use Jenkins to automate the build, test, and deployment process.

### [Phase 5: Presentation](./docs/phase-5.md)
> **Goal:** Demonstrate a code change triggering an automated pipeline and deployment.

---

## Agile & DevOps Practices
*   **Daily Stand-ups:** Focus on "Pipeline Blockers" and "Test Failures."
*   **Infrastructure as Code (Concept):** Aim to document your AWS and Docker configurations clearly.
*   **Continuous Feedback:** Use your automated test results as the primary gatekeeper for your deployment.

## Success Criteria
*   **Test Coverage:** All core user stories are validated by automated API & E2E tests.
*   **Deployment Success:** The application is accessible via a public AWS URL.
*   **Pipeline Integrity:** A code push triggers a Jenkins build that runs tests before deploying.
*   **Reliability:** The deployment process is repeatable and minimizes manual intervention.