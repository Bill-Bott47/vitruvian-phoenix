#!/bin/bash
# Deploy Phoenix Workout API to pai (192.168.1.189)
set -e

PAI_HOST="jonathan@192.168.1.189"
PAI_DEST="/home/jonathan/phoenix-api"
SERVICE_NAME="phoenix-api"
PORT=8505

echo "🚀 Deploying Phoenix Workout API to pai..."

# 1. Sync files to pai
rsync -avz --exclude '__pycache__' --exclude 'venv' \
  "$(dirname "$0")/" "$PAI_HOST:$PAI_DEST/"

# 2. Install dependencies on pai
ssh "$PAI_HOST" "
  cd $PAI_DEST
  python3 -m venv venv 2>/dev/null || true
  source venv/bin/activate
  pip install -r requirements.txt -q
  echo '✅ Dependencies installed'
"

# 3. Create systemd service if it doesn't exist
ssh "$PAI_HOST" "
  if [ ! -f /etc/systemd/system/${SERVICE_NAME}.service ]; then
    sudo tee /etc/systemd/system/${SERVICE_NAME}.service > /dev/null << 'SVCEOF'
[Unit]
Description=Phoenix Workout API
After=network.target

[Service]
Type=simple
User=jonathan
WorkingDirectory=${PAI_DEST}
ExecStart=${PAI_DEST}/venv/bin/python main.py
Restart=on-failure
RestartSec=5
Environment=PORT=${PORT}
Environment=HOST=0.0.0.0

[Install]
WantedBy=multi-user.target
SVCEOF
    sudo systemctl daemon-reload
    sudo systemctl enable ${SERVICE_NAME}
    echo '✅ Service created'
  fi
"

# 4. Restart the service
ssh "$PAI_HOST" "
  sudo systemctl restart ${SERVICE_NAME}
  sleep 2
  sudo systemctl status ${SERVICE_NAME} --no-pager | head -20
"

echo ""
echo "✅ Phoenix API deployed!"
echo "   Local:   http://192.168.1.189:${PORT}"
echo "   Health:  http://192.168.1.189:${PORT}/health"
echo "   Docs:    http://192.168.1.189:${PORT}/docs"
