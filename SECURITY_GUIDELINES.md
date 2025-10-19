# Security Guidelines

## Preventing Secret Leaks

### 1. Environment Variables

- Always store secrets in environment variables, never in code
- Use [.env.example](file:///Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR%20-/.env.example) files to document required variables without exposing actual values
- Ensure [.gitignore](file:///Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR%20-/.gitignore) includes [.env](file:///Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR%20-/.env) and other secret files

### 2. Pre-commit Hooks

- Pre-commit hooks are installed to automatically check for secrets
- Run `pre-commit run --all-files` to manually check all files
- The hooks will prevent committing files that contain secrets

### 3. GitHub Security Features

- Enable GitHub secret scanning in repository settings
- Use GitHub's push protection to prevent accidental secret commits
- Regularly review security alerts

### 4. Secret Management Best Practices

- Rotate secrets regularly
- Use different secrets for development, staging, and production
- Never hardcode secrets in documentation or comments
- Use placeholders like `YOUR_SECRET_HERE` in documentation

### 5. What to Do If Secrets Are Exposed

1. Immediately rotate the exposed secrets
2. Use `git-filter-repo` or BFG to remove secrets from git history
3. Force push the cleaned repository
4. Notify team members to re-clone the repository
5. Update all environments with new secrets

### 6. Regular Security Audits

- Run security audits regularly using the provided scripts
- Check for hardcoded secrets in code reviews
- Monitor logs for suspicious activity
