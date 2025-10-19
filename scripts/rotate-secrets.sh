#!/bin/bash

# Script to rotate secrets in the TalkAR project
# This script helps generate new secrets and update environment files

set -e  # Exit on any error

echo "🔐 TalkAR Secret Rotation Script"
echo "================================="

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to generate a secure random string
generate_secret() {
  local length=${1:-32}
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 $length | tr -d "=+/" | cut -c1-$length
  elif command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr -d "-" | cut -c1-$length
  else
    # Fallback - not as secure but works
    cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w $length | head -n 1
  fi
}

# Function to update .env file
update_env_file() {
  local file_path=$1
  local key=$2
  local value=$3
  
  if [ -f "$file_path" ]; then
    if grep -q "^${key}=" "$file_path"; then
      # Update existing key
      sed -i.bak "s|^${key}=.*|${key}=${value}|" "$file_path"
      rm "${file_path}.bak"
    else
      # Add new key
      echo "${key}=${value}" >> "$file_path"
    fi
    echo -e "${GREEN}✓ Updated ${file_path}${NC}"
  else
    echo -e "${YELLOW}⚠ File not found: ${file_path}${NC}"
  fi
}

# Check if we're in the right directory
if [ ! -f "package.json" ] && [ ! -f "README.md" ]; then
  echo -e "${RED}Error: Please run this script from the project root directory${NC}"
  exit 1
fi

echo ""
echo -e "${BLUE}This script will help you rotate secrets in your TalkAR project.${NC}"
echo -e "${BLUE}It will generate new secure secrets and update your environment files.${NC}"
echo ""

# Confirm before proceeding
read -p "Do you want to proceed with secret rotation? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Operation cancelled."
  exit 0
fi

echo ""
echo -e "${YELLOW}Generating new secrets...${NC}"

# Generate new secrets
NEW_JWT_SECRET=$(generate_secret 48)
NEW_SUPABASE_JWT_SECRET=$(generate_secret 48)

echo ""
echo -e "${GREEN}✓ Generated new JWT secret${NC}"
echo -e "${GREEN}✓ Generated new Supabase JWT secret${NC}"

echo ""
echo -e "${YELLOW}Updating environment files...${NC}"

# Update root .env file
update_env_file ".env" "JWT_SECRET" "$NEW_JWT_SECRET"
update_env_file ".env" "SUPABASE_JWT_SECRET" "$NEW_SUPABASE_JWT_SECRET"

# Update backend .env file
update_env_file "backend/.env" "JWT_SECRET" "$NEW_JWT_SECRET"
update_env_file "backend/.env" "SUPABASE_JWT_SECRET" "$NEW_SUPABASE_JWT_SECRET"

# Update example files
update_env_file ".env.example" "JWT_SECRET" "change-me-super-strong-at-least-32-chars"
update_env_file ".env.example" "SUPABASE_JWT_SECRET" "change-me-super-strong-at-least-32-chars"
update_env_file "backend/.env.example" "JWT_SECRET" "change-me-super-strong-at-least-32-chars"
update_env_file "backend/.env.example" "SUPABASE_JWT_SECRET" "change-me-super-strong-at-least-32-chars"

echo ""
echo -e "${GREEN}✅ Secret rotation completed!${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update your Supabase project with new JWT secrets in the Supabase Dashboard"
echo "2. Update any other deployment environments (Vercel, Netlify, etc.)"
echo "3. Restart your services to use the new secrets"
echo "4. Update your team with the new secrets"
echo ""
echo -e "${RED}Important:${NC}"
echo "- Store the new secrets securely"
echo "- Never commit real secrets to the repository"
echo "- Update your deployment platforms with the new secrets"
echo ""

read -p "Do you want to run a security audit to verify the changes? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  echo -e "${BLUE}Running security audit...${NC}"
  node security-audit.js
fi

echo ""
echo -e "${GREEN}Secret rotation process completed!${NC}"