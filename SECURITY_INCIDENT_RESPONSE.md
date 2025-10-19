# Security Incident Response - Leaked Service Role Key

**Date**: October 17, 2025  
**Severity**: HIGH  
**Issue**: Supabase service_role key exposed in docker-compose.yml committed to GitHub

## Immediate Actions Required

### ⚠️ CRITICAL: Complete These Steps Immediately

---

## Step 1: Revoke/Rotate Service Role Key ⚡ URGENT

### Action Required NOW:

1. **Login to Supabase Dashboard**: https://app.supabase.com
2. **Navigate to**: Project Settings → API
3. **Regenerate Keys**:
   - Click "Generate new anon key" → Copy the new key
   - Click "Generate new service role key" → Copy the new key
   - Note: The old keys become invalid immediately upon regeneration

### What This Does:

- **Immediately invalidates** the leaked service_role key
- Prevents any unauthorized access using the compromised key
- Generates fresh keys for your application

### Save These New Keys Securely:

```bash
# DO NOT commit these values - paste them into your local .env file
SUPABASE_ANON_KEY=<paste-new-anon-key-here>
SUPABASE_SERVICE_ROLE_KEY=<paste-new-service-role-key-here>
```

---

## Step 2: Update Environment Variables

### Local Development:

```bash
# Update your local .env file (already gitignored)
cd /Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR\ -

# Edit .env and replace:
# SUPABASE_ANON_KEY=<OLD_PLACEHOLDER>
# SUPABASE_SERVICE_ROLE_KEY=<OLD_PLACEHOLDER>
# With the new keys from Supabase Dashboard

# Verify .env is not tracked:
git status .env  # Should show "nothing to commit" or not listed
```

### Production/Staging Deployments:

**If using environment variables in hosting platforms:**

#### Vercel/Netlify:

```bash
# Update via CLI or dashboard
vercel env add SUPABASE_ANON_KEY production
vercel env add SUPABASE_SERVICE_ROLE_KEY production
```

#### AWS/GCP/Azure:

- Update environment variables in your container/function configuration
- Redeploy services to pick up new values

#### Docker Compose (Production):

```bash
# Update .env file on production server
# Then restart services:
docker-compose down
docker-compose up -d
```

### CI/CD Secrets:

#### GitHub Actions:

1. Go to: Repository → Settings → Secrets and variables → Actions
2. Update:
   - `SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY`

---

## Step 3: Redeploy All Applications

### Backend:

```bash
cd backend
npm install  # Ensure dependencies are current
# If using Docker:
docker-compose restart backend
# Or manually:
npm run build
npm start
```

### Admin Dashboard:

```bash
cd admin-dashboard
npm install
# Update production build:
npm run build
# Redeploy to hosting (Vercel/Netlify/etc.)
```

### Mobile App:

```bash
cd mobile-app
# Rebuild APK/AAB with new keys
./gradlew assembleRelease
# Update any hardcoded API keys if present (check build.gradle, local.properties)
```

---

## Step 4: Remove Keys from Git History 🔥

### Prerequisites:

```bash
# Install git-filter-repo (recommended over BFG)
brew install git-filter-repo  # macOS
# Or: pip install git-filter-repo
```

### Identify Leaked Keys:

First, let's find all commits containing the leaked key:

```bash
# Search for the old service role key pattern
git log -S "eyJ" --all --oneline
# Or search docker-compose.yml history:
git log -p docker-compose.yml | grep -i "service_role"
```

### Prepare Replacement File:

Edit `scripts/git-scrub-secrets.txt` and replace placeholders with actual leaked values:

```bash
# Create the replacement file with ACTUAL leaked strings
cat > scripts/git-scrub-secrets.txt << 'EOF'
# Format: literal-string==>replacement
OLD_LEAKED_SERVICE_ROLE_KEY==>[REDACTED_SUPABASE_SERVICE_ROLE_KEY]
OLD_LEAKED_ANON_KEY==>[REDACTED_SUPABASE_ANON_KEY]
OLD_LEAKED_JWT_SECRET==>[REDACTED_JWT_SECRET]
OLD_POSTGRES_PASSWORD==>[REDACTED_POSTGRES_PASSWORD]
EOF
```

**IMPORTANT**: Replace the `OLD_LEAKED_*` placeholders with the actual leaked strings from your history.

### Execute History Rewrite:

⚠️ **WARNING**: This rewrites Git history. Coordinate with your team!

```bash
# Make a backup first!
git clone /Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR\ - ~/TalkAR-backup

cd /Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR\ -

# Execute the filter
git filter-repo --replace-text scripts/git-scrub-secrets.txt --force

# Verify the secrets are gone:
git log -p | grep -i "service_role" | grep -v "REDACTED"

# Force push to remote (⚠️ DESTRUCTIVE):
git push origin --force --all
git push origin --force --tags
```

### Notify Collaborators:

After force-pushing, all collaborators must re-clone or reset their local repos:

```bash
# For collaborators:
cd ~/TalkAR
git fetch origin
git reset --hard origin/main
# Or safer: re-clone
git clone git@github.com:ajitreddy013/TalkAR.git TalkAR-new
```

---

## Step 5: Rotate Database Credentials

### Check if DB Credentials Were Exposed:

```bash
# Search for postgres passwords in history:
git log -p | grep -i "postgres_password"
```

### If Exposed, Rotate DB Password:

#### Supabase Managed Postgres:

1. Go to: Supabase Dashboard → Project Settings → Database
2. Click "Reset database password"
3. Update `POSTGRES_PASSWORD` in all environments
4. Restart database connections

#### Self-Hosted Postgres:

```bash
# Connect to PostgreSQL as superuser
psql -U postgres

# Rotate password:
ALTER USER talkar_user WITH PASSWORD 'new-secure-password-here';

# Update .env and restart services
```

---

## Step 6: Audit Logs for Suspicious Activity

### Supabase Logs:

1. **Navigate to**: Supabase Dashboard → Logs
2. **Check Each Service**:
   - **API Logs**: Look for unusual request patterns
   - **Auth Logs**: Check for unauthorized login attempts
   - **Database Logs**: Review for suspicious queries
3. **Filter for suspicious activity**:

   ```
   # Look for:
   - Requests using the old service_role key after rotation
   - Unusual IP addresses
   - High-volume requests
   - Data exfiltration patterns (large SELECT queries)
   - Unauthorized CREATE/DROP/ALTER operations
   ```

4. **Export logs** for forensic analysis:
   - Download logs from the exposure window
   - Check timestamps against GitHub commit history

### PostgreSQL Audit:

If you have PostgreSQL logging enabled:

```sql
-- Check for suspicious queries
SELECT
  log_time,
  user_name,
  database_name,
  query
FROM pg_log
WHERE log_time > '2025-10-10'  -- Adjust to exposure start date
  AND (query ILIKE '%DROP%'
       OR query ILIKE '%DELETE%'
       OR query ILIKE '%ALTER%')
ORDER BY log_time DESC;
```

### Check for Data Breaches:

```sql
-- Check for large data exports
SELECT
  usename,
  client_addr,
  query_start,
  state,
  query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start DESC;
```

---

## Step 7: Enable GitHub Secret Scanning

### Enable in Repository Settings:

1. **Navigate to**: https://github.com/ajitreddy013/TalkAR/settings/security_analysis
2. **Enable**:
   - ✅ **Dependency graph**
   - ✅ **Dependabot alerts**
   - ✅ **Dependabot security updates**
   - ✅ **Secret scanning** (if available for your plan)
   - ✅ **Push protection** (prevents committing secrets)

### Configure .gitignore:

Already done - verify it includes:

```bash
cat .gitignore | grep -E "(\.env|secrets|\.pem|\.key)"
```

### Add Pre-commit Hooks:

```bash
# Install pre-commit framework
pip install pre-commit detect-secrets

# Install hooks
pre-commit install

# Establish baseline:
detect-secrets scan > .secrets.baseline
```

---

## Step 8: Additional Security Measures

### 1. Enable MFA on Supabase Account

- Supabase Dashboard → Account Settings → Security
- Enable Two-Factor Authentication

### 2. Review Supabase Project Settings:

- **API Settings**: Ensure JWT expiry is reasonable
- **Auth Settings**: Review allowed redirect URLs
- **Database**: Enable Row Level Security (RLS) on all tables

### 3. Implement API Rate Limiting:

```typescript
// In backend/src/middleware/rateLimiter.ts
import rateLimit from "express-rate-limit";

export const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per windowMs
  message: "Too many requests, please try again later.",
});
```

### 4. Monitor for Future Leaks:

```bash
# Add to CI/CD pipeline (.github/workflows/security.yml)
# See the security.yml file in .github/workflows/
```

### 5. Regular Secret Rotation:

Use the provided script to rotate secrets regularly:

```bash
# Run the secret rotation script
./scripts/rotate-secrets.sh
```

### 6. Security Audits:

Run regular security audits using the provided script:

```bash
# Run security audit
node security-audit.js
```

---

## Verification Checklist

After completing all steps, verify:

- [ ] Old service_role key no longer works (test with curl)
- [ ] New keys work in all environments (dev/staging/prod)
- [ ] All applications successfully redeployed
- [ ] Git history scrubbed (no secrets in `git log -p`)
- [ ] Supabase logs show no suspicious activity
- [ ] GitHub secret scanning enabled
- [ ] Pre-commit hooks installed and working
- [ ] Documentation updated with new security practices
- [ ] Team notified of incident and response

---

## Testing New Keys:

```bash
# Test new anon key
curl 'https://<your-project-ref>.supabase.co/rest/v1/' \
  -H "apikey: <NEW_ANON_KEY>" \
  -H "Authorization: Bearer <NEW_ANON_KEY>"

# Test new service_role key (be careful!)
curl 'https://<your-project-ref>.supabase.co/rest/v1/' \
  -H "apikey: <NEW_SERVICE_ROLE_KEY>" \
  -H "Authorization: Bearer <NEW_SERVICE_ROLE_KEY>"
```

---

## Post-Incident Documentation

### Create Incident Report:

- Document exposure timeline
- List affected systems/data
- Record actions taken
- Identify root cause
- Plan preventive measures

### Update Security Policies:

- Mandatory code review for config changes
- Regular secret rotation schedule
- Security training for team members
- Incident response procedures

---

## Support Resources

- **Supabase Security**: https://supabase.com/docs/guides/platform/security
- **GitHub Secret Scanning**: https://docs.github.com/en/code-security/secret-scanning
- **git-filter-repo Docs**: https://github.com/newren/git-filter-repo
- **OWASP API Security**: https://owasp.org/www-project-api-security/

---

## Emergency Contacts

- **Supabase Support**: support@supabase.io
- **Security Issues**: security@supabase.io
- **GitHub Support**: https://support.github.com/

---

**Remember**: Speed is critical. Complete Steps 1-3 immediately, then proceed with history cleanup and auditing.
