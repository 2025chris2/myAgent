#!/bin/bash
set -e

# ============================================
# llongAgent 首次服务器初始化脚本
# 创建用户、目录、安装 systemd 服务
# ============================================

APP_DIR="/opt/llongagent"
FRONTEND_DIST="/var/www/llongagent"

echo "=== Creating user and directories ==="
sudo useradd -r -s /bin/false llongagent 2>/dev/null || true
sudo mkdir -p "$APP_DIR" "$FRONTEND_DIST"

echo "=== Installing systemd service ==="
sudo cp "$(dirname "$0")/llongagent.service" /etc/systemd/system/llongagent.service
sudo systemctl daemon-reload
sudo systemctl enable llongagent

echo "=== Setup done ==="
echo "Run ./deploy.sh to build and deploy."
