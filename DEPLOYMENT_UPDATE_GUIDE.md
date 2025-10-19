# Deployment Environment Update Guide

**Date**: October 17, 2025  
**Task**: Update Supabase keys in all deployment environments

---

## ✅ Local Development - COMPLETED

- ✅ Root `.env` updated with new keys
- ✅ `backend/.env` updated with new keys
- ✅ `admin-dashboard/.env` updated with ANON key only (frontend-safe)
- ✅ Keys tested and verified working (HTTP 200)

---

## 📋 Deployment Environments to Update

### 1. GitHub Actions Secrets

Update repository secrets at: https://github.com/ajitreddy013/TalkAR/settings/secrets/actions

**Add/Update these secrets:**

```
SUPABASE_URL=your-supabase-url-here
SUPABASE_ANON_KEY=your-anon-key-here
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here
SUPABASE_JWT_SECRET=your-jwt-secret-here
```

**Steps:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret" for each
3. Or update existing secrets with new values

---

### 2. Production Hosting Platform

**Choose your platform and follow the instructions:**

#### Option A: Vercel

```bash
# Install Vercel CLI if not already installed
npm i -g vercel

# Login
vercel login

# Add environment variables to production

# Example (do not paste secrets in code):
vercel env add SUPABASE_URL production
vercel env add SUPABASE_ANON_KEY production
vercel env add SUPABASE_SERVICE_ROLE_KEY production
vercel env add SUPABASE_JWT_SECRET production

# Trigger redeployment
vercel --prod
```

**Or via Vercel Dashboard:**

1. Go to: https://vercel.com/dashboard
2. Select your project
3. Settings → Environment Variables
4. Add/Update the variables above

---

#### Option B: Netlify

```bash
# Install Netlify CLI if not already installed
npm i -g netlify-cli

# Login
netlify login

# Link to your site
netlify link

# Add environment variables

# Example (do not paste secrets in code):
netlify env:set SUPABASE_URL "your-supabase-url-here"
netlify env:set SUPABASE_ANON_KEY "your-anon-key-here"
netlify env:set SUPABASE_SERVICE_ROLE_KEY "your-service-role-key-here"

# Trigger rebuild
netlify build
netlify deploy --prod
```

**Or via Netlify Dashboard:**

1. Go to: https://app.netlify.com/
2. Select your site
3. Site settings → Environment variables
4. Add/Update the variables

---

#### Option C: AWS (EC2, ECS, Lambda)

**For EC2/ECS:**

```bash
# SSH into your server
ssh user@your-server-ip

# Edit .env file on server
cd /path/to/TalkAR
nano .env

# Update with new keys (paste from local .env)
# Save and exit (Ctrl+X, Y, Enter)

# Restart services
docker-compose down
docker-compose up -d

# Or if using PM2
pm2 restart backend
```

**For Lambda/Fargate:**

1. Update environment variables in AWS Console
2. Go to Lambda/ECS service → Configuration → Environment variables
3. Update SUPABASE\_\* variables
4. Redeploy function/task

---

#### Option D: Google Cloud Platform

**For Cloud Run:**

```bash
# Update service with new env vars
gcloud run services update backend \
  --set-env-vars="SUPABASE_URL=your-supabase-url-here,SUPABASE_ANON_KEY=your-anon-key-here,SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here"
```

---

#### Option E: Digital Ocean App Platform

**Via Dashboard:**

1. Go to: https://cloud.digitalocean.com/apps
2. Select your app
3. Settings → App-Level Environment Variables
4. Edit and update SUPABASE\_\* variables
5. Save and trigger redeployment

---

#### Option F: Heroku

```bash
# Install Heroku CLI if not already installed
npm i -g heroku

# Login
heroku login

# Set config vars

# Example (do not paste secrets in code):
heroku config:set SUPABASE_URL="your-supabase-url-here" -a your-app-name
heroku config:set SUPABASE_ANON_KEY="your-anon-key-here" -a your-app-name
heroku config:set SUPABASE_SERVICE_ROLE_KEY="your-service-role-key-here" -a your-app-name

# Restart app
heroku restart -a your-app-name
```

---

### 3. Docker Compose (Production Server)

If using Docker Compose on a VPS/server:

```bash
# SSH to server
ssh user@your-server

# Navigate to project
cd /path/to/TalkAR

# Update .env file
nano .env
# (Paste new keys from local .env)

# Restart containers
docker-compose down
docker-compose up -d

# Verify services are running
docker-compose ps
docker-compose logs backend
docker-compose logs admin-dashboard
```

---

### 4. Mobile App Configuration

**For Android (mobile-app/):**

The mobile app should communicate with your backend API, which uses the service_role key. The mobile app itself should **NEVER** contain Supabase keys directly.

**If you need to update mobile config:**

```bash
cd mobile-app

# Update local.properties or gradle.properties if Supabase is configured
# But remember: mobile apps should only use backend API endpoints

# Rebuild APK/AAB
./gradlew assembleRelease

# Test with new backend
./gradlew connectedAndroidTest
```

---

## 🔒 Security Best Practices

### ✅ DO:

- Use `SUPABASE_SERVICE_ROLE_KEY` only in backend/server code
- Use `SUPABASE_ANON_KEY` in frontend (with RLS policies enabled)
- Store keys in environment variables, never in code
- Use different keys for dev/staging/production if possible
- Restrict ANON key permissions in Supabase Dashboard → Authentication → Policies

### ❌ DON'T:

- Never commit keys to git
- Never expose `SERVICE_ROLE_KEY` to client-side code (frontend, mobile)
- Never log keys in application logs
- Never share keys in chat/email

---

## ✅ Verification Checklist

After updating each environment:

- [ ] GitHub Actions secrets updated
- [ ] Production hosting platform updated (Vercel/Netlify/AWS/etc.)
- [ ] Staging environment updated (if applicable)
- [ ] Docker Compose production server updated (if applicable)
- [ ] Mobile app config reviewed (should use backend API only)
- [ ] Test deployments successful
- [ ] Applications can connect to Supabase
- [ ] Old keys no longer work (test with curl)

---

## 🧪 Testing Deployment

After updating each environment, test:

```bash
# Test backend API
curl https://your-backend-domain.com/api/v1/health

# Test admin dashboard
# Open in browser and verify Supabase connection

# Test old key is revoked (should fail)
<<<<<<< HEAD
curl 'https://your-supabase-project-ref.supabase.co/rest/v1/' \
=======
curl 'https://adktqahcctnqzdzlzrvx.supabase.co/rest/v1/' \
>>>>>>> afbabf0751309f911b71357bc5d82ab569e9f086
  -H "apikey: OLD_KEY_HERE"
# Expected: 401 Unauthorized

# Test new key works
<<<<<<< HEAD
curl 'https://your-supabase-project-ref.supabase.co/rest/v1/' \
  -H "apikey: YOUR_NEW_ANON_KEY"
=======
curl 'https://adktqahcctnqzdzlzrvx.supabase.co/rest/v1/' \
  -H "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFka3RxYWhjY3RucXpkemx6cnZ4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA2NTgyMjUsImV4cCI6MjA3NjIzNDIyNX0.5qa2lZGv8ThK8hpQsNSvRBuNYBlxaiRNloCVN87Aa1o"
>>>>>>> afbabf0751309f911b71357bc5d82ab569e9f086
# Expected: 200 OK
```

---

## 📞 Support

If you encounter issues:

- Check Supabase Dashboard → Logs for connection errors
- Verify environment variables are set correctly
- Ensure services restarted after updating env vars
- Review application logs for Supabase-related errors

---

**Next Steps After Deployment Update:**

1. Proceed to: "Redeploy all applications" in todo list
2. Then: "Scrub leaked keys from Git history"
3. Finally: "Enable GitHub secret scanning"
