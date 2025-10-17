#!/bin/bash
echo "🔍 Checking Backend Images Configuration"
echo "=========================================="
echo ""

# Check if backend is running
if curl -s http://localhost:3000/api/v1/images > /dev/null 2>&1; then
    echo "✅ Backend is RUNNING"
    echo ""
    echo "📊 Images in Database:"
    curl -s http://localhost:3000/api/v1/images | jq -r '.[] | "   • \(.name) (ID: \(.id))"' 2>/dev/null || echo "   (Unable to parse - check if backend is running)"
else
    echo "❌ Backend is NOT RUNNING"
    echo ""
    echo "📝 To start backend:"
    echo "   cd backend && npm install && npm start"
fi

echo ""
echo "=========================================="
echo ""
echo "💡 Currently ARCore uses TEST IMAGES:"
echo "   • test_image"
echo "   • test_image_2"  
echo "   • test_image_3"
echo ""
echo "🎯 For real detection, we need to:"
echo "   1. Ensure Sunrich image is in backend database"
echo "   2. Update ARCore to load Sunrich image"
echo "   3. Point camera at Sunrich bottle"
