#!/bin/bash
# EC2 Setup Script (Amazon Linux 2023)
# Usage: ssh -i ~/.ssh/todo-app-key.pem ec2-user@54.81.54.237
#        Then paste this entire script.
#
# Idempotent: safe to re-run if interrupted mid-way.

set -e

echo "=== Adding 2GB swap (prevents OOM on t3.small) ==="
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

echo "=== Installing Docker ==="
sudo yum install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

echo "=== Installing Docker Compose ==="
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m) \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

echo "=== Installing Docker Buildx ==="
BUILDX_VERSION=$(curl -s https://api.github.com/repos/docker/buildx/releases/latest | grep '"tag_name"' | cut -d'"' -f4)
sudo curl -SL "https://github.com/docker/buildx/releases/download/${BUILDX_VERSION}/buildx-${BUILDX_VERSION}.linux-amd64" \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx

echo "=== Installing AWS CLI ==="
sudo yum install aws-cli -y

echo "=== Installing Git ==="
sudo yum install git -y

echo "=== Creating production .env ==="
cat > /home/ec2-user/.env << 'EOF'
JWT_SECRET=T0d0AppPr0duct10nS3cr3tK3yTh4t1s64Ch4r4ct3rsL0ng!!R4nd0m
CORS_ALLOWED_ORIGINS=http://todo-app-frontend-team02.s3-website-us-east-1.amazonaws.com
SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/todo.db
AWS_ACCESS_KEY_ID=<your-aws-access-key>
AWS_SECRET_ACCESS_KEY=<your-aws-secret-key>
AWS_REGION=us-east-1
S3_BUCKET=todo-app-frontend-team02
EOF
chmod 600 /home/ec2-user/.env

echo "=== Cloning project ==="
if [ -d /home/ec2-user/project ]; then
  cd /home/ec2-user/project && git pull
else
  git clone https://github.com/JaehoonSong12/PEP-Team02-TheDJs.git /home/ec2-user/project
fi

echo "=== Starting Jenkins ==="
cd /home/ec2-user/project/jenkins
sudo docker compose -f docker-compose.jenkins.yml up -d

echo ""
echo "=== DONE ==="
echo "Jenkins UI: http://54.81.54.237:9090"
echo ""
echo "Next steps:"
echo "  1. Open http://54.81.54.237:9090 in browser"
echo "  2. Create a Pipeline job pointing to jenkins/Jenkinsfile"
echo "  3. Trigger a build (or push to main)"
