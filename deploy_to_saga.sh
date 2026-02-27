#!/bin/bash
# deploy_to_saga.sh — rebuild APK and serve it for Saga sideload
# Usage: ./deploy_to_saga.sh
# Then open http://192.168.1.215:8765 on Saga and tap the APK

set -e

JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_DIR="$PROJECT_DIR/androidApp/build/outputs/apk/debug"
PORT=8765

echo "🔨 Building APK..."
cd "$PROJECT_DIR"
JAVA_HOME="$JAVA_HOME" ./gradlew :androidApp:assembleDebug --quiet

echo "✅ Build complete: $APK_DIR/androidApp-debug.apk"

# Kill any existing server on that port
lsof -ti:$PORT | xargs kill -9 2>/dev/null || true

echo "📡 Serving on http://192.168.1.215:$PORT"
echo "   → Open that URL on Saga, tap the APK, install"
echo "   → Ctrl+C when done"
cd "$APK_DIR"
python3 -m http.server $PORT
