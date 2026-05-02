#!/bin/bash

# TalkAR Mobile Development Setup Script
# This script sets up ADB port forwarding for local development

echo "🔧 TalkAR Mobile Dev Setup"
echo "=========================="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: adb is not installed or not in PATH"
    echo "   Please install Android SDK Platform Tools"
    exit 1
fi

# Check if device is connected
echo "📱 Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | tr -d ' ')

if [ "$DEVICES" -eq "0" ]; then
    echo "❌ No devices connected via USB"
    echo "   Please connect your Android device and enable USB debugging"
    exit 1
fi

echo "✅ Found $DEVICES device(s) connected"
echo ""

# Setup port forwarding
echo "🔌 Setting up port forwarding..."
echo "   Backend (4000) → localhost:4000"

adb reverse tcp:4000 tcp:4000

if [ $? -eq 0 ]; then
    echo "✅ Port forwarding successful!"
    echo ""
    echo "📝 Your phone can now access:"
    echo "   http://localhost:4000 → Your Mac's backend"
    echo ""
    echo "🚀 You're ready to run the app!"
    echo ""
    echo "💡 Tip: Run this script again if you reconnect your device"
else
    echo "❌ Port forwarding failed"
    echo "   Make sure USB debugging is authorized on your device"
    exit 1
fi
