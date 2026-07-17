
#!/bin/bash
set -e

echo "========================================"
echo "  EC2 Setup: Jenkins + Backend (clean)"
echo "========================================"

# --- 1. CLEANUP ---
echo ""
echo "=== [1/8] Cleaning previous state ==="
sudo docker stop jenkins-jenkins-1 todo-backend 2>/dev/null || true
sudo docker rm jenkins-jenkins-1 todo-backend 2>/dev/null || true
rm -rf /home/ec2-user/project
echo "  Done."

# --- 2. SWAP (prevents OOM on t3.small) ---
echo ""
echo "=== [2/8] Configuring 2GB swap ==="
if [ -f /swapfile ]; then
    echo "  Swap already exists, skipping."
else
    sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
    echo "  Swap created and enabled."
fi

# --- 3. DOCKER ---
echo ""
echo "=== [3/8] Installing Docker ==="
sudo yum install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Persist socket permissions across reboots (Jenkins needs access)
sudo mkdir -p /etc/systemd/system/docker.service.d
cat <<'OVERRIDE' | sudo tee /etc/systemd/system/docker.service.d/socket-permissions.conf
[Service]
ExecStartPost=/bin/chmod 666 /var/run/docker.sock
OVERRIDE
sudo systemctl daemon-reload
sudo systemctl restart docker

# --- 4. DOCKER COMPOSE + BUILDX ---
echo ""
echo "=== [4/8] Installing Docker Compose + Buildx ==="
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

BUILDX_VERSION=$(curl -s https://api.github.com/repos/docker/buildx/releases/latest | grep '"tag_name"' | cut -d'"' -f4)
sudo curl -SL "https://github.com/docker/buildx/releases/download/${BUILDX_VERSION}/buildx-${BUILDX_VERSION}.linux-amd64" \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx

# --- 5. SYSTEM PACKAGES ---
echo ""
echo "=== [5/8] Installing AWS CLI + Git ==="
sudo yum install aws-cli git -y

# --- 6. PRODUCTION .env ---
echo ""
echo "=== [6/8] Creating production .env ==="
cat > /home/ec2-user/.env << 'EOF'
JWT_SECRET==?????????????????????????????????????????????????
CORS_ALLOWED_ORIGINS=http://todo-app-frontend-team02.s3-website-us-east-1.amazonaws.com
SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/todo.db
AWS_ACCESS_KEY_ID==?????????????????????????????????????????????????
AWS_SECRET_ACCESS_KEY=?????????????????????????????????????????????????
AWS_REGION=us-east-1
S3_BUCKET=todo-app-frontend-team02
EC2_PUBLIC_IP==?????????????????????????????????????????????????
EOF
chmod 600 /home/ec2-user/.env
echo "  .env written. Fill in AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY."

# --- 7. CLONE PROJECT ---
echo ""
echo "=== [7/8] Cloning project ==="
git clone https://github.com/JaehoonSong12/PEP-Team02-TheDJs.git /home/ec2-user/project

# --- 8. START JENKINS ---
echo ""
echo "=== [8/8] Building and starting Jenkins ==="
cd /home/ec2-user/project/jenkins
sudo docker compose -f docker-compose.jenkins.yml up -d --build

echo ""
echo "========================================"
echo "  SETUP COMPLETE"
echo "========================================"
echo "  Jenkins UI:  http://54.81.54.237:9090"
echo "  Backend API: http://54.81.54.237:8080 (after first pipeline run)"
echo "  S3 Frontend: http://todo-app-frontend-team02.s3-website-us-east-1.amazonaws.com"
echo ""
echo "  Next:"
echo "    1. Fill real AWS credentials in /home/ec2-user/.env"
echo "    2. Create Pipeline job in Jenkins UI -> jenkins/Jenkinsfile"
echo "    3. Push to main (or Build Now)"




# # # EXTRA (Client side, )
# # -----------------------
# # Some user specific scriptings done, for local test, using these...

# TEMP=$PWD
# cd .. || exit

# on_kt     # JAVA/Kotlin
# on_gradle # Gradle

# cd "$TEMP" || exit
