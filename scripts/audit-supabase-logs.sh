#!/usr/bin/env bash
set -euo pipefail

# Quick audit script to check Supabase logs for suspicious activity
# Run this after rotating keys to identify any unauthorized access

echo "============================================"
echo "Supabase Security Audit"
echo "============================================"
echo ""

# Color codes
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}This script helps you audit for suspicious activity.${NC}"
echo ""
echo "Manual steps required:"
echo ""

echo "1. LOGIN TO SUPABASE DASHBOARD:"
echo "   https://app.supabase.com"
echo ""

echo "2. NAVIGATE TO LOGS:"
echo "   Select your project → Logs"
echo ""

echo "3. CHECK EACH SERVICE FOR SUSPICIOUS ACTIVITY:"
echo ""

echo -e "${YELLOW}   API LOGS:${NC}"
echo "   - Look for requests after key rotation using old keys (should fail)"
echo "   - Check for unusual IP addresses"
echo "   - Look for high-volume requests from single IPs"
echo "   - Filter by status codes: 401 (unauthorized) might indicate old key usage"
echo ""

echo -e "${YELLOW}   AUTH LOGS:${NC}"
echo "   - Check for failed login attempts"
echo "   - Look for unusual geographic locations"
echo "   - Check for account enumeration attempts"
echo ""

echo -e "${YELLOW}   DATABASE LOGS:${NC}"
echo "   - Look for suspicious queries:"
echo "     * SELECT * FROM (pulling all data)"
echo "     * DROP/DELETE operations"
echo "     * ALTER TABLE commands"
echo "     * Queries accessing sensitive tables"
echo ""

echo -e "${YELLOW}   FUNCTIONS LOGS:${NC}"
echo "   - Check for unauthorized function invocations"
echo "   - Look for errors that might indicate exploitation attempts"
echo ""

echo "4. EXPORT LOGS FOR ANALYSIS:"
echo "   - Download logs from the exposure period"
echo "   - Timeframe: Check from when secret was first committed to GitHub"
echo "   - Command to find first exposure:"
echo "     git log --all --oneline -- docker-compose.yml | tail -1"
echo ""

echo "5. CHECK FOR SPECIFIC PATTERNS:"
echo ""

echo -e "${YELLOW}   Suspicious SQL patterns to look for:${NC}"
cat << 'EOF'
   - Information schema queries (attacker reconnaissance):
     SELECT * FROM information_schema.tables
     SELECT * FROM pg_catalog.pg_tables
   
   - Mass data extraction:
     SELECT * FROM users
     SELECT * FROM sensitive_table WHERE 1=1
   
   - Privilege escalation attempts:
     ALTER USER ... WITH SUPERUSER
     GRANT ALL PRIVILEGES
   
   - Backdoor creation:
     CREATE FUNCTION ... LANGUAGE plpgsql SECURITY DEFINER
     INSERT INTO admin_users ...
EOF
echo ""

echo "6. CHECK GITHUB COMMIT HISTORY:"
echo "   When was the secret first exposed?"
echo ""
git log --oneline --all -- docker-compose.yml | head -20
echo ""

echo "7. POSTGRES DIRECT AUDIT (if you have psql access):"
echo ""
cat << 'EOSQL'
-- Connect to your database:
-- psql "postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres"

-- Check recent connections:
SELECT 
  datname,
  usename,
  application_name,
  client_addr,
  backend_start,
  state
FROM pg_stat_activity
WHERE backend_start > NOW() - INTERVAL '7 days'
ORDER BY backend_start DESC;

-- Check for recent table modifications:
SELECT 
  schemaname,
  tablename,
  last_vacuum,
  last_autovacuum,
  last_analyze,
  n_tup_ins,
  n_tup_upd,
  n_tup_del
FROM pg_stat_user_tables
WHERE n_tup_del > 0 OR n_tup_upd > 100
ORDER BY n_tup_del DESC;

-- Check for suspicious function calls:
SELECT 
  funcname,
  calls,
  total_time,
  self_time
FROM pg_stat_user_functions
ORDER BY calls DESC;
EOSQL
echo ""

echo "8. CHECK AWS S3 LOGS (if credentials were exposed):"
echo "   - Review CloudTrail logs for unusual S3 access"
echo "   - Check for data exfiltration (high egress)"
echo "   - Look for bucket policy changes"
echo ""

echo "9. INDICATORS OF COMPROMISE:"
echo ""
echo -e "${RED}   High Priority Alerts:${NC}"
echo "   ⚠️  Requests after key rotation timestamp (unauthorized access)"
echo "   ⚠️  Mass data SELECT queries"
echo "   ⚠️  DROP/DELETE operations on production data"
echo "   ⚠️  New user accounts created unexpectedly"
echo "   ⚠️  Permission/role escalations"
echo "   ⚠️  Unusual geographic locations (IPs from unexpected countries)"
echo ""

echo -e "${YELLOW}   Medium Priority Alerts:${NC}"
echo "   ⚠️  High volume of failed authentication attempts"
echo "   ⚠️  Schema enumeration queries"
echo "   ⚠️  Unusual API call patterns"
echo ""

echo "10. DOCUMENT FINDINGS:"
echo "    Create an incident report with:"
echo "    - Timeline of exposure"
echo "    - Any suspicious activity found"
echo "    - Data accessed/compromised (if any)"
echo "    - Remediation actions taken"
echo ""

echo -e "${GREEN}After completing the audit:${NC}"
echo "  - If suspicious activity found: Consider it a breach, investigate further"
echo "  - If no activity found: Still complete all rotation steps as precaution"
echo "  - Document findings in security-audit-$(date +%Y%m%d).md"
echo ""

# Check if we can query Supabase via CLI (if installed)
if command -v supabase &> /dev/null; then
    echo -e "${BLUE}Supabase CLI detected. You can also use:${NC}"
    echo "  supabase projects list"
    echo "  supabase db dump"
    echo ""
fi

echo "============================================"
echo "Audit checklist saved to: ./audit-checklist.txt"
echo "============================================"

# Save checklist to file
cat > audit-checklist.txt << 'CHECKLIST'
TalkAR Security Audit Checklist
Generated: $(date)

[ ] 1. Logged into Supabase Dashboard
[ ] 2. Reviewed API logs for suspicious requests
[ ] 3. Reviewed Auth logs for failed logins
[ ] 4. Reviewed Database logs for suspicious queries
[ ] 5. Checked for requests using old keys after rotation
[ ] 6. Identified unusual IP addresses or geographic locations
[ ] 7. Checked for mass data extraction patterns
[ ] 8. Verified no DROP/DELETE/ALTER commands by unauthorized users
[ ] 9. Exported logs for the exposure period
[ ] 10. Ran PostgreSQL audit queries (if applicable)
[ ] 11. Checked AWS S3 CloudTrail logs (if credentials exposed)
[ ] 12. Documented all findings
[ ] 13. Created incident report if suspicious activity found
[ ] 14. Notified security team/stakeholders if breach detected

Findings:
---------
[Add your findings here]

Suspicious Activity: YES / NO

If YES, describe:


Next Actions:
-------------
[List follow-up actions]

Completed by: _________________
Date: _________________
CHECKLIST

echo ""
echo -e "${GREEN}✓ Audit guide complete${NC}"
echo ""
