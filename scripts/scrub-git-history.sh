#!/usr/bin/env bash
set -euo pipefail

# Git History Scrubbing Script for TalkAR Security Incident
# This script removes leaked secrets from Git history using git-filter-repo

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "============================================"
echo "TalkAR Security - Git History Scrubbing"
echo "============================================"
echo ""

# Color codes for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Check if git-filter-repo is installed
if ! command -v git-filter-repo &> /dev/null; then
    echo -e "${RED}ERROR: git-filter-repo is not installed${NC}"
    echo ""
    echo "Install it with:"
    echo "  macOS:   brew install git-filter-repo"
    echo "  Linux:   pip install git-filter-repo"
    echo "  Windows: pip install git-filter-repo"
    echo ""
    exit 1
fi

# Check if we're in a git repository
if [ ! -d .git ]; then
    echo -e "${RED}ERROR: Not in a git repository${NC}"
    exit 1
fi

# Warn about destructive operation
echo -e "${YELLOW}⚠️  WARNING: This operation will rewrite Git history!${NC}"
echo ""
echo "This script will:"
echo "  1. Create a backup of your repository"
echo "  2. Remove leaked secrets from all commits"
echo "  3. Prepare for force-push to remote"
echo ""
echo "Before proceeding:"
echo "  - Ensure you have rotated all leaked keys"
echo "  - Notify all team members about the upcoming force-push"
echo "  - Ensure you have edited scripts/git-scrub-secrets.txt with actual leaked values"
echo ""

read -p "Have you completed the above? Type 'yes' to continue: " confirm
if [ "$confirm" != "yes" ]; then
    echo "Aborting."
    exit 1
fi

# Create backup
BACKUP_DIR="$HOME/TalkAR-backup-$(date +%Y%m%d-%H%M%S)"
echo ""
echo -e "${GREEN}Creating backup at: $BACKUP_DIR${NC}"
git clone "$REPO_ROOT" "$BACKUP_DIR"
echo "Backup created successfully!"

# Check if secrets file exists
SECRETS_FILE="$REPO_ROOT/scripts/git-scrub-secrets.txt"
if [ ! -f "$SECRETS_FILE" ]; then
    echo -e "${RED}ERROR: Secrets file not found at $SECRETS_FILE${NC}"
    echo "Create it first with the actual leaked values."
    exit 1
fi

# Preview what will be replaced
echo ""
echo "Secrets that will be redacted:"
cat "$SECRETS_FILE"
echo ""

read -p "Does this look correct? Type 'yes' to continue: " confirm
if [ "$confirm" != "yes" ]; then
    echo "Aborting. Edit $SECRETS_FILE and run again."
    exit 1
fi

# Get current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo ""
echo "Current branch: $CURRENT_BRANCH"

# Show repository status
echo ""
echo "Current repository status:"
git status --short

if [ -n "$(git status --porcelain)" ]; then
    echo -e "${YELLOW}⚠️  You have uncommitted changes!${NC}"
    read -p "Commit or stash them first. Continue anyway? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "Aborting."
        exit 1
    fi
fi

# Execute git-filter-repo
echo ""
echo -e "${GREEN}Executing git-filter-repo...${NC}"
echo "This may take a few minutes depending on repository size."
echo ""

git filter-repo --replace-text "$SECRETS_FILE" --force

echo ""
echo -e "${GREEN}✓ Git history rewrite complete!${NC}"
echo ""

# Verify secrets are gone
echo "Verifying secrets removal..."
echo ""

# Search for common secret patterns (should not find any now)
if git log -p | grep -qi "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" | grep -v "REDACTED"; then
    echo -e "${RED}⚠️  Warning: Found potential JWT tokens in history${NC}"
else
    echo -e "${GREEN}✓ No JWT tokens found${NC}"
fi

if git log -p docker-compose.yml | grep -i "service_role" | grep -v "REDACTED" | grep -v "SERVICE_ROLE_KEY"; then
    echo -e "${RED}⚠️  Warning: Found service_role references in docker-compose.yml history${NC}"
else
    echo -e "${GREEN}✓ No service_role secrets found in docker-compose.yml history${NC}"
fi

echo ""
echo -e "${YELLOW}============================================${NC}"
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "${YELLOW}============================================${NC}"
echo ""
echo "1. Review the changes:"
echo "   git log -p | less"
echo ""
echo "2. Force-push to remote (⚠️  DESTRUCTIVE):"
echo "   git remote add origin git@github.com:ajitreddy013/TalkAR.git"
echo "   git push origin --force --all"
echo "   git push origin --force --tags"
echo ""
echo "3. Notify ALL collaborators to:"
echo "   - Backup their local changes"
echo "   - Delete their local clone"
echo "   - Re-clone the repository:"
echo "     git clone git@github.com:ajitreddy013/TalkAR.git"
echo ""
echo "4. Verify on GitHub that secrets are removed:"
echo "   https://github.com/ajitreddy013/TalkAR/commits/main"
echo ""
echo -e "${GREEN}Backup location: $BACKUP_DIR${NC}"
echo ""
echo "If something goes wrong, restore from backup:"
echo "  cd $BACKUP_DIR"
echo "  git push origin --force --all"
echo ""
