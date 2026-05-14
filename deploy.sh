#!/bin/bash
set -e

# ============================================
# llongAgent 一键构建+部署脚本（服务器上运行）
# 前端打包进 JAR，8080 端口直接对外服务
# ============================================

APP_DIR="/opt/llongagent"

echo "=== Step 1/5: Build frontend ==="
cd "$(dirname "$0")/frontend"
npm ci --silent
npm run build
cd ..

echo "=== Step 2/5: Build backend (with embedded frontend) ==="
mvn clean package -DskipTests -q
echo "Backend build done."

echo "=== Step 3/5: Stop service ==="
sudo systemctl stop llongagent 2>/dev/null || true

echo "=== Step 4/5: Deploy JAR ==="
sudo cp target/*.jar "$APP_DIR/llongAgent.jar"

echo "=== Step 5/5: Start service ==="
sudo systemctl daemon-reload
sudo systemctl enable llongagent
sudo systemctl start llongagent

echo "=== Done! ==="
echo "App: http://$(hostname -I | awk '{print $1}'):8080"
echo "Health: curl http://127.0.0.1:8080/actuator/health"
