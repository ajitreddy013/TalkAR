#!/bin/bash

# Docker Setup Verification Script for TalkAR Project
# This script verifies that all Docker services are properly configured and running

set -e

echo "🐳 Starting Docker Setup Verification for TalkAR Project"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Check if Docker is installed and running
echo "1. Checking Docker installation..."
if command -v docker &> /dev/null; then
    print_status "Docker is installed"
    if docker info &> /dev/null; then
        print_status "Docker daemon is running"
    else
        print_error "Docker daemon is not running"
        exit 1
    fi
else
    print_error "Docker is not installed"
    exit 1
fi

# Check if Docker Compose is available
echo ""
echo "2. Checking Docker Compose..."
if docker-compose --version &> /dev/null; then
    print_status "Docker Compose is available"
elif docker compose version &> /dev/null; then
    print_status "Docker Compose (v2) is available"
else
    print_error "Docker Compose is not available"
    exit 1
fi

# Verify docker-compose.yml exists
echo ""
echo "3. Checking docker-compose.yml..."
if [ -f "docker-compose.yml" ]; then
    print_status "docker-compose.yml file exists"
else
    print_error "docker-compose.yml file not found"
    exit 1
fi

# Check individual Dockerfiles
echo ""
echo "4. Checking individual Dockerfiles..."
if [ -f "backend/Dockerfile" ]; then
    print_status "Backend Dockerfile exists"
else
    print_error "Backend Dockerfile not found"
fi

if [ -f "admin-dashboard/Dockerfile" ]; then
    print_status "Admin Dashboard Dockerfile exists"
else
    print_error "Admin Dashboard Dockerfile not found"
fi

# Validate Docker compose syntax
echo ""
echo "5. Validating Docker Compose syntax..."
if docker-compose config > /dev/null 2>&1; then
    print_status "Docker Compose syntax is valid"
else
    print_error "Docker Compose syntax validation failed"
    exit 1
fi

# Check for required environment files
echo ""
echo "6. Checking environment configuration..."
if [ -f "backend/.env.example" ]; then
    print_status "Backend environment example file exists"
else
    print_warning "Backend .env.example file not found"
fi

if [ -f "admin-dashboard/.env.example" ]; then
    print_status "Admin Dashboard environment example file exists"
else
    print_warning "Admin Dashboard .env.example file not found"
fi

# Check for Supabase configuration
echo ""
echo "7. Checking Supabase integration..."
if grep -q "supabase" docker-compose.yml; then
    print_status "Supabase service is configured in docker-compose.yml"
else
    print_warning "Supabase service not found in docker-compose.yml"
fi

if grep -q "SUPABASE_URL" docker-compose.yml; then
    print_status "Supabase environment variables are configured"
else
    print_warning "Supabase environment variables not found"
fi

# Test building Docker images
echo ""
echo "8. Testing Docker image builds..."
echo "This may take a few minutes..."

if docker-compose build --no-cache > /dev/null 2>&1; then
    print_status "All Docker images built successfully"
else
    print_error "Docker image build failed"
    exit 1
fi

# Check port availability
echo ""
echo "9. Checking port availability..."
check_port() {
    local port=$1
    local service=$2
    if ! netstat -tuln 2>/dev/null | grep -q ":$port "; then
        print_status "Port $port is available for $service"
    else
        print_warning "Port $port is already in use for $service"
    fi
}

check_port 3000 "Backend API"
check_port 3001 "Admin Dashboard"
check_port 5432 "PostgreSQL"
check_port 8000 "Supabase API"
check_port 8001 "Supabase Studio"
check_port 3002 "Supabase Studio Web Interface"

# Summary
echo ""
echo "=================================================="
echo "Docker Setup Verification Complete!"
echo ""
echo "Next steps:"
echo "1. Copy .env.example files to .env and configure your environment variables"
echo "2. Run: docker-compose up -d"
echo "3. Access services:"
echo "   - Backend API: http://localhost:3000"
echo "   - Admin Dashboard: http://localhost:3001"
echo "   - Supabase Studio: http://localhost:3002"
echo "   - Supabase API: http://localhost:8000"
echo ""
echo "To stop all services: docker-compose down"
echo "To view logs: docker-compose logs -f"
echo ""
print_status "Docker configuration is ready for deployment!"