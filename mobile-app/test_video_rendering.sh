#!/bin/bash

# Test script for 3D video rendering diagnostics
# This script helps identify which break point is failing

echo "=========================================="
echo "TalkAR 3D Video Rendering Test"
echo "=========================================="
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected!"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo "✅ Device connected"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c
echo "✅ Logs cleared"
echo ""

# Start log monitoring in background
echo "Starting log monitor..."
LOG_FILE="talkar_test_$(date +%Y%m%d_%H%M%S).log"
adb logcat | grep -E "TalkAR|VideoPlane|IMAGE|VIDEO NODE" > "$LOG_FILE" &
LOG_PID=$!
echo "✅ Logging to: $LOG_FILE"
echo ""

# Launch app
echo "Launching TalkAR app..."
adb shell am start -n com.talkar.app/.MainActivity
echo "✅ App launched"
echo ""

echo "=========================================="
echo "TEST INSTRUCTIONS"
echo "=========================================="
echo "1. Point camera at Sunrich poster"
echo "2. Wait for 'Detected: sunrich' message"
echo "3. Long-press on the poster"
echo "4. Wait for video to play"
echo ""
echo "Press ENTER when test is complete..."
read

# Stop log monitoring
kill $LOG_PID 2>/dev/null

echo ""
echo "=========================================="
echo "ANALYZING LOGS"
echo "=========================================="
echo ""

# Check each break point
BP1=$(grep -c "NEW IMAGE DETECTED" "$LOG_FILE")
BP2=$(grep -c "VIDEO NODE CREATED AND STORED" "$LOG_FILE")
BP3=$(grep -c "IMAGE LONG-PRESSED" "$LOG_FILE")
BP4=$(grep -c "State updated with.*video URI" "$LOG_FILE")
BP5=$(grep -c "LaunchedEffect triggered" "$LOG_FILE")
BP6=$(grep -c "PLAYING VIDEO ON AR PLANE" "$LOG_FILE")
BP7=$(grep -c "Loading video:" "$LOG_FILE")
BP8=$(grep -c "Video prepared successfully" "$LOG_FILE")
BP9=$(grep -c "Creating 3D video plane" "$LOG_FILE")
BP10=$(grep -c "3D video plane created successfully" "$LOG_FILE")

# Display results
echo "Break Point Results:"
echo ""
if [ $BP1 -gt 0 ]; then echo "✅ BP1: Image Detection"; else echo "❌ BP1: Image Detection"; fi
if [ $BP2 -gt 0 ]; then echo "✅ BP2: VideoNode Creation"; else echo "❌ BP2: VideoNode Creation"; fi
if [ $BP3 -gt 0 ]; then echo "✅ BP3: Long Press"; else echo "❌ BP3: Long Press"; fi
if [ $BP4 -gt 0 ]; then echo "✅ BP4: Video URI Set"; else echo "❌ BP4: Video URI Set"; fi
if [ $BP5 -gt 0 ]; then echo "✅ BP5: LaunchedEffect"; else echo "❌ BP5: LaunchedEffect"; fi
if [ $BP6 -gt 0 ]; then echo "✅ BP6: VideoNode Found"; else echo "❌ BP6: VideoNode Found"; fi
if [ $BP7 -gt 0 ]; then echo "✅ BP7: loadVideo Called"; else echo "❌ BP7: loadVideo Called"; fi
if [ $BP8 -gt 0 ]; then echo "✅ BP8: Video Prepared"; else echo "❌ BP8: Video Prepared"; fi
if [ $BP9 -gt 0 ]; then echo "✅ BP9: 3D Plane Creation Started"; else echo "❌ BP9: 3D Plane Creation Started"; fi
if [ $BP10 -gt 0 ]; then echo "✅ BP10: 3D Plane Created"; else echo "❌ BP10: 3D Plane Created"; fi

echo ""
echo "=========================================="

# Find first failure
if [ $BP1 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 1 (Image Detection)"
    echo "Issue: AR not detecting images"
    echo "Check: Lighting, poster visibility, ARCore"
elif [ $BP2 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 2 (VideoNode Creation)"
    echo "Issue: VideoPlaneNode not being created"
    echo "Check: Anchor creation, exceptions"
elif [ $BP3 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 3 (Long Press)"
    echo "Issue: Gesture not detected"
    echo "Check: Touch events, press duration"
elif [ $BP4 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 4 (Video URI)"
    echo "Issue: ViewModel not setting video URI"
    echo "Check: State updates, API calls"
elif [ $BP5 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 5 (LaunchedEffect)"
    echo "Issue: Compose not recomposing"
    echo "Check: State collection, videoUriToPlay"
elif [ $BP6 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 6 (VideoNode Retrieval)"
    echo "Issue: VideoNode not in map"
    echo "Check: videoNodesRef, image name matching"
elif [ $BP7 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 7 (loadVideo Call)"
    echo "Issue: loadVideo() not executing"
    echo "Check: Coroutine scope, exceptions"
elif [ $BP8 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 8 (Video Preparation)"
    echo "Issue: MediaPlayer not preparing"
    echo "Check: Video file, format, MediaPlayer errors"
elif [ $BP9 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 9 (3D Plane Creation)"
    echo "Issue: createVideoPlane() not called"
    echo "Check: onPrepared callback, exceptions"
elif [ $BP10 -eq 0 ]; then
    echo "❌ FAILED AT: Break Point 10 (3D Plane Completion)"
    echo "Issue: Filament rendering incomplete"
    echo "Check: Material, texture, entity creation"
else
    echo "✅ ALL BREAK POINTS PASSED!"
    echo "If video still not visible, check:"
    echo "- Material shader"
    echo "- Texture updates"
    echo "- Entity visibility"
fi

echo "=========================================="
echo ""

# Check for errors
ERRORS=$(grep -i "exception\|error\|failed" "$LOG_FILE" | grep -v "GetRecentDevicePose\|hit_test\|No point hit" | head -10)
if [ ! -z "$ERRORS" ]; then
    echo "⚠️ ERRORS FOUND:"
    echo "$ERRORS"
    echo ""
fi

echo "Full log saved to: $LOG_FILE"
echo ""
echo "To view full log:"
echo "  cat $LOG_FILE"
echo ""
echo "To search for specific text:"
echo "  grep 'search_term' $LOG_FILE"
echo ""
