#!/usr/bin/env bash
set -euo pipefail

# GitHub Secret Scanning Setup Script
# Enables secret scanning and push protection for TalkAR repository

echo "============================================"
echo "GitHub Secret Scanning Setup"
echo "============================================"
echo ""

REPO_OWNER="ajitreddy013"
REPO_NAME="TalkAR"

echo "Repository: $REPO_OWNER/$REPO_NAME"
echo ""

echo "This script will guide you through enabling GitHub security features."
echo ""

echo "=== AUTOMATIC SETUP (via GitHub CLI) ==="
echo ""

if command -v gh &> /dev/null; then
    echo "✓ GitHub CLI detected"
    echo ""
    
    read -p "Enable secret scanning via GitHub CLI? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        echo "Enabling security features..."
        
        # Enable Dependabot alerts
        gh api -X PUT "/repos/$REPO_OWNER/$REPO_NAME/vulnerability-alerts" || echo "Dependabot may already be enabled"
        
        # Note: Secret scanning requires GitHub Advanced Security for private repos
        # For public repos, it's enabled by default
        
        echo ""
        echo "✓ Enabled Dependabot vulnerability alerts"
        echo ""
        echo "Note: Secret scanning is automatically enabled for public repositories."
        echo "For private repositories, you need GitHub Advanced Security (Enterprise feature)."
        echo ""
    fi
else
    echo "GitHub CLI not found. Install it with:"
    echo "  macOS:   brew install gh"
    echo "  Linux:   See https://cli.github.com/manual/installation"
    echo ""
fi

echo "=== MANUAL SETUP INSTRUCTIONS ==="
echo ""
echo "1. Navigate to your repository settings:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/settings/security_analysis"
echo ""

echo "2. Enable the following features:"
echo ""

echo "   ✅ Dependency graph"
echo "      - Automatically enabled for public repos"
echo "      - Shows your project dependencies"
echo ""

echo "   ✅ Dependabot alerts"
echo "      - Notifies you of vulnerable dependencies"
echo "      - Click 'Enable' if not already active"
echo ""

echo "   ✅ Dependabot security updates"
echo "      - Automatically creates PRs to fix vulnerabilities"
echo "      - Click 'Enable' after enabling Dependabot alerts"
echo ""

echo "   ✅ Secret scanning (Public repos: automatic)"
echo "      - Detects leaked secrets in your code"
echo "      - For private repos: Requires GitHub Advanced Security"
echo "      - Direct link: https://github.com/$REPO_OWNER/$REPO_NAME/settings/security_analysis"
echo ""

echo "   ✅ Push protection"
echo "      - Prevents pushing commits with secrets"
echo "      - Available under Secret scanning settings"
echo "      - Highly recommended!"
echo ""

echo "   ✅ Code scanning (via CodeQL)"
echo "      - Already configured in .github/workflows/security.yml"
echo "      - Will run automatically on push and pull requests"
echo ""

echo "3. Configure Secret Scanning custom patterns (optional):"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/settings/security_analysis/custom_patterns"
echo ""
echo "   Add patterns for:"
echo "   - Supabase keys: eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]*\.[A-Za-z0-9_-]*"
echo "   - AWS keys: AKIA[0-9A-Z]{16}"
echo "   - Generic API keys: [a-zA-Z0-9]{32,}"
echo ""

echo "4. Set up branch protection rules:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/settings/branches"
echo ""
echo "   For 'main' branch, enable:"
echo "   - Require pull request reviews"
echo "   - Require status checks (including security scans)"
echo "   - Require branches to be up to date"
echo "   - Include administrators in restrictions"
echo ""

echo "5. Configure security advisories:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/security/advisories"
echo ""
echo "   - Set up private vulnerability reporting"
echo "   - Add security policy (SECURITY.md)"
echo ""

echo "=== VERIFICATION ==="
echo ""
echo "After enabling, verify by:"
echo ""
echo "1. Check security overview:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/security"
echo ""
echo "2. Test push protection (should block):"
echo "   echo 'AKIA1234567890ABCDEF' > test-secret.txt"
echo "   git add test-secret.txt"
echo "   git commit -m 'test'"
echo "   git push  # Should be blocked!"
echo ""
echo "3. View security alerts:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/security/dependabot"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/security/secret-scanning"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/security/code-scanning"
echo ""

echo "=== ADDITIONAL SECURITY MEASURES ==="
echo ""
echo "1. Enable 2FA on your GitHub account:"
echo "   https://github.com/settings/security"
echo ""
echo "2. Use GitHub Actions secrets for CI/CD:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/settings/secrets/actions"
echo ""
echo "   Add these secrets:"
echo "   - SUPABASE_URL"
echo "   - SUPABASE_ANON_KEY"
echo "   - SUPABASE_SERVICE_ROLE_KEY"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo ""
echo "3. Review access permissions:"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/settings/access"
echo ""
echo "   - Remove users who no longer need access"
echo "   - Use teams instead of individual access"
echo "   - Use least-privilege principle"
echo ""

echo "=== PRE-COMMIT HOOKS SETUP ==="
echo ""
echo "Local secret scanning with pre-commit hooks:"
echo ""
echo "1. Install pre-commit:"
echo "   pip install pre-commit"
echo "   # or: brew install pre-commit"
echo ""
echo "2. Install hooks (already configured in .pre-commit-config.yaml):"
echo "   pre-commit install"
echo ""
echo "3. Create secrets baseline:"
echo "   detect-secrets scan > .secrets.baseline"
echo ""
echo "4. Test pre-commit hooks:"
echo "   pre-commit run --all-files"
echo ""
echo "Now all commits will be scanned for secrets before pushing!"
echo ""

echo "============================================"
echo "Setup Instructions Summary Saved"
echo "============================================"
echo ""

cat > github-security-setup-checklist.md << 'CHECKLIST'
# GitHub Security Setup Checklist

## Repository Security Features

### 1. Dependency Graph
- [ ] Navigate to: https://github.com/ajitreddy013/TalkAR/settings/security_analysis
- [ ] Verify "Dependency graph" is enabled (should be automatic for public repos)

### 2. Dependabot Alerts
- [ ] Enable "Dependabot alerts"
- [ ] Configure alert frequency (daily/weekly)

### 3. Dependabot Security Updates
- [ ] Enable "Dependabot security updates"
- [ ] Review and merge automated security PRs

### 4. Secret Scanning
- [ ] Enable "Secret scanning"
  - Public repos: Automatic
  - Private repos: Requires GitHub Advanced Security
- [ ] Configure custom patterns for:
  - [ ] Supabase keys
  - [ ] AWS credentials
  - [ ] Custom API keys

### 5. Push Protection
- [ ] Enable "Push protection" under Secret scanning
- [ ] Test by attempting to commit a fake secret

### 6. Code Scanning (CodeQL)
- [ ] Verify .github/workflows/security.yml is present
- [ ] Check that workflow runs successfully
- [ ] Review any alerts in Security tab

## Branch Protection

### 7. Main Branch Protection
- [ ] Navigate to: https://github.com/ajitreddy013/TalkAR/settings/branches
- [ ] Add rule for 'main' branch:
  - [ ] Require pull request reviews (at least 1)
  - [ ] Require status checks to pass
  - [ ] Require branches to be up to date
  - [ ] Include administrators

## Local Security Tools

### 8. Pre-commit Hooks
- [ ] Install pre-commit: `pip install pre-commit`
- [ ] Install hooks: `pre-commit install`
- [ ] Create baseline: `detect-secrets scan > .secrets.baseline`
- [ ] Test: `pre-commit run --all-files`

### 9. Git Hooks for Secret Detection
- [ ] Hooks installed and working
- [ ] Team members have hooks installed

## Account Security

### 10. Personal Security
- [ ] Enable 2FA on GitHub account
- [ ] Use SSH keys instead of passwords
- [ ] Review authorized applications

### 11. Repository Access
- [ ] Review collaborators and remove unnecessary access
- [ ] Use teams for organization
- [ ] Audit third-party app access

## Secrets Management

### 12. GitHub Actions Secrets
- [ ] Add all required secrets to Actions
- [ ] Remove any hardcoded secrets from workflows
- [ ] Use environment-specific secrets

### 13. Environment Variables
- [ ] Verify .env is in .gitignore
- [ ] All environments use environment variables
- [ ] No secrets in docker-compose.yml

## Monitoring

### 14. Security Monitoring
- [ ] Subscribe to security alerts
- [ ] Check Security tab regularly
- [ ] Review weekly security digest

### 15. Audit Logging
- [ ] Enable audit log (Enterprise feature)
- [ ] Monitor for suspicious activity
- [ ] Set up alerts for security events

## Documentation

### 16. Security Documentation
- [ ] Create SECURITY.md with responsible disclosure policy
- [ ] Document incident response procedures
- [ ] Maintain security contact information

## Verification

### 17. Test Security Measures
- [ ] Test push protection with fake secret
- [ ] Verify pre-commit hooks block secrets
- [ ] Confirm dependabot creates PRs for vulnerabilities
- [ ] Check CodeQL scanning results

---

**Completion Date**: _______________
**Verified By**: _______________
**Next Review**: _______________
CHECKLIST

echo "Checklist saved to: github-security-setup-checklist.md"
echo ""
echo "✓ Setup guide complete!"
echo ""
