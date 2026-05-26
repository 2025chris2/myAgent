#!/bin/bash
set -e

# ============================================
# llongAgent 一键构建+部署脚本（服务器上运行）
# 前端打包进 JAR，8080 端口直接对外服务
# ============================================

APP_DIR="/opt/llongagent"
MCP_SERVER_DIR="$APP_DIR/mcp-servers"

echo "=== Step 1/5: Build frontend ==="
cd "$(dirname "$0")/frontend"
npm ci --silent
npm run build
cd ..

echo "=== Step 2/5: Build backend (with embedded frontend) ==="
mvn clean package -DskipTests -q
echo "Backend build done."

echo "=== Step 3/5: Build MCP server ==="
cd mcp-server
mvn clean package -DskipTests -q
cd ..
echo "MCP server build done."

echo "=== Step 4/5: Stop services ==="
sudo systemctl stop llongagent 2>/dev/null || true
sudo systemctl stop mcp-server 2>/dev/null || true

echo "=== Step 5/5: Deploy JARs & restart ==="
# Main app
sudo cp target/*.jar "$APP_DIR/llongAgent.jar"

# MCP server
sudo mkdir -p "$MCP_SERVER_DIR"
sudo cp mcp-server/target/llong-image-search-mcp-server-0.0.1-SNAPSHOT.jar "$MCP_SERVER_DIR/"

# Install MCP server systemd service
sudo cp mcp-server.service /etc/systemd/system/mcp-server.service

sudo systemctl daemon-reload
sudo systemctl enable llongagent
sudo systemctl start llongagent

sudo systemctl enable mcp-server
sudo systemctl start mcp-server

echo "=== Done! ==="
echo "App: http://$(hostname -I | awk '{print $1}'):8080"
echo "MCP Server: http://$(hostname -I | awk '{print $1}'):8127/sse"
echo "Health: curl http://127.0.0.1:8080/actuator/health"
