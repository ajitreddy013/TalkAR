#!/bin/bash

# Week 4 Phase 1: 3D Avatar Integration Verification Script
# This script verifies that all 3D avatar components are properly integrated

echo "========================================"
echo "Week 4 Phase 1: 3D Avatar Verification"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project root
PROJECT_ROOT="/Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR -"
MOBILE_APP="$PROJECT_ROOT/mobile-app"

# Verification counters
PASSED=0
FAILED=0

# Function to check file exists
check_file() {
    local file=$1
    local description=$2
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $description"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $description"
        echo -e "  ${YELLOW}Missing: $file${NC}"
        ((FAILED++))
        return 1
    fi
}

# Function to check directory exists
check_dir() {
    local dir=$1
    local description=$2
    
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $description"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $description"
        echo -e "  ${YELLOW}Missing: $dir${NC}"
        ((FAILED++))
        return 1
    fi
}

# Function to check file contains text
check_file_contains() {
    local file=$1
    local text=$2
    local description=$3
    
    if [ -f "$file" ] && grep -q "$text" "$file"; then
        echo -e "${GREEN}✓${NC} $description"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $description"
        ((FAILED++))
        return 1
    fi
}

echo "=== 1. Directory Structure ==="
check_dir "$MOBILE_APP/app/src/main/res/raw" "res/raw directory for 3D models"
check_dir "$MOBILE_APP/app/src/main/java/com/talkar/app/ar" "AR module directory"
check_dir "$MOBILE_APP/app/src/main/java/com/talkar/app/data/models" "Data models directory"
echo ""

echo "=== 2. Core Files ==="
check_file "$MOBILE_APP/app/src/main/java/com/talkar/app/data/models/AvatarModel3D.kt" "AvatarModel3D data class"
check_file "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "AvatarManager module"
check_file "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/Avatar3DRenderer.kt" "Avatar3DRenderer"
check_file "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/ImageAnchorManager.kt" "ImageAnchorManager (updated)"
echo ""

echo "=== 3. Dependencies ==="
check_file_contains "$MOBILE_APP/app/build.gradle" "com.google.ar.sceneform:core" "Sceneform core dependency"
check_file_contains "$MOBILE_APP/app/build.gradle" "com.google.ar.sceneform:animation" "Sceneform animation dependency"
check_file_contains "$MOBILE_APP/app/build.gradle" "com.google.android.filament:filament-android" "Filament rendering engine"
echo ""

echo "=== 4. Data Model Validation ==="
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/data/models/AvatarModel3D.kt" "IdleAnimation" "IdleAnimation enum defined"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/data/models/AvatarModel3D.kt" "AvatarType" "AvatarType enum defined"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/data/models/AvatarModel3D.kt" "Position3D" "Position3D data class"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/data/models/AvatarModel3D.kt" "Rotation3D" "Rotation3D data class"
echo ""

echo "=== 5. AvatarManager Features ==="
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "registerAvatar" "registerAvatar method"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "loadAvatarForImage" "loadAvatarForImage method"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "mapImageToAvatar" "mapImageToAvatar method"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "renderableCache" "Renderable caching"
echo ""

echo "=== 6. Idle Animations ==="
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "startBreathingAnimation" "Breathing animation"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "startBlinkingAnimation" "Blinking animation"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/AvatarManager.kt" "applyIdleAnimation" "Apply idle animation method"
echo ""

echo "=== 7. Avatar3DRenderer Features ==="
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/Avatar3DRenderer.kt" "ArSceneView" "ARCore SceneView integration"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/Avatar3DRenderer.kt" "onFrameUpdate" "Frame update callback"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/Avatar3DRenderer.kt" "handleImageTracking" "Image tracking handler"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ar/Avatar3DRenderer.kt" "AugmentedImage" "AugmentedImage support"
echo ""

echo "=== 8. ViewModel Integration ==="
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ui/viewmodels/SimpleARViewModel.kt" "AvatarManager" "AvatarManager import"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ui/viewmodels/SimpleARViewModel.kt" "initializeAvatarManager" "AvatarManager initialization"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ui/viewmodels/SimpleARViewModel.kt" "registerDefaultAvatars" "Default avatars registration"
check_file_contains "$MOBILE_APP/app/src/main/java/com/talkar/app/ui/viewmodels/SimpleARViewModel.kt" "AvatarLoadState" "Avatar load state tracking"
echo ""

echo "=== 9. Documentation ==="
check_file "$MOBILE_APP/app/src/main/res/raw/README.md" "3D models README"
echo ""

echo "========================================"
echo "Verification Summary"
echo "========================================"
echo -e "Passed: ${GREEN}${PASSED}${NC}"
echo -e "Failed: ${RED}${FAILED}${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Week 4 Phase 1 implementation is complete.${NC}"
    echo ""
    echo "Next Steps:"
    echo "1. Add sample GLB/GLTF 3D models to mobile-app/app/src/main/res/raw/"
    echo "2. Update avatar configurations with actual model resource IDs"
    echo "3. Test avatar loading with real AR image detection"
    echo "4. Fine-tune avatar scale, position, and rotation offsets"
    echo "5. Test idle animations (breathing/blinking)"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please review the missing components.${NC}"
    exit 1
fi
