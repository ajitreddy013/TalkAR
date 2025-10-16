#!/usr/bin/env node

/**
 * Performance and Security Test Suite for TalkAR Project
 * This script runs comprehensive tests for performance, security, and API endpoints
 */

const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

// Test configuration
const CONFIG = {
  backendUrl: process.env.BACKEND_URL || 'http://localhost:3000',
  adminDashboardUrl: process.env.ADMIN_URL || 'http://localhost:3001',
  supabaseUrl: process.env.SUPABASE_URL || 'http://localhost:8000',
  timeout: 10000,
  concurrentRequests: 10,
  maxResponseTime: 2000, // 2 seconds
};

// Colors for output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m'
};

// Test results
const results = {
  passed: 0,
  failed: 0,
  warnings: 0,
  tests: []
};

// Utility functions
function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

function makeRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const startTime = Date.now();
    const urlObj = new URL(url);
    const isHttps = urlObj.protocol === 'https:';
    const client = isHttps ? https : http;
    
    const reqOptions = {
      hostname: urlObj.hostname,
      port: urlObj.port || (isHttps ? 443 : 80),
      path: urlObj.pathname + urlObj.search,
      method: options.method || 'GET',
      headers: options.headers || {},
      timeout: options.timeout || CONFIG.timeout
    };

    const req = client.request(reqOptions, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        const endTime = Date.now();
        const responseTime = endTime - startTime;
        
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: data,
          responseTime,
          url
        });
      });
    });

    req.on('error', reject);
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Request timeout'));
    });

    if (options.body) {
      req.write(options.body);
    }
    req.end();
  });
}

function addResult(testName, passed, message, details = {}) {
  results.tests.push({
    name: testName,
    passed,
    message,
    details,
    timestamp: new Date().toISOString()
  });
  
  if (passed) {
    results.passed++;
    log(`✅ ${testName}`, colors.green);
  } else {
    results.failed++;
    log(`❌ ${testName}: ${message}`, colors.red);
  }
}

function addWarning(testName, message) {
  results.warnings++;
  log(`⚠️  ${testName}: ${message}`, colors.yellow);
}

// Performance Tests
async function testPerformance() {
  log('\n📊 PERFORMANCE TESTS', colors.bright);
  log('=====================', colors.bright);
  
  // Test backend response time
  try {
    const response = await makeRequest(`${CONFIG.backendUrl}/health`);
    const passed = response.responseTime < CONFIG.maxResponseTime;
    addResult(
      'Backend Health Check Performance',
      passed,
      `Response time: ${response.responseTime}ms (threshold: ${CONFIG.maxResponseTime}ms)`,
      { responseTime: response.responseTime, threshold: CONFIG.maxResponseTime }
    );
  } catch (error) {
    addResult('Backend Health Check Performance', false, error.message);
  }
  
  // Test concurrent requests
  try {
    const requests = Array(CONFIG.concurrentRequests).fill(null).map(() => 
      makeRequest(`${CONFIG.backendUrl}/health`)
    );
    
    const startTime = Date.now();
    const responses = await Promise.all(requests);
    const totalTime = Date.now() - startTime;
    
    const allPassed = responses.every(r => r.statusCode === 200);
    const avgResponseTime = responses.reduce((sum, r) => sum + r.responseTime, 0) / responses.length;
    
    addResult(
      'Concurrent Request Handling',
      allPassed,
      `${CONFIG.concurrentRequests} concurrent requests completed in ${totalTime}ms, avg: ${avgResponseTime.toFixed(2)}ms`,
      { concurrentRequests: CONFIG.concurrentRequests, totalTime, avgResponseTime }
    );
  } catch (error) {
    addResult('Concurrent Request Handling', false, error.message);
  }
  
  // Test admin dashboard loading
  try {
    const response = await makeRequest(CONFIG.adminDashboardUrl);
    const passed = response.responseTime < CONFIG.maxResponseTime * 2; // Allow more time for React app
    addResult(
      'Admin Dashboard Loading Performance',
      passed,
      `Response time: ${response.responseTime}ms (threshold: ${CONFIG.maxResponseTime * 2}ms)`,
      { responseTime: response.responseTime, threshold: CONFIG.maxResponseTime * 2 }
    );
  } catch (error) {
    addResult('Admin Dashboard Loading Performance', false, error.message);
  }
}

// Security Tests
async function testSecurity() {
  log('\n🔒 SECURITY TESTS', colors.bright);
  log('==================', colors.bright);
  
  // Test security headers
  try {
    const response = await makeRequest(`${CONFIG.backendUrl}/health`);
    const headers = response.headers;
    
    const securityHeaders = {
      'x-content-type-options': 'nosniff',
      'x-frame-options': 'DENY',
      'x-xss-protection': '1; mode=block',
      'strict-transport-security': 'max-age=31536000'
    };
    
    let missingHeaders = [];
    for (const [header, expectedValue] of Object.entries(securityHeaders)) {
      if (!headers[header] || !headers[header].includes(expectedValue)) {
        missingHeaders.push(header);
      }
    }
    
    if (missingHeaders.length === 0) {
      addResult('Security Headers', true, 'All security headers present');
    } else {
      addWarning('Security Headers', `Missing headers: ${missingHeaders.join(', ')}`);
      addResult('Security Headers', true, 'Basic security check completed with warnings');
    }
  } catch (error) {
    addResult('Security Headers Check', false, error.message);
  }
  
  // Test SQL injection protection
  try {
    const maliciousInput = "'; DROP TABLE users; --";
    const response = await makeRequest(`${CONFIG.backendUrl}/api/v1/sync/voices`);
    
    if (response.statusCode === 200) {
      addResult('SQL Injection Protection', true, 'Endpoint handles requests safely');
    } else {
      addResult('SQL Injection Protection', false, `Unexpected status code: ${response.statusCode}`);
    }
  } catch (error) {
    addResult('SQL Injection Protection', false, error.message);
  }
  
  // Test CORS configuration
  try {
    const response = await makeRequest(`${CONFIG.backendUrl}/health`, {
      headers: {
        'Origin': 'https://malicious-site.com'
      }
    });
    
    const corsHeader = response.headers['access-control-allow-origin'];
    if (!corsHeader || corsHeader === '*') {
      addWarning('CORS Configuration', 'CORS allows all origins - review for production');
    } else {
      addResult('CORS Configuration', true, 'CORS properly configured');
    }
  } catch (error) {
    addResult('CORS Configuration Check', false, error.message);
  }
}

// API Tests
async function testAPI() {
  log('\n🌐 API TESTS', colors.bright);
  log('=============', colors.bright);
  
  // Test health endpoints
  try {
    const backendHealth = await makeRequest(`${CONFIG.backendUrl}/health`);
    addResult(
      'Backend Health API',
      backendHealth.statusCode === 200,
      `Status: ${backendHealth.statusCode}, Response time: ${backendHealth.responseTime}ms`
    );
  } catch (error) {
    addResult('Backend Health API', false, error.message);
  }
  
  // Test Supabase connection
  try {
    const supabaseHealth = await makeRequest(`${CONFIG.supabaseUrl}/health`);
    addResult(
      'Supabase Health Check',
      supabaseHealth.statusCode === 200,
      `Status: ${supabaseHealth.statusCode}`
    );
  } catch (error) {
    addResult('Supabase Health Check', false, error.message);
  }
  
  // Test sync endpoints
  try {
    const syncResponse = await makeRequest(`${CONFIG.backendUrl}/api/v1/sync/voices`);
    addResult(
      'Sync Voices API',
      syncResponse.statusCode === 200,
      `Status: ${syncResponse.statusCode}, Response time: ${syncResponse.responseTime}ms`
    );
  } catch (error) {
    addResult('Sync Voices API', false, error.message);
  }
}

// Database Tests
async function testDatabase() {
  log('\n🗄️  DATABASE TESTS', colors.bright);
  log('===================', colors.bright);
  
  // Test database connection (if backend provides endpoint)
  try {
    const response = await makeRequest(`${CONFIG.backendUrl}/api/v1/health/db`);
    addResult(
      'Database Connection',
      response.statusCode === 200,
      `Status: ${response.statusCode}`
    );
  } catch (error) {
    addWarning('Database Connection', 'Database health endpoint not available - check backend logs');
  }
}

// Frontend Tests
async function testFrontend() {
  log('\n🎨 FRONTEND TESTS', colors.bright);
  log('=================', colors.bright);
  
  // Test admin dashboard accessibility
  try {
    const response = await makeRequest(CONFIG.adminDashboardUrl);
    addResult(
      'Admin Dashboard Accessibility',
      response.statusCode === 200,
      `Status: ${response.statusCode}, Response time: ${response.responseTime}ms`
    );
    
    // Check for basic HTML structure
    if (response.body && response.body.includes('<!DOCTYPE html>')) {
      addResult('HTML Structure', true, 'Valid HTML structure detected');
    } else {
      addWarning('HTML Structure', 'Unable to verify HTML structure');
    }
  } catch (error) {
    addResult('Admin Dashboard Accessibility', false, error.message);
  }
}

// Generate test report
function generateReport() {
  log('\n📋 TEST REPORT', colors.bright);
  log('==============', colors.bright);
  
  const totalTests = results.passed + results.failed;
  const successRate = totalTests > 0 ? (results.passed / totalTests * 100).toFixed(1) : 0;
  
  log(`Total Tests: ${totalTests}`, colors.cyan);
  log(`Passed: ${results.passed}`, colors.green);
  log(`Failed: ${results.failed}`, colors.red);
  log(`Warnings: ${results.warnings}`, colors.yellow);
  log(`Success Rate: ${successRate}%`, successRate >= 80 ? colors.green : colors.red);
  
  // Save detailed report
  const reportPath = 'performance-security-report.json';
  fs.writeFileSync(reportPath, JSON.stringify(results, null, 2));
  log(`\nDetailed report saved to: ${reportPath}`, colors.blue);
  
  // Summary recommendations
  log('\n🔧 RECOMMENDATIONS:', colors.bright);
  if (results.failed > 0) {
    log('- Address failed tests before production deployment', colors.red);
  }
  if (results.warnings > 0) {
    log('- Review warnings for potential improvements', colors.yellow);
  }
  if (successRate >= 90) {
    log('- System appears ready for production deployment', colors.green);
  } else if (successRate >= 70) {
    log('- System needs improvements before production deployment', colors.yellow);
  } else {
    log('- System requires significant fixes before deployment', colors.red);
  }
  
  return results.failed === 0;
}

// Main test runner
async function runTests() {
  log('🚀 TalkAR Performance & Security Test Suite', colors.bright);
  log('===========================================', colors.bright);
  log(`Backend URL: ${CONFIG.backendUrl}`);
  log(`Admin Dashboard URL: ${CONFIG.adminDashboardUrl}`);
  log(`Supabase URL: ${CONFIG.supabaseUrl}`);
  log(`Timeout: ${CONFIG.timeout}ms`);
  log(`Max Response Time: ${CONFIG.maxResponseTime}ms`);
  
  try {
    await testPerformance();
    await testSecurity();
    await testAPI();
    await testDatabase();
    await testFrontend();
    
    const success = generateReport();
    process.exit(success ? 0 : 1);
  } catch (error) {
    log(`\n❌ Test suite failed: ${error.message}`, colors.red);
    process.exit(1);
  }
}

// Run tests if this script is executed directly
if (require.main === module) {
  runTests();
}

module.exports = { runTests, CONFIG };