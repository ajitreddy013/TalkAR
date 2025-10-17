#!/usr/bin/env bash
set -euo pipefail

# Interactive .env updater for rotated Supabase keys
# Run this AFTER generating new keys in Supabase Dashboard

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

echo "============================================"
echo "🔑 Supabase Key Rotation - .env Updater"
echo "============================================"
echo ""
echo "Current .env location: $ENV_FILE"
echo ""

echo "⚠️  WARNING: Supabase secret keys (anon, service_role) must ONLY be used in backend/server code."
echo "   Never expose these keys in frontend, mobile, or client-side code."
echo "   service_role key is highly privileged—never expose to clients."
echo "   If you use Supabase in the frontend, use only the anon key and restrict its permissions in Supabase Dashboard."
echo ""

if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Error: .env file not found!"
    echo "Creating new .env from .env.example..."
    cp "$ROOT_DIR/.env.example" "$ENV_FILE"
fi

echo "📋 INSTRUCTIONS:"
echo ""
echo "1. Open Supabase Dashboard: https://app.supabase.com"
echo "2. Select your project"
echo "3. Go to: Project Settings → API"
echo "4. Click 'Generate new anon key' and copy it"
echo "5. Click 'Generate new service role key' and copy it"
echo ""
echo "============================================"
echo ""

# Function to update or add env variable
update_env_var() {
    local key="$1"
    local value="$2"
    
    if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
        # Update existing
        sed -i.bak "s|^${key}=.*|${key}=${value}|" "$ENV_FILE" && rm -f "$ENV_FILE.bak"
    else
        # Add new
        echo "${key}=${value}" >> "$ENV_FILE"
    fi
}

# Collect new keys
echo "Enter your NEW Supabase ANON key:"
read -r NEW_ANON_KEY

echo ""
echo "Enter your NEW Supabase SERVICE_ROLE key:"
read -r NEW_SERVICE_ROLE_KEY

echo ""
echo "Enter your Supabase project URL (https://xxxxx.supabase.co):"
read -r SUPABASE_URL

# Validate inputs
if [ -z "$NEW_ANON_KEY" ] || [ -z "$NEW_SERVICE_ROLE_KEY" ] || [ -z "$SUPABASE_URL" ]; then
    echo "❌ Error: All fields are required!"
    exit 1
fi

# Update .env
echo ""
echo "Updating .env file..."

update_env_var "SUPABASE_URL" "$SUPABASE_URL"
update_env_var "SUPABASE_ANON_KEY" "$NEW_ANON_KEY"
update_env_var "SUPABASE_SERVICE_ROLE_KEY" "$NEW_SERVICE_ROLE_KEY"
update_env_var "REACT_APP_SUPABASE_URL" "$SUPABASE_URL"
update_env_var "REACT_APP_SUPABASE_ANON_KEY" "$NEW_ANON_KEY"

echo ""
echo "✅ Local .env updated successfully!"
echo ""

# Show current values (masked)
echo "Current values in .env:"
echo "  SUPABASE_URL: $SUPABASE_URL"
echo "  SUPABASE_ANON_KEY: ${NEW_ANON_KEY:0:20}...${NEW_ANON_KEY: -20}"
echo "  SUPABASE_SERVICE_ROLE_KEY: ${NEW_SERVICE_ROLE_KEY:0:20}...${NEW_SERVICE_ROLE_KEY: -20}"
echo ""

echo "============================================"
echo "✅ Step 1 Complete: Local .env updated"
echo "============================================"
echo ""
echo "NEXT STEPS:"
echo ""
echo "2. Update Production Environment Variables:"
echo "   - Update in your deployment platform (Vercel/AWS/etc.)"
echo "   - Update in CI/CD secrets (GitHub Actions)"
echo ""
echo "3. Redeploy Applications:"
echo "   cd backend && npm run build && npm start"
echo "   cd admin-dashboard && npm run build"
echo ""
echo "4. Test the new keys:"
echo "   curl '$SUPABASE_URL/rest/v1/' \\"
echo "     -H 'apikey: $NEW_ANON_KEY' \\"
echo "     -H 'Authorization: Bearer $NEW_ANON_KEY'"
echo ""
echo "5. Run audit script:"
echo "   bash scripts/audit-supabase-logs.sh"
echo ""
echo "6. Scrub Git history:"
echo "   bash scripts/scrub-git-history.sh"
echo ""
echo "For detailed instructions, see:"
echo "  - URGENT_SECURITY_RESPONSE.md"
echo "  - SECURITY_INCIDENT_RESPONSE.md"
echo ""
