FROM jenkins/jenkins:lts-alpine

USER root

# Install Docker CLI (to talk to host daemon via socket)
RUN apk add --no-cache docker-cli docker-compose

# Install Node.js (for Angular build) and AWS CLI (for S3 sync)
RUN apk add --no-cache nodejs npm aws-cli

# Install plugins from list
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins
