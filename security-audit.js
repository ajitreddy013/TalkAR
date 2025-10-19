#!/usr/bin/env node

/**
 * Security Audit Script for TalkAR Project
 * This script performs security audits on configuration files, dependencies, and code patterns
 */

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

// Colors for output
const colors = {
  reset: "\x1b[0m",
  bright: "\x1b[1m",
  green: "\x1b[32m",
  red: "\x1b[31m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  cyan: "\x1b[36m",
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

// Security audit results
const auditResults = {
  passed: 0,
  failed: 0,
  warnings: 0,
  issues: [],
};

function addIssue(severity, category, message, file = null, line = null) {
  auditResults.issues.push({
    severity,
    category,
    message,
    file,
    line,
    timestamp: new Date().toISOString(),
  });

  if (severity === "high") {
    auditResults.failed++;
    log(`❌ [HIGH] ${category}: ${message}`, colors.red);
  } else if (severity === "medium") {
    auditResults.warnings++;
    log(`⚠️  [MEDIUM] ${category}: ${message}`, colors.yellow);
  } else {
    auditResults.passed++;
    log(`✅ [LOW] ${category}: ${message}`, colors.green);
  }
}

// Check for hardcoded secrets
function checkHardcodedSecrets(content, filePath) {
  const secretPatterns = [
    /password\s*=\s*['"]\w+['"]/gi,
    /api[_-]?key\s*=\s*['"]\w+['"]/gi,
    /secret\s*=\s*['"]\w+['"]/gi,
    /token\s*=\s*['"]\w+['"]/gi,
    /aws[_-]?access[_-]?key\s*=\s*['"]\w+['"]/gi,
    /aws[_-]?secret\s*=\s*['"]\w+['"]/gi,
    /jwt[_-]?secret\s*=\s*['"]\w+['"]/gi,
    /[a-zA-Z0-9]*key\s*=\s*['"][a-zA-Z0-9]{10,}['"]/gi, // Generic key pattern
    /[a-zA-Z0-9]*token\s*=\s*['"][a-zA-Z0-9]{10,}['"]/gi, // Generic token pattern
  ];

  let foundSecrets = false;
  secretPatterns.forEach((pattern, index) => {
    const matches = content.match(pattern);
    if (matches) {
      matches.forEach((match) => {
        addIssue(
          "high",
          "Hardcoded Secret",
          `Found potential hardcoded secret: ${match}`,
          filePath
        );
        foundSecrets = true;
      });
    }
  });

  return foundSecrets;
}

// Check for weak JWT secrets
function checkJWTSecret(secret) {
  if (!secret || secret.length < 32) {
    addIssue(
      "high",
      "JWT Security",
      "JWT secret is too short (should be at least 32 characters)"
    );
    return false;
  }

  if (
    secret === "your-super-secret-jwt-key" ||
    secret.includes("example") ||
    secret.includes("demo")
  ) {
    addIssue(
      "high",
      "JWT Security",
      "JWT secret appears to be a default/demo value"
    );
    return false;
  }

  return true;
}

// Check for SQL injection vulnerabilities
function checkSQLInjectionVulnerabilities(content, filePath) {
  const dangerousPatterns = [
    /query\s*\(\s*[`"'].*\$\{.*\}.*[`"']\s*\)/gi, // Template literals in queries
    /query\s*\(\s*.*\+\s*.*\)/gi, // String concatenation in queries
    /exec\s*\(\s*[`"'].*\$\{.*\}.*[`"']\s*\)/gi, // Template literals in exec
  ];

  let foundVulnerabilities = false;
  dangerousPatterns.forEach((pattern) => {
    const matches = content.match(pattern);
    if (matches) {
      matches.forEach((match) => {
        addIssue(
          "high",
          "SQL Injection",
          `Potential SQL injection vulnerability: ${match}`,
          filePath
        );
        foundVulnerabilities = true;
      });
    }
  });

  return foundVulnerabilities;
}

// Check for XSS vulnerabilities
function checkXSSVulnerabilities(content, filePath) {
  const dangerousPatterns = [
    /innerHTML\s*=\s*.*userInput/gi,
    /dangerouslySetInnerHTML/gi,
    /document\.write\s*\(/gi,
    /eval\s*\(/gi,
  ];

  let foundVulnerabilities = false;
  dangerousPatterns.forEach((pattern) => {
    const matches = content.match(pattern);
    if (matches) {
      matches.forEach((match) => {
        addIssue(
          "medium",
          "XSS Vulnerability",
          `Potential XSS vulnerability: ${match}`,
          filePath
        );
        foundVulnerabilities = true;
      });
    }
  });

  return foundVulnerabilities;
}

// Check CORS configuration
function checkCORSConfiguration(content, filePath) {
  const corsPatterns = [
    /cors\s*\(\s*\{\s*origin\s*:\s*\*\s*\}/gi, // Allow all origins
    /access-control-allow-origin\s*:\s*\*/gi, // Allow all origins header
  ];

  let foundIssues = false;
  corsPatterns.forEach((pattern) => {
    const matches = content.match(pattern);
    if (matches) {
      matches.forEach((match) => {
        addIssue(
          "medium",
          "CORS Configuration",
          `Permissive CORS configuration found: ${match}`,
          filePath
        );
        foundIssues = true;
      });
    }
  });

  return foundIssues;
}

// Check for outdated dependencies
function checkDependencies(packageJsonPath) {
  try {
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, "utf8"));
    const dependencies = {
      ...packageJson.dependencies,
      ...packageJson.devDependencies,
    };

    const knownVulnerabilities = [
      "lodash",
      "jquery",
      "bootstrap",
      "moment",
      "request",
    ];

    Object.keys(dependencies).forEach((dep) => {
      if (knownVulnerabilities.includes(dep)) {
        addIssue(
          "medium",
          "Dependencies",
          `Potentially outdated/vulnerable dependency: ${dep}`,
          packageJsonPath
        );
      }
    });

    return true;
  } catch (error) {
    addIssue(
      "low",
      "Dependencies",
      `Could not check dependencies: ${error.message}`,
      packageJsonPath
    );
    return false;
  }
}

// Check environment configuration
function checkEnvironmentConfig() {
  const envFiles = [
    ".env",
    "backend/.env",
    "admin-dashboard/.env",
    "backend/.env.example",
    "admin-dashboard/.env.example",
  ];

  envFiles.forEach((filePath) => {
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, "utf8");

      // Check for default values
      if (
        content.includes("your-") ||
        content.includes("example") ||
        content.includes("demo")
      ) {
        addIssue(
          "medium",
          "Environment Configuration",
          `Environment file contains default/example values: ${filePath}`,
          filePath
        );
      }

      // Check JWT secret
      const jwtMatch = content.match(/JWT_SECRET=(.+)/);
      if (jwtMatch) {
        checkJWTSecret(jwtMatch[1]);
      }

      // Check for hardcoded secrets in env files
      checkHardcodedSecrets(content, filePath);
    }
  });
}

// Check Docker security
function checkDockerSecurity() {
  const dockerFiles = [
    "Dockerfile",
    "backend/Dockerfile",
    "admin-dashboard/Dockerfile",
  ];

  dockerFiles.forEach((filePath) => {
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, "utf8");

      // Check for root user
      if (!content.includes("USER") || content.includes("USER root")) {
        addIssue(
          "medium",
          "Docker Security",
          "Dockerfile does not specify non-root user",
          filePath
        );
      }

      // Check for latest tag
      if (content.includes(":latest")) {
        addIssue(
          "low",
          "Docker Security",
          "Using latest tag - consider pinning to specific version",
          filePath
        );
      }

      // Check for ADD instead of COPY
      if (content.includes("ADD")) {
        addIssue(
          "low",
          "Docker Security",
          "Using ADD command - consider using COPY instead",
          filePath
        );
      }

      // Check for secrets in Dockerfile
      if (
        content.includes("ENV") &&
        (content.includes("KEY") ||
          content.includes("SECRET") ||
          content.includes("PASSWORD"))
      ) {
        addIssue(
          "high",
          "Docker Security",
          "Potential secrets in Dockerfile environment variables",
          filePath
        );
      }
    }
  });
}

// Check Supabase configuration
function checkSupabaseConfig() {
  const configFiles = [
    "docker-compose.yml",
    "backend/.env.example",
    "admin-dashboard/.env.example",
    ".env",
  ];

  configFiles.forEach((filePath) => {
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, "utf8");

      // Check for default Supabase keys
      if (
        content.includes("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9") &&
        (content.includes("supabase") || content.includes("SERVICE_ROLE"))
      ) {
        addIssue(
          "high",
          "Supabase Configuration",
          "Supabase keys detected - ensure these are not production keys",
          filePath
        );
      }

      // Check for localhost Supabase URL in production context
      if (
        content.includes("localhost:8000") &&
        !filePath.includes(".example")
      ) {
        addIssue(
          "medium",
          "Supabase Configuration",
          "Localhost Supabase URL - configure production URL",
          filePath
        );
      }
    }
  });
}

// Check file permissions
function checkFilePermissions() {
  const sensitiveFiles = [
    ".env",
    "backend/.env",
    "admin-dashboard/.env",
    "docker-compose.yml",
    "supabase/config.toml",
  ];

  sensitiveFiles.forEach((filePath) => {
    if (fs.existsSync(filePath)) {
      try {
        const stats = fs.statSync(filePath);
        const mode = stats.mode & parseInt("777", 8); // Get last 3 digits of mode

        // Check if file has overly permissive permissions (more than 600 for regular files)
        if (mode > parseInt("600", 8)) {
          addIssue(
            "medium",
            "File Permissions",
            `File has overly permissive permissions: ${mode.toString(
              8
            )} - should be 600 or less`,
            filePath
          );
        }
      } catch (error) {
        addIssue(
          "low",
          "File Permissions",
          `Could not check file permissions: ${error.message}`,
          filePath
        );
      }
    }
  });
}

// Main audit function
async function runSecurityAudit() {
  log("🔒 TalkAR Security Audit", colors.bright);
  log("=========================", colors.bright);

  log("\n📁 Scanning project files...", colors.blue);

  // Check JavaScript/TypeScript files
  const codeFiles = [
    ...getFilesByExtension(".", ".js"),
    ...getFilesByExtension(".", ".ts"),
    ...getFilesByExtension(".", ".tsx"),
    ...getFilesByExtension(".", ".jsx"),
  ];

  codeFiles.forEach((filePath) => {
    // Skip node_modules and other non-project files
    if (filePath.includes("node_modules") || filePath.includes(".git")) {
      return;
    }

    try {
      const content = fs.readFileSync(filePath, "utf8");
      checkHardcodedSecrets(content, filePath);
      checkSQLInjectionVulnerabilities(content, filePath);
      checkXSSVulnerabilities(content, filePath);
      checkCORSConfiguration(content, filePath);
    } catch (error) {
      // Ignore binary files and files that can't be read as text
      if (error.code !== "EISDIR" && !error.message.includes("binary")) {
        addIssue(
          "low",
          "File Access",
          `Could not read file: ${error.message}`,
          filePath
        );
      }
    }
  });

  // Check configuration files
  log("\n⚙️  Checking configuration files...", colors.blue);
  checkEnvironmentConfig();
  checkDockerSecurity();
  checkSupabaseConfig();
  checkFilePermissions();

  // Check package.json files
  log("\n📦 Checking dependencies...", colors.blue);
  const packageFiles = [
    "package.json",
    "backend/package.json",
    "admin-dashboard/package.json",
  ];
  packageFiles.forEach((filePath) => {
    if (fs.existsSync(filePath)) {
      checkDependencies(filePath);
    }
  });

  // Generate audit report
  generateAuditReport();
}

// Get files by extension recursively
function getFilesByExtension(dir, ext) {
  let files = [];
  try {
    const items = fs.readdirSync(dir);
    items.forEach((item) => {
      // Skip node_modules and git directories
      if (item === "node_modules" || item === ".git") {
        return;
      }

      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        files = files.concat(getFilesByExtension(fullPath, ext));
      } else if (path.extname(item) === ext) {
        files.push(fullPath);
      }
    });
  } catch (error) {
    // Directory doesn't exist or can't be read
  }
  return files;
}

// Generate audit report
function generateAuditReport() {
  log("\n📋 SECURITY AUDIT REPORT", colors.bright);
  log("========================", colors.bright);

  const totalIssues = auditResults.issues.length;
  const highIssues = auditResults.issues.filter(
    (i) => i.severity === "high"
  ).length;
  const mediumIssues = auditResults.issues.filter(
    (i) => i.severity === "medium"
  ).length;
  const lowIssues = auditResults.issues.filter(
    (i) => i.severity === "low"
  ).length;

  log(`Total Issues Found: ${totalIssues}`, colors.cyan);
  log(
    `High Severity: ${highIssues}`,
    highIssues > 0 ? colors.red : colors.green
  );
  log(
    `Medium Severity: ${mediumIssues}`,
    mediumIssues > 0 ? colors.yellow : colors.green
  );
  log(`Low Severity: ${lowIssues}`, colors.green);

  // Risk assessment
  log("\n🎯 RISK ASSESSMENT", colors.bright);
  if (highIssues > 0) {
    log(
      "🔴 HIGH RISK: Critical security issues detected - immediate action required",
      colors.red
    );
  } else if (mediumIssues > 5) {
    log("🟡 MEDIUM RISK: Several security improvements needed", colors.yellow);
  } else if (mediumIssues > 0) {
    log("🟢 LOW RISK: Minor security improvements recommended", colors.green);
  } else {
    log("✅ SECURE: No significant security issues detected", colors.green);
  }

  // Save detailed report
  const reportPath = "security-audit-report.json";
  fs.writeFileSync(
    reportPath,
    JSON.stringify(
      {
        summary: {
          totalIssues,
          highIssues,
          mediumIssues,
          lowIssues,
          riskLevel:
            highIssues > 0 ? "HIGH" : mediumIssues > 5 ? "MEDIUM" : "LOW",
        },
        issues: auditResults.issues,
        timestamp: new Date().toISOString(),
        recommendations: generateRecommendations(
          highIssues,
          mediumIssues,
          lowIssues
        ),
      },
      null,
      2
    )
  );

  log(`\nDetailed audit report saved to: ${reportPath}`, colors.blue);

  // Next steps
  log("\n🔧 NEXT STEPS", colors.bright);
  if (highIssues > 0) {
    log("1. Address all HIGH severity issues immediately", colors.red);
    log("2. Review and fix hardcoded secrets", colors.red);
    log("3. Update JWT secrets and Supabase keys", colors.red);
    log("4. Fix SQL injection vulnerabilities", colors.red);
  }
  if (mediumIssues > 0) {
    log("5. Review and fix MEDIUM severity issues", colors.yellow);
    log("6. Update outdated dependencies", colors.yellow);
    log("7. Review CORS configurations", colors.yellow);
  }
  log("8. Run regular security audits", colors.blue);
  log("9. Implement automated security scanning in CI/CD", colors.blue);

  return highIssues === 0;
}

function generateRecommendations(high, medium, low) {
  const recommendations = [];

  if (high > 0) {
    recommendations.push(
      "Address all high severity security issues before production deployment"
    );
    recommendations.push(
      "Replace all hardcoded secrets with environment variables"
    );
    recommendations.push("Use strong JWT secrets (minimum 32 characters)");
    recommendations.push("Generate new Supabase keys for production");
  }

  if (medium > 0) {
    recommendations.push("Update outdated dependencies");
    recommendations.push("Review and tighten CORS configurations");
    recommendations.push("Implement proper input validation and sanitization");
  }

  recommendations.push("Implement Content Security Policy (CSP) headers");
  recommendations.push("Use HTTPS in production");
  recommendations.push("Implement rate limiting for API endpoints");
  recommendations.push("Set up security monitoring and alerting");
  recommendations.push("Regularly rotate secrets and API keys");
  recommendations.push("Train team members on security best practices");

  return recommendations;
}

// Run audit if this script is executed directly
if (require.main === module) {
  runSecurityAudit();
}

module.exports = { runSecurityAudit };
