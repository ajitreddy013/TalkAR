# Security Rotation & History Scrub Guide

This guide helps you rotate leaked keys and optionally scrub Git history.

## 1) Rotate Supabase Keys

In Supabase Dashboard (Project Settings → API):

**WARNING:** Supabase secret keys (anon, service_role) must ONLY be used in backend/server code. Never expose these keys in frontend, mobile, or client-side code. The service_role key is highly privileged—never expose to clients. If you use Supabase in the frontend, use only the anon key and restrict its permissions in Supabase Dashboard.

Update your local `.env` and deployment secrets with new values:

## 2) Rotate AWS Credentials (if applicable)

- Create new IAM access keys
- Disable old keys
- Update `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`

## 3) Update Local .env (example values)

Create or update `.env` in repo root (kept gitignored):

JWT_SECRET=<paste-generated-secret>
SUPABASE_URL=https://<your-project-ref>.supabase.co
SUPABASE_ANON_KEY=<new-anon-key>
SUPABASE_SERVICE_ROLE_KEY=<new-service-role-key>
SUPABASE_JWT_SECRET=<new-jwt-secret>
REACT_APP_SUPABASE_URL=https://<your-project-ref>.supabase.co
REACT_APP_SUPABASE_ANON_KEY=<new-anon-key>

## 4) Optional: Scrub Git History

Warning: This rewrites commit history and requires force-push. Coordinate with collaborators.

Using git filter-repo (recommended):

- Install: `brew install git-filter-repo` (macOS) or see docs.
- Create a file `scripts/git-scrub-supabase.txt` listing strings to purge (one per line). Examples:
  - `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9`
  - Old anon key
  - Old service role key
  - Old JWT secret
- Run:
  - `git filter-repo --invert-paths --paths-from-file scripts/git-scrub-supabase.txt`
  - Or to replace strings: `git filter-repo --replace-text scripts/git-scrub-supabase.txt`
- Force push: `git push --force origin main`

## 5) Invalidate Cached Artifacts

- Re-deploy environments with new secrets
- Rotate any CI/CD stored secrets
- Verify applications pick up new keys (restart services)

## 6) Verify

- Run `npm run security:audit` (if configured) or `node security-audit.js`
- Run `node performance-security-tests.js` (optional) and check `performance-security-report.json`

## Notes

- We already removed hardcoded secrets from docker-compose.yml and added `.env.example`.
- `.env` remains gitignored; do not commit real secrets.
