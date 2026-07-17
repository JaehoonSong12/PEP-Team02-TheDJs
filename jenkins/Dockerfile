FROM jenkins/jenkins:lts-alpine

USER root

# Docker CLI: allows Jenkins to manage sibling containers via the mounted
# /var/run/docker.sock (Docker-outside-of-Docker pattern). Required for
# docker build, docker stop, docker run in the Deploy Backend stage.
RUN apk add --no-cache docker-cli docker-compose

# Node.js + npm: required for npm ci and npm run build in the Build Frontend
# stage. aws-cli: required for aws s3 sync in the Deploy Frontend to S3 stage.
RUN apk add --no-cache nodejs npm aws-cli

# Pre-install Jenkins plugins at image build time so they are ready on first
# boot without manual UI clicks. See plugins.txt for the list.
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Drop privileges back to the non-root Jenkins user for runtime security.
USER jenkins
