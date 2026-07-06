# Phase 3: Cloud Infrastructure

## Objective
The goal of this phase is to provision and configure the cloud environment required to host your application. You will move away from local hosting and establish a production-ready architecture on AWS, utilizing S3 for static frontend hosting and EC2 for your dynamic backend services.

## Checkpoints
- [ ] **AWS Resource Provisioning:** Set up the necessary AWS infrastructure (EC2 instances and S3 buckets).
- [ ] **Frontend Hosting (S3):** Configure an S3 bucket to host your containerized/built Angular application as a static website.
- [ ] **Backend Hosting (EC2):** Deploy your Spring Boot application onto an EC2 instance.
- [ ] **Network Configuration:** Configure Security Groups and routing to allow necessary traffic (HTTP/HTTPS) while maintaining a secure perimeter.

## Technical Requirements

### 1. Frontend Deployment (AWS S3)
*   **Static Website Hosting:** Enable the "Static website hosting" feature on your S3 bucket.
*   **Content Delivery:** Upload your production Angular build artifacts (the contents of your `dist/` folder) to the bucket.
*   **Permissions:** Configure Bucket Policies to allow public read access to your website assets (ensure this is scoped correctly to prevent data leaks).

### 2. Backend Deployment (AWS EC2)
*   **Instance Configuration:** Provision an EC2 instance capable of running your Docker containers or your Spring Boot JAR.
*   **Environment Parity:** Ensure the EC2 environment (Java version, OS, environment variables) matches the requirements established in Phase 2.
*   **Data Persistence:** Configure a strategy for your SQLite database to ensure data is not lost when the EC2 instance is stopped or terminated.

### 3. Networking and Security
*   **Security Groups:** Implement the principle of least privilege. 
    *   Open port 80/443 for web traffic.
    *   Open specific ports for your backend API.
    *   Restrict SSH access (Port 22) to known IP addresses only.
*   **Connectivity:** Ensure the frontend (hosted on S3) can successfully communicate with the backend (hosted on EC2) via the public internet/API endpoints.
    * While there is no direct connection between the S3 bucket and the EC2 instance, the browser will treat them as different origins. You must configure CORS (Cross-Origin Resource Sharing) in your Spring Boot application to allow requests from your S3 website URL.

## Deliverables for Phase 3
By the end of this phase, your team should have:
1.  **Live URLs:** A publicly accessible URL for your frontend (S3) and a functional API endpoint (EC2).
2.  **Infrastructure Documentation:** A brief summary of your AWS configuration (Instance types, Security Group rules, and S3 settings).
3.  **Manual Deployment Verification:** Proof that the application can be manually deployed to these cloud resources and functions correctly.

## Moving to Phase 4
Once your application is successfully running in the AWS cloud, proceed to **Phase 4: CI/CD Pipeline** to automate this entire process.

---

[Return to README.md](../README.md)