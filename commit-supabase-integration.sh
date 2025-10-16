#!/bin/bash

# TalkAR Supabase Integration Commit Script
# This script stages and commits the Supabase integration changes

echo "🚀 Preparing to commit Supabase integration..."

# Check if we're in the right directory
if [ ! -f "package.json" ] || [ ! -d "backend" ] || [ ! -d "admin-dashboard" ]; then
    echo "❌ Error: Please run this script from the TalkAR project root directory"
    exit 1
fi

echo "📋 Current git status:"
git status --porcelain

echo ""
echo "🔍 Checking for sensitive data..."

# Check for potential hardcoded secrets in files we're about to commit
if grep -r "sk-" backend/src/ admin-dashboard/src/ supabase/ 2>/dev/null; then
    echo "⚠️  Warning: Found potential secrets in source files"
    echo "Please review the files above before continuing"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "📦 Staging files for commit..."

# Stage new files
echo "Adding new Supabase integration files..."
git add SUPABASE_SETUP_GUIDE.md
git add admin-dashboard/.env.example
git add admin-dashboard/src/hooks/
git add admin-dashboard/src/services/supabase.ts
git add admin-dashboard/test-supabase-frontend.js
git add backend/src/config/supabase.ts
git add backend/src/indexSupabase.ts
git add backend/src/routes/authSupabase.ts
git add backend/src/routes/syncSupabase.ts
git add backend/src/services/authServiceSupabase.ts
git add backend/src/services/supabaseService.ts
git add backend/src/services/syncServiceSupabase.ts
git add backend/test-backend-api-supabase.js
git add backend/test-supabase-integration.js
git add supabase/

# Stage modified files
echo "Adding modified configuration files..."
git add backend/env.example
git add backend/src/index.ts
git add backend/src/routes/scripts.ts

# Stage package.json files (but not package-lock.json)
echo "Adding package.json files..."
git add admin-dashboard/package.json
git add backend/package.json

echo ""
echo "📊 Files staged for commit:"
git diff --cached --name-only

echo ""
echo "📝 Creating commit..."

# Create the commit
git commit -m "feat: Add comprehensive Supabase integration

- Implement Supabase authentication service with JWT token support
- Add Supabase-based sync service with enhanced video generation
- Create Supabase API routes for auth and sync operations
- Add frontend Supabase integration with React hooks and services
- Include comprehensive test suites for backend and frontend
- Add database schema migrations for user profiles, projects, and sync jobs
- Create setup guide and configuration templates
- Update existing services to support Supabase integration
- Add real-time subscription support
- Implement proper error handling and validation

Security Features:
- Environment variable based configuration
- JWT token authentication
- Input validation and sanitization
- Role-based access control
- Secure password handling

Testing:
- Backend integration tests
- Frontend component tests
- End-to-end API testing
- Database schema validation"

echo ""
echo "✅ Commit created successfully!"
echo ""
echo "🎯 Next steps:"
echo "1. Review the commit: git show HEAD"
echo "2. Push to GitHub: git push origin main"
echo "3. Set up environment variables in your deployment platform"
echo "4. Run database migrations: cd backend && npm run migrate"
echo "5. Test the integration: cd backend && node test-supabase-integration.js"
echo ""
echo "📚 Documentation:"
echo "- Setup Guide: SUPABASE_SETUP_GUIDE.md"
echo "- Git Push Guide: GIT_PUSH_GUIDE.md"
echo "- API Documentation: docs/API.md"
echo ""
echo "🎉 Supabase integration is ready for deployment!"