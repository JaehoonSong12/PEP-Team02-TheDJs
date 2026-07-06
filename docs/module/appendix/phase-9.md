# Phase 4: CI/CD Pipeline

## Objective
The goal of this phase is to implement automation that bridges the gap between your code repository and your cloud infrastructure. Using Jenkins, you will build a CI/CD (Continuous Integration/Continuous Deployment) pipeline that automates the building, testing, and deployment of your application. This phase moves the project from manual deployment to an automated, repeatable workflow.

## Checkpoints
- [ ] **Jenkins Environment Setup:** Install and configure Jenkins on a dedicated server or instance.
- [ ] **Pipeline Scripting (MVP):** Create a Jenkinsfile to automate the basic build and deployment steps.
- [ ] **Automated Testing Integration:** Integrate your REST Assured and Selenium/Cucumber test suites into the pipeline.
- [ ] **Deployment Automation (Stretch Goal):** Automate the push to AWS S3 and the update of the EC2 instance within the pipeline.

## Technical Requirements

### 1. Jenkins Pipeline (The Jenkinsfile)
Your pipeline should be defined as code (a `Jenkinsfile`) stored in your version control system. The pipeline must follow a logical sequence of stages:
*   **Stage 1: Checkout:** Pull the latest code from GitHub.
*   **Stage 2: Build:** Compile the application and build Docker images.
*   **Stage 3: Test:** Execute the automated testing suite (API and E2E). **The pipeline must fail if any test fails.**
*   **Stage 4: Deploy:** Push the artifacts to AWS (S3 for frontend, EC2 for backend).

### 2. Testing Gatekeeping
The pipeline acts as the quality gate for your project. 
*   **Failure Handling:** If the REST Assured or Selenium tests fail, the pipeline must stop immediately. No code should be deployed to AWS if the tests do not pass.
*   **Reporting:** Configure Jenkins to capture and display test results so that failures are easy to diagnose.

### 3. Deployment Automation (MVP vs. Stretch Goal)
*   **MVP (Minimum Viable Product):** Jenkins should successfully automate the build and test stages. Deployment to AWS may involve manual steps or simple scripted commands.
*   **Stretch Goal (Full Automation):** The pipeline should handle the entire lifecycle. This includes building the Docker images, pushing them to a registry, updating the EC2 instance, and syncing the Angular build to the S3 bucket automatically upon a successful merge to the `main` branch.

## Deliverables for Phase 4
By the end of this phase, your team should have:
1.  **A Functional CI/CD Pipeline:** A working Jenkins pipeline triggered by code changes.
2.  **Automated Deployment:** A demonstrated workflow where a code push results in an updated application on AWS.
3.  **Pipeline Logs:** Evidence of a "Green Build" where all tests passed and the deployment was successful.

## Moving to Phase 5
Once your pipeline is running and successfully deploying code to the cloud, proceed to **Phase 5: Presentation**.

---

[Return to README.md](../README.md)