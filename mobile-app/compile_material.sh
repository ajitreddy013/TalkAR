#!/bin/bash

set -e

echo "🔨 Filament Material Compiler"
echo "=============================="
echo ""

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Darwin*)    PLATFORM="mac";;
    Linux*)     PLATFORM="linux";;
    *)          echo "❌ Unsupported OS: ${OS}"; exit 1;;
esac

echo "📱 Platform: ${PLATFORM}"

# Filament version
FILAMENT_VERSION="1.52.0"
FILAMENT_DIR="$HOME/Downloads/filament"
MATC="${FILAMENT_DIR}/bin/matc"

# Download Filament if not exists
if [ ! -f "${MATC}" ]; then
    echo ""
    echo "📥 Downloading Filament ${FILAMENT_VERSION}..."
    
    cd ~/Downloads
    FILAMENT_URL="https://github.com/google/filament/releases/download/v${FILAMENT_VERSION}/filament-v${FILAMENT_VERSION}-${PLATFORM}.tgz"
    
    echo "   URL: ${FILAMENT_URL}"
    curl -L -O "${FILAMENT_URL}"
    
    echo "📦 Extracting..."
    tar -xzf "filament-v${FILAMENT_VERSION}-${PLATFORM}.tgz"
    
    echo "✅ Filament downloaded to ${FILAMENT_DIR}"
else
    echo "✅ Filament already installed at ${FILAMENT_DIR}"
fi

# Verify matc exists
if [ ! -f "${MATC}" ]; then
    echo "❌ matc not found at ${MATC}"
    exit 1
fi

echo ""
echo "🔧 Compiling material..."

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${SCRIPT_DIR}"

# Create output directory
mkdir -p app/src/main/assets/materials

# Material files
MAT_SOURCE="materials/video_material.mat"
MAT_OUTPUT="app/src/main/assets/materials/video_material.filamat"

# Check source exists
if [ ! -f "${MAT_SOURCE}" ]; then
    echo "❌ Material source not found: ${MAT_SOURCE}"
    exit 1
fi

# Compile
echo "   Source: ${MAT_SOURCE}"
echo "   Output: ${MAT_OUTPUT}"
echo ""

"${MATC}" \
  -a opengl \
  -p mobile \
  -o "${MAT_OUTPUT}" \
  "${MAT_SOURCE}"

# Verify output
if [ -f "${MAT_OUTPUT}" ]; then
    FILE_SIZE=$(ls -lh "${MAT_OUTPUT}" | awk '{print $5}')
    echo ""
    echo "✅ Material compiled successfully!"
    echo "   File: ${MAT_OUTPUT}"
    echo "   Size: ${FILE_SIZE}"
    echo ""
    echo "🎉 Ready to use! Build and install the app now."
else
    echo ""
    echo "❌ Compilation failed - output file not created"
    exit 1
fi
