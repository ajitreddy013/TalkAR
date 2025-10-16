# URGENT: Security Incident - Quick Action Guide

**Status**: 🔴 ACTIVE INCIDENT - Service Role Key Leaked  
**Date**: October 17, 2025  
**Priority**: CRITICAL

---

## ⚡ IMMEDIATE ACTIONS (Do Now - 15 minutes)

### 1. Rotate Supabase Keys (5 min) 🔥

```bash
# 1. Open Supabase Dashboard
open https://app.supabase.com

# 2. Go to: Project Settings → API → Generate new keys
# 3. Copy NEW keys to your local .env:
nano .env

# Paste the new keys:
SUPABASE_ANON_KEY=<new-anon-key>
SUPABASE_SERVICE_ROLE_KEY=<new-service-role-key>
```

### 2. Update Production Environment (5 min)

```bash
# Update your deployment platform (choose one):

# Vercel:
vercel env add SUPABASE_ANON_KEY production
vercel env add SUPABASE_SERVICE_ROLE_KEY production

# Docker on server:
ssh your-server
nano /path/to/TalkAR/.env
# Update keys, then:
docker-compose restart

# AWS/GCP: Update via console
```

### 3. Redeploy Applications (5 min)

```bash
# Backend
cd backend && npm run build && npm start

# Admin Dashboard
cd admin-dashboard && npm run build
# Then deploy to hosting

# Mobile App - rebuild and publish update
cd mobile-app && ./gradlew assembleRelease
```

---

## 🔍 AUDIT & INVESTIGATION (Next 30 minutes)

### 4. Check for Unauthorized Access

```bash
# Run audit helper script
bash scripts/audit-supabase-logs.sh

# Then manually check Supabase Dashboard → Logs
# Look for suspicious activity BEFORE key rotation
```

### 5. Identify Exposure Window

```bash
# When was secret first committed?
git log --all --oneline -- docker-compose.yml | grep -i "supabase"

# Check all commits with the leaked key
git log -S "eyJ" --all --pretty=format:"%h %ad %s" --date=short
```

---

## 🧹 CLEANUP (Within 24 hours)

### 6. Scrub Git History

```bash
# Step 1: Edit replacement file with ACTUAL leaked values
nano scripts/git-scrub-secrets.txt

# Replace placeholders like this:
# eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ACTUAL_OLD_KEY_HERE==>[REDACTED]

# Step 2: Run the scrubbing script
bash scripts/scrub-git-history.sh

# Step 3: Force push (⚠️ destructive, coordinate with team!)
git push origin --force --all
```

### 7. Rotate Database Credentials (if exposed)

```bash
# If Postgres password was also exposed:
# 1. Supabase Dashboard → Database Settings → Reset Password
# 2. Update all .env files with new password
# 3. Restart all services
```

---

## 🛡️ ENABLE PROTECTIONS (Within 48 hours)

### 8. Enable GitHub Secret Scanning

```bash
# Run setup helper
bash scripts/setup-github-security.sh

# Or manually:
# 1. Go to: https://github.com/ajitreddy013/TalkAR/settings/security_analysis
# 2. Enable:
#    - Dependabot alerts
#    - Secret scanning
#    - Push protection
```

### 9. Install Pre-commit Hooks (prevent future leaks)

```bash
# Install pre-commit
pip install pre-commit
# or: brew install pre-commit

# Install hooks
pre-commit install

# Create baseline
detect-secrets scan > .secrets.baseline

# Test it works
echo "AKIA1234567890EXAMPLE" > test.txt
git add test.txt
git commit -m "test"  # Should BLOCK this commit!
```

---

## 📋 DETAILED GUIDES

Full documentation available in:

- **Complete incident response**: `SECURITY_INCIDENT_RESPONSE.md`
- **Rotation procedures**: `SECURITY_ROTATION_GUIDE.md`

---

## 🎯 SUCCESS CRITERIA

You're safe when ALL of these are true:

- [ ] ✅ Old service_role key fails when tested
- [ ] ✅ New keys work in all environments (dev/staging/prod)
- [ ] ✅ All applications redeployed and functional
- [ ] ✅ Logs show no suspicious activity during exposure
- [ ] ✅ Git history scrubbed (no secrets in `git log -p`)
- [ ] ✅ GitHub secret scanning enabled
- [ ] ✅ Pre-commit hooks installed and blocking secrets
- [ ] ✅ Team notified of changes

---

## 🆘 NEED HELP?

### Test if old key still works (it shouldn't):

```bash
# This should return 401 Unauthorized
curl 'https://<project-ref>.supabase.co/rest/v1/' \
  -H "apikey: <OLD_KEY>" \
  -H "Authorization: Bearer <OLD_KEY>"
```

### Verify new keys work:

```bash
# This should return 200 OK
curl 'https://<project-ref>.supabase.co/rest/v1/' \
  -H "apikey: <NEW_ANON_KEY>" \
  -H "Authorization: Bearer <NEW_ANON_KEY>"
```

### If you're stuck:

- Supabase support: support@supabase.io
- Security incidents: security@supabase.io

---

## 📊 TIMELINE TRACKER

| Step                 | Time   | Status | Notes                  |
| -------------------- | ------ | ------ | ---------------------- |
| 1. Rotate keys       | 5 min  | [ ]    | Supabase Dashboard     |
| 2. Update prod env   | 5 min  | [ ]    | All environments       |
| 3. Redeploy apps     | 5 min  | [ ]    | Backend, Admin, Mobile |
| 4. Audit logs        | 30 min | [ ]    | Check for breaches     |
| 5. Identify exposure | 10 min | [ ]    | Git history            |
| 6. Scrub history     | 1 hr   | [ ]    | git-filter-repo        |
| 7. Rotate DB creds   | 15 min | [ ]    | If needed              |
| 8. Enable scanning   | 30 min | [ ]    | GitHub settings        |
| 9. Pre-commit hooks  | 15 min | [ ]    | Local protection       |

**Total estimated time**: 2-3 hours

---

## 🚨 RED FLAGS - Call for help if you see:

- Mass data extraction queries in logs
- Suspicious IP addresses from unexpected countries
- New admin users created
- Database structure modifications (ALTER TABLE, DROP)
- Requests AFTER key rotation using old keys (ongoing attack!)

---

**Remember**: Speed matters! The faster you rotate, the smaller the exposure window.

Good luck! 🛡️
